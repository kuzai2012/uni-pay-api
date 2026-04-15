import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 汇聚支付微信主扫支付测试 - RSA签名版本
 *
 * 零外部依赖，直接编译运行（需与 JoinPayRsaSignature.java 同目录）:
 *   javac JoinPayRsaSignature.java
 *   javac JoinPayRsaNativePayTest.java
 *   java JoinPayRsaNativePayTest
 *
 * 与 MD5 版本(JoinPayNativePayTest.java)的区别：
 * - 使用 RSA 私钥签名替代 MD5 密钥签名
 * - 参数构建逻辑完全一致，确保模型有明确参照
 */
public class JoinPayRsaNativePayTest {

    // ========== 配置区（请修改为你的实际信息）==========
    static final String BASE_URL          = "https://trade.joinpay.cc";         // 测试环境
    static final String MERCHANT_NO       = "888100500008456";                   // 商户号
    static final String TRADE_MERCHANT_NO = "";                                  // 报备商户号（微信渠道必填，测试环境可先留空）
    static final String NOTIFY_URL        = "http://10.30.50.49:8090/notify_url.jsp"; // 回调地址

    // RSA私钥（PKCS8格式），请替换为你的真实私钥
    static final String RSA_PRIVATE_KEY   = "-----BEGIN PRIVATE KEY-----\n"
                                          + "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC7...\n"  // ← 替换为完整私钥
                                          + "-----END PRIVATE KEY-----";

    // ========== 主方法 ==========
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  汇聚支付 - 微信主扫支付测试(RSA签名)");
        System.out.println("========================================\n");

        // 1. 构建请求参数 — 与MD5版本完全一致的参数构建方式
        Map<String, String> params = new LinkedHashMap<>();
        params.put("p0_Version",          "2.6");                    // 支付类接口版本线 ≥ 2.6
        params.put("p1_MerchantNo",       MERCHANT_NO);
        params.put("p2_OrderNo",          "TEST" + System.currentTimeMillis());
        params.put("p3_Amount",           "0.03");
        params.put("p4_Cur",              "1");                     // 人民币币种标识，固定为 "1"
        params.put("p5_ProductName",      "测试商品-微信主扫-RSA");
        params.put("p9_NotifyUrl",        NOTIFY_URL);
        params.put("q1_FrpCode",          "WEIXIN_NATIVE");
        params.put("qa_TradeMerchantNo",  TRADE_MERCHANT_NO);

        // 2. 使用RSA私钥生成签名（替代MD5的 sign(params, MERCHANT_KEY)）
        Map<String, Object> signParams = new HashMap<>(params);
        String hmac = JoinPayRsaSignature.sign(signParams, RSA_PRIVATE_KEY);
        params.put("hmac", hmac);

        System.out.println("[请求参数]");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if ("hmac".equals(entry.getKey())) {
                System.out.println("  hmac = " + entry.getValue().substring(0, Math.min(16, entry.getValue().length())) + "...");
            } else {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue());
            }
        }
        System.out.println();

        // 3. 发送请求（与MD5版本完全相同的HTTP请求逻辑）
        String apiUrl = BASE_URL + "/tradeRt/uniPay";
        System.out.println("[请求] POST " + apiUrl);
        System.out.println();

        String responseBody = doPost(apiUrl, params);
        System.out.println("[响应结果]");
        System.out.println(prettyPrint(responseBody));
        System.out.println();

        // 4. 解析结果
        String raCode  = extractField(responseBody, "ra_Code");
        String codeMsg = extractField(responseBody, "rb_CodeMsg");
        String trxNo   = extractField(responseBody, "r7_TrxNo");
        String orderNo = extractField(responseBody, "r2_OrderNo");
        String amount  = extractField(responseBody, "r3_Amount");
        String payUrl  = extractField(responseBody, "rc_Result");
        String pic     = extractField(responseBody, "rd_Pic");

        if ("100".equals(raCode)) {
            System.out.println("========================================");
            System.out.println("  下单成功!");
            System.out.println("========================================");
            System.out.println("  商户订单号: " + orderNo);
            System.out.println("  支付金额:   " + amount + " 元");
            System.out.println("  交易流水号: " + trxNo);
            System.out.println("  扫码支付链接: " + payUrl);
            if (pic != null && !pic.isEmpty()) {
                System.out.println("  二维码图片: " + (pic.length() > 50 ? pic.substring(0, 50) + "..." : pic) + " (base64)");
            }
            System.out.println();
            System.out.println("  >> 请用微信扫描上方链接的二维码完成支付 <<");
            System.out.println("========================================");
        } else {
            System.out.println("========================================");
            System.out.println("  下单失败!");
            System.out.println("========================================");
            System.out.println("  错误码: " + raCode);
            System.out.println("  错误信息: " + codeMsg);
            System.out.println("========================================");
        }
    }

    // ========== HTTP POST请求（与MD5版本完全一致）==========
    static String doPost(String urlStr, Map<String, String> params) throws Exception {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (body.length() > 0) body.append("&");
            body.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(entry.getValue() != null ? entry.getValue() : "", "UTF-8"));
        }

        URL url = new URL(urlStr);
        javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

        // 忽略SSL证书（测试环境）
        conn.setSSLSocketFactory(trustAllSslContext().getSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        conn.disconnect();

        if (code != 200) {
            throw new RuntimeException("HTTP " + code + ": " + response);
        }
        return response.toString();
    }

    static javax.net.ssl.SSLContext trustAllSslContext() throws Exception {
        javax.net.ssl.TrustManager[] tm = new javax.net.ssl.TrustManager[]{
            new javax.net.ssl.X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
            }
        };
        javax.net.ssl.SSLContext ctx = javax.net.ssl.SSLContext.getInstance("TLS");
        ctx.init(null, tm, new java.security.SecureRandom());
        return ctx;
    }

    // ========== JSON字段提取（与MD5版本完全一致）==========
    static String extractField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;

        int colonIdx = json.indexOf(":", idx + pattern.length());
        if (colonIdx < 0) return null;

        int start = colonIdx + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;

        if (start >= json.length()) return null;

        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            if (end < 0) return null;
            return json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != '\n') {
                end++;
            }
            return json.substring(start, end).trim();
        }
    }

    static String prettyPrint(String json) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{' || c == '[') {
                sb.append(c).append("\n");
                indent++;
                for (int j = 0; j < indent; j++) sb.append("  ");
            } else if (c == '}' || c == ']') {
                sb.append("\n");
                indent--;
                for (int j = 0; j < indent; j++) sb.append("  ");
                sb.append(c);
            } else if (c == ',') {
                sb.append(c).append("\n");
                for (int j = 0; j < indent; j++) sb.append("  ");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
