package com.joinpay.sdk;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 汇聚支付 RSA 签名工具
 *
 * 签名算法（与 RSAUtils.java / SignBiz.java 的CERT模式一致）：
 * 1. 排除 hmac 参数，按 key 字母升序排序，拼接所有 value（包括空字符串）
 * 2. 使用商户 RSA 私钥签名（MD5withRSA）
 * 3. 验签使用商户公钥（X.509格式，公钥长度>1024时使用X.509解析器）
 */
public class JoinPayRsaSignature {

    /** 与 RSAUtils.java:76 保持一致 */
    private static final String SIGN_ALGORITHM = "MD5withRSA";

    /**
     * 使用RSA私钥生成签名
     *
     * @param params       请求参数
     * @param privateKey   私钥（支持PKCS1/PKCS8格式，PEM或纯Base64均可）
     * @return Base64编码的签名字符串
     */
    public static String sign(Map<String, Object> params, String privateKey) throws Exception {
        String signStr = buildSignString(params);

        byte[] keyBytes = Base64.getDecoder().decode(cleanKey(privateKey));
        byte[] pkcs8Bytes = ensurePkcs8(keyBytes);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privKey = keyFactory.generatePrivate(keySpec);

        // 签名
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privKey);
        signature.update(signStr.getBytes("UTF-8"));
        byte[] signBytes = signature.sign();

        return Base64.getEncoder().encodeToString(signBytes);
    }

    /**
     * 使用RSA公钥验证签名
     *
     * @param params     包含hmac的完整参数Map
     * @param publicKey  公钥（PEM格式或纯Base64均可）
     * @param hmac       收到的签名值
     * @return true=验签通过
     */
    public static boolean verify(Map<String, Object> params, String publicKey, String hmac) throws Exception {
        String signStr = buildSignString(params);

        // 处理Base64解码时空格问题（兼容 SignBiz.java:128 的处理方式）
        hmac = hmac.replace(" ", "+");

        // 解码公钥并验签
        byte[] keyBytes = Base64.getDecoder().decode(cleanKey(publicKey));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pubKey = keyFactory.generatePublic(keySpec);

        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initVerify(pubKey);
        signature.update(signStr.getBytes("UTF-8"));
        byte[] signBytes = Base64.getDecoder().decode(hmac);

        return signature.verify(signBytes);
    }

    /**
     * 清理密钥格式：去除PEM头尾标记和所有空白字符
     * 支持以下输入格式：
     * - 纯Base64字符串（无换行）
     * - 含换行的Base64字符串（Java字符串中的 \n）
     * - 完整PEM格式（含 -----BEGIN PRIVATE KEY----- 等头尾）
     */
    private static String cleanKey(String key) {
        // 去除PEM头尾
        String cleaned = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            // Java字符串拼接中的 \\n 转义换行
            .replace("\\n", "")
            // 实际换行符
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "");
        return cleaned.trim();
    }

    /**
     * 检测密钥是否为PKCS1格式，如果是则自动转换为PKCS8格式。
     *
     * PKCS1 → PKCS8 ASN.1结构:
     *   SEQUENCE { INTEGER(0), SEQUENCE { OID(rsaEncryption), NULL }, OCTET_STRING(pkcs1Bytes) }
     *
     * 不依赖BouncyCastle，逐字节构建避免数组索引覆盖问题。
     */
    private static byte[] ensurePkcs8(byte[] keyBytes) {
        if (isPkcs8(keyBytes)) {
            return keyBytes;
        }

        int keyLen = keyBytes.length;

        // 算法标识: SEQUENCE { OID(rsaEncryption 1.2.840.113549.1.1.1), NULL } = 15字节
        byte[] algId = {
            0x30, 0x0D,
            0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
            0x05, 0x00
        };

        // 版本: INTEGER 0 = 3字节
        byte[] version = {0x02, 0x01, 0x00};

        // OCTET STRING 头: tag + 长度编码
        int octetHeaderLen = (keyLen < 128) ? 2 : (keyLen < 256) ? 3 : 4;
        byte[] octetHeader = new byte[octetHeaderLen];
        octetHeader[0] = 0x04;
        if (keyLen < 128) {
            octetHeader[1] = (byte) keyLen;
        } else if (keyLen < 256) {
            octetHeader[1] = (byte) 0x81;
            octetHeader[2] = (byte) keyLen;
        } else {
            octetHeader[1] = (byte) 0x82;
            octetHeader[2] = (byte) ((keyLen >> 8) & 0xFF);
            octetHeader[3] = (byte) (keyLen & 0xFF);
        }

        // SEQUENCE 内部内容长度
        int contentLen = version.length + algId.length + octetHeader.length + keyLen;

        // 外层 SEQUENCE 头: tag + 长度编码
        int seqHeaderLen = (contentLen < 128) ? 2 : (contentLen < 256) ? 3 : 4;
        byte[] seqHeader = new byte[seqHeaderLen];
        seqHeader[0] = 0x30;
        if (contentLen < 128) {
            seqHeader[1] = (byte) contentLen;
        } else if (contentLen < 256) {
            seqHeader[1] = (byte) 0x81;
            seqHeader[2] = (byte) contentLen;
        } else {
            seqHeader[1] = (byte) 0x82;
            seqHeader[2] = (byte) ((contentLen >> 8) & 0xFF);
            seqHeader[3] = (byte) (contentLen & 0xFF);
        }

        // 拼接: SEQUENCE头 + 版本 + 算法标识 + OCTET_STRING头 + PKCS1密钥
        byte[] result = new byte[seqHeader.length + contentLen];
        int pos = 0;
        System.arraycopy(seqHeader, 0, result, pos, seqHeader.length); pos += seqHeader.length;
        System.arraycopy(version, 0, result, pos, version.length); pos += version.length;
        System.arraycopy(algId, 0, result, pos, algId.length); pos += algId.length;
        System.arraycopy(octetHeader, 0, result, pos, octetHeader.length); pos += octetHeader.length;
        System.arraycopy(keyBytes, 0, result, pos, keyLen);

        return result;
    }

    /**
     * 判断密钥是否为PKCS8格式。
     * PKCS8 DER结构: 30 82 xx xx 02 01 00 30 0D 06 09 2A 86 48 ...
     * PKCS1 DER结构: 30 82 xx xx 02 01 00 02 82 xx xx ... (第7字节是0x02而非0x30)
     */
    private static boolean isPkcs8(byte[] keyBytes) {
        if (keyBytes.length < 13) return false;
        // 两者前6字节相同 (30 82 xx xx 02 01 00)，区分在第7-8字节:
        // PKCS8: 30 0D (算法标识SEQUENCE)
        // PKCS1: 02 xx (模数INTEGER)
        return keyBytes[7] == 0x30 && keyBytes[8] == 0x0D;
    }

    /**
     * 构建待签名字符串（与 SignBiz.java:80-99 一致）
     * 注意：value为空的参数不参与签名（SignBiz接收的HTTP请求中本身不包含空值字段）
     */
    private static String buildSignString(Map<String, Object> params) {
        List<String> keys = new ArrayList<>();
        for (String key : params.keySet()) {
            if ("hmac".equalsIgnoreCase(key)) continue;
            keys.add(key);
        }
        Collections.sort(keys);

        StringBuilder content = new StringBuilder();
        for (String key : keys) {
            Object value = params.get(key);
            String strValue = value == null ? "" : value.toString().trim();
            if (!strValue.isEmpty()) {
                content.append(strValue);
            }
        }
        return content.toString();
    }
}
