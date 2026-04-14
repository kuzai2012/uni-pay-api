package ${PACKAGE}.joinpay.sdk;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;

/**
 * 汇聚支付 MD5 签名工具
 */
public class JoinPaySignature {

    public static String sign(Map<String, Object> params, String merchantKey) {
        List<String> keys = new ArrayList<>();
        for (String key : params.keySet()) {
            if ("hmac".equalsIgnoreCase(key)) {
                continue;
            }
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

        return DigestUtils.md5Hex(content.toString() + merchantKey).toUpperCase();
    }

    public static boolean verify(Map<String, Object> params, String merchantKey) {
        String receivedHmac = params.get("hmac") == null ? "" : params.get("hmac").toString().toUpperCase();
        String calculatedHmac = sign(params, merchantKey);
        return calculatedHmac.equals(receivedHmac);
    }
}
