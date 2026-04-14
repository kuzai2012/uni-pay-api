package ${PACKAGE}.joinpay.sdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 汇聚支付 HTTP 客户端
 *
 * 封装了汇聚支付API的HTTP请求逻辑，支持MD5和RSA双签名模式。
 */
public class JoinPayClient {

    private final String baseUrl;
    private final String merchantNo;
    private final String merchantKey;
    private final String rsaPrivateKey;
    private final String signType;
    private final String version;

    public JoinPayClient(String baseUrl, String merchantNo, String merchantKey,
                         String rsaPrivateKey, String signType) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.merchantNo = merchantNo;
        this.merchantKey = merchantKey;
        this.rsaPrivateKey = rsaPrivateKey;
        this.signType = signType != null ? signType.toUpperCase() : "MD5";
        this.version = "2.6";
    }

    public JSONObject unifiedPay(UnifiedPayRequest request) throws Exception {
        Map<String, Object> params = buildUnifiedPayParams(request);
        return post("/tradeRt/uniPay", params);
    }

    public JSONObject queryOrder(String orderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", version);
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", orderNo);
        return post("/tradeRt/queryOrder", params);
    }

    public JSONObject refund(RefundRequest request) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", "2.3");
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", request.orderNo);
        params.put("p3_RefundOrderNo", request.refundOrderNo);
        params.put("p4_RefundAmount", request.refundAmount);
        params.put("p5_RefundReason", request.reason);
        if (request.notifyUrl != null && !request.notifyUrl.isEmpty()) {
            params.put("p6_NotifyUrl", request.notifyUrl);
        }
        return post("/tradeRt/refund", params);
    }

    public JSONObject queryRefund(String refundOrderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", "2.3");
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_RefundOrderNo", refundOrderNo);
        return post("/tradeRt/queryRefund", params);
    }

    public JSONObject queryRefundInfo(String orderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", orderNo);
        return post("/tradeRt/queryRefundInfo", params);
    }

    public JSONObject closeOrder(String orderNo, String frpCode, String trxNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", orderNo);
        params.put("p3_FrpCode", frpCode);
        if (trxNo != null && !trxNo.isEmpty()) {
            params.put("p4_TrxNo", trxNo);
        }
        return post("/tradeRt/closeOrder", params);
    }

    public JSONObject queryFunds(String orderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", version);
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", orderNo);
        return post("/tradeRt/queryFundsControlOrder", params);
    }

    private Map<String, Object> buildUnifiedPayParams(UnifiedPayRequest req) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", version);
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", req.orderNo);
        params.put("p3_Amount", req.amount);
        params.put("p4_Cur", req.cur != null ? req.cur : "1");
        params.put("p5_ProductName", req.productName);
        if (req.productDesc != null) params.put("p6_ProductDesc", req.productDesc);
        if (req.mp != null) params.put("p7_Mp", req.mp);
        if (req.returnUrl != null) params.put("p8_ReturnUrl", req.returnUrl);
        params.put("p9_NotifyUrl", req.notifyUrl != null ? req.notifyUrl : "");
        params.put("q1_FrpCode", req.frpCode);
        if (req.subMerchantNo != null) params.put("q3_SubMerchantNo", req.subMerchantNo);
        if (req.isShowPic != null) params.put("q4_IsShowPic", req.isShowPic);
        if (req.openId != null) params.put("q5_OpenId", req.openId);
        if (req.authCode != null) params.put("q6_AuthCode", req.authCode);
        if (req.appId != null) params.put("q7_AppId", req.appId);
        if (req.terminalNo != null) params.put("q8_TerminalNo", req.terminalNo);
        if (req.transactionModel != null) params.put("q9_TransactionModel", req.transactionModel);
        params.put("qa_TradeMerchantNo", req.tradeMerchantNo != null ? req.tradeMerchantNo : "");
        if (req.buyerId != null) params.put("qb_buyerId", req.buyerId);
        if (req.terminalIp != null) params.put("ql_TerminalIp", req.terminalIp);
        if (req.contractId != null) params.put("qm_ContractId", req.contractId);
        if (req.specialInfo != null) params.put("qn_SpecialInfo", req.specialInfo);
        return params;
    }

    private JSONObject post(String apiPath, Map<String, Object> params) throws Exception {
        String hmac;
        if ("RSA".equals(signType)) {
            hmac = JoinPayRsaSignature.sign(params, rsaPrivateKey);
        } else {
            hmac = JoinPaySignature.sign(params, merchantKey);
        }
        params.put("hmac", hmac);

        String body = encodeParams(params);
        String url = baseUrl + apiPath;

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String responseBody;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            responseBody = br.lines().collect(Collectors.joining("\n"));
        }
        conn.disconnect();

        try {
            return JSON.parseObject(responseBody);
        } catch (Exception e) {
            JSONObject result = new JSONObject();
            result.put("ra_Code", "-1");
            result.put("rb_CodeMsg", "非JSON响应: " + responseBody.substring(0, Math.min(200, responseBody.length())));
            return result;
        }
    }

    private String encodeParams(Map<String, Object> params) {
        return params.entrySet().stream()
                .map(e -> {
                    try {
                        return URLEncoder.encode(e.getKey(), "UTF-8") + "=" +
                               URLEncoder.encode(e.getValue() == null ? "" : e.getValue().toString(), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        return "";
                    }
                })
                .collect(Collectors.joining("&"));
    }

    public static class UnifiedPayRequest {
        public String orderNo;
        public String amount;
        public String productName;
        public String frpCode;
        public String notifyUrl;
        public String tradeMerchantNo;
        public String cur;
        public String productDesc;
        public String mp;
        public String returnUrl;
        public String subMerchantNo;
        public String isShowPic;
        public String openId;
        public String authCode;
        public String appId;
        public String terminalNo;
        public String transactionModel;
        public String buyerId;
        public String terminalIp;
        public String contractId;
        public String specialInfo;
    }

    public static class RefundRequest {
        public String orderNo;
        public String refundOrderNo;
        public String refundAmount;
        public String reason;
        public String notifyUrl;
    }
}
