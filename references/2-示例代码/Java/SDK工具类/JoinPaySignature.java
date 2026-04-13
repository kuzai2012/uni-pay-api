package com.joinpay.sdk;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;

/**
 * 汇聚支付 MD5 签名工具
 *
 * 签名算法（与 SignBiz.java 一致）：
 * 1. 排除 hmac 参数
 * 2. 按 key 字母升序排序
 * 3. 拼接所有 value（不含 key）
 * 4. MD5(拼接值 + merchantKey).toUpperCase()
 */
public class JoinPaySignature {

    /**
     * 生成MD5签名
     *
     * @param params       请求参数（不包含hmac）
     * @param merchantKey  商户密钥
     * @return 签名值（大写）
     */
    public static String sign(Map<String, Object> params, String merchantKey) {
        // 1. 排除hmac，按key排序
        List<String> keys = new ArrayList<>();
        for (String key : params.keySet()) {
            if ("hmac".equalsIgnoreCase(key)) {
                continue;
            }
            keys.add(key);
        }
        Collections.sort(keys);

        // 2. 拼接所有value
        StringBuilder content = new StringBuilder();
        for (String key : keys) {
            Object value = params.get(key);
            String strValue = value == null ? "" : value.toString().trim();
            if (!strValue.isEmpty()) {
                content.append(strValue);
            }
        }

        // 3. MD5(拼接值 + 密钥).toUpperCase()
        return DigestUtils.md5Hex(content.toString() + merchantKey).toUpperCase();
    }

    /**
     * 验证MD5签名
     *
     * @param params      包含hmac的完整参数Map
     * @param merchantKey 商户密钥
     * @return true=验签通过
     */
    public static boolean verify(Map<String, Object> params, String merchantKey) {
        String receivedHmac = params.get("hmac") == null ? "" : params.get("hmac").toString().toUpperCase();
        String calculatedHmac = sign(params, merchantKey);
        return calculatedHmac.equals(receivedHmac);
    }

    /**
     * 将参数Map转换为URL编码字符串（用于日志/调试）
     */
    public static String toSortedQueryString(Map<String, Object> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            sb.append(keys.get(i)).append("=").append(params.get(keys.get(i)));
            if (i < keys.size() - 1) {
                sb.append("&");
            }
        }
        return sb.toString();
    }
}
