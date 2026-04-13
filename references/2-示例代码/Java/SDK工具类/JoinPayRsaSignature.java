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
 * 签名算法（与 SignBiz.java 的CERT模式一致）：
 * 1. 排除 hmac 参数，按 key 字母升序排序，拼接所有 value
 * 2. 使用商户 RSA 私钥签名（SHA256WithRSA / SHA1WithRSA）
 * 3. 验签使用商户公钥（X.509格式，公钥长度>1024时使用X.509解析器）
 */
public class JoinPayRsaSignature {

    private static final String SIGN_ALGORITHM = "SHA256WithRSA";

    /**
     * 使用RSA私钥生成签名
     *
     * @param params       请求参数
     * @param privateKey   PKCS8格式的私钥（Base64编码，不含PEM头尾）
     * @return Base64编码的签名字符串
     */
    public static String sign(Map<String, Object> params, String privateKey) throws Exception {
        String signStr = buildSignString(params);

        // 解码私钥
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
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
     * @param publicKey  公钥（Base64编码）
     * @param hmac       收到的签名值
     * @return true=验签通过
     */
    public static boolean verify(Map<String, Object> params, String publicKey, String hmac) throws Exception {
        String signStr = buildSignString(params);

        // 处理Base64解码时空格问题（兼容 SignBiz.java:128 的处理方式）
        hmac = hmac.replace(" ", "+");

        // 判断是否为X.509证书公钥
        boolean isX509 = publicKey.length() > 1024;

        // 解码公钥并验签
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        PublicKey pubKey;
        if (isX509) {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey = keyFactory.generatePublic(keySpec);
        } else {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey = keyFactory.generatePublic(keySpec);
        }

        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initVerify(pubKey);
        signature.update(signStr.getBytes("UTF-8"));
        byte[] signBytes = Base64.getDecoder().decode(hmac);

        return signature.verify(signBytes);
    }

    /**
     * 构建待签名字符串（与MD5方式的拼接逻辑一致）
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
