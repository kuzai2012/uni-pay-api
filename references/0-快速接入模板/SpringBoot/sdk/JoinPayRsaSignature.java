package ${PACKAGE}.joinpay.sdk;

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
 */
public class JoinPayRsaSignature {

    private static final String SIGN_ALGORITHM = "MD5withRSA";

    public static String sign(Map<String, Object> params, String privateKey) throws Exception {
        String signStr = buildSignString(params);

        byte[] keyBytes = Base64.getDecoder().decode(cleanKey(privateKey));
        byte[] pkcs8Bytes = ensurePkcs8(keyBytes);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privKey);
        signature.update(signStr.getBytes("UTF-8"));
        byte[] signBytes = signature.sign();

        return Base64.getEncoder().encodeToString(signBytes);
    }

    public static boolean verify(Map<String, Object> params, String publicKey, String hmac) throws Exception {
        String signStr = buildSignString(params);

        hmac = hmac.replace(" ", "+");

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
     * 便捷验签方法 - 从params中自动提取hmac
     */
    public static boolean verify(Map<String, Object> params, String publicKey) throws Exception {
        Object hmacObj = params.get("hmac");
        if (hmacObj == null) {
            return false;
        }
        return verify(params, publicKey, hmacObj.toString());
    }

    private static String cleanKey(String key) {
        String cleaned = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\n", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "");
        return cleaned.trim();
    }

    private static byte[] ensurePkcs8(byte[] keyBytes) {
        if (isPkcs8(keyBytes)) {
            return keyBytes;
        }

        int keyLen = keyBytes.length;

        byte[] algId = {
            0x30, 0x0D,
            0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
            0x05, 0x00
        };

        byte[] version = {0x02, 0x01, 0x00};

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

        int contentLen = version.length + algId.length + octetHeader.length + keyLen;

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

        byte[] result = new byte[seqHeader.length + contentLen];
        int pos = 0;
        System.arraycopy(seqHeader, 0, result, pos, seqHeader.length); pos += seqHeader.length;
        System.arraycopy(version, 0, result, pos, version.length); pos += version.length;
        System.arraycopy(algId, 0, result, pos, algId.length); pos += algId.length;
        System.arraycopy(octetHeader, 0, result, pos, octetHeader.length); pos += octetHeader.length;
        System.arraycopy(keyBytes, 0, result, pos, keyLen);

        return result;
    }

    private static boolean isPkcs8(byte[] keyBytes) {
        if (keyBytes.length < 13) return false;
        return keyBytes[7] == 0x30 && keyBytes[8] == 0x0D;
    }

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
