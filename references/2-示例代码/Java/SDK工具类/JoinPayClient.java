package com.joinpay.sdk;

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
 * 参考 gw_pay 项目 UniPayApiController.java 的调用模式。
 */
public class JoinPayClient {

    /** 汇聚支付网关地址 */
    private final String baseUrl;

    /** 商户号 */
    private final String merchantNo;

    /** 商户密钥(MD5签名用) */
    private final String merchantKey;

    /** RSA私钥(RSA签名用) */
    private final String rsaPrivateKey;

    /** 签名方式: "MD5" 或 "RSA" */
    private final String signType;

    /** 接口版本号(默认2.6) */
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

    // ==================== 公开API方法 ====================

    /**
     * 统一支付下单
     */
    public JSONObject unifiedPay(UnifiedPayRequest request) throws Exception {
        Map<String, Object> params = buildUnifiedPayParams(request);
        return post("/tradeRt/uniPay", params);
    }

    /**
     * 订单查询
     */
    public JSONObject queryOrder(String orderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", version);
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", orderNo);
        return post("/tradeRt/queryOrder", params);
    }

    /**
     * 退款申请
     */
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

    /**
     * 退款查询
     */
    public JSONObject queryRefund(String refundOrderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", "2.3");
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_RefundOrderNo", refundOrderNo);
        return post("/tradeRt/queryRefund", params);
    }

    /**
     * 退款信息查询（根据支付订单号查全部退款记录）
     */
    public JSONObject queryRefundInfo(String orderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", orderNo);
        return post("/tradeRt/queryRefundInfo", params);
    }

    /**
     * 关闭订单
     */
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

    /**
     * 资金查询（发货管理）
     */
    public JSONObject queryFunds(String orderNo) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p0_Version", version);
        params.put("p1_MerchantNo", merchantNo);
        params.put("p2_OrderNo", orderNo);
        return post("/tradeRt/queryFundsControlOrder", params);
    }

    // ==================== 内部实现 ====================

    /**
     * 构建统一支付下单参数
     */
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

        // 回调地址校验（汇聚服务端会拒绝 localhost/127.0.0.1 等内网地址）
        String notifyUrl = req.notifyUrl;
        if (notifyUrl == null || notifyUrl.isEmpty()) {
            notifyUrl = "";
        } else {
            validateNotifyUrl(notifyUrl); // 不合法则抛异常
        }
        params.put("p9_NotifyUrl", notifyUrl);
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

    /**
     * 执行POST请求并返回JSON结果
     */
    private JSONObject post(String apiPath, Map<String, Object> params) throws Exception {
        // 1. 生成签名
        String hmac;
        if ("RSA".equals(signType)) {
            hmac = JoinPayRsaSignature.sign(params, rsaPrivateKey);
        } else {
            hmac = JoinPaySignature.sign(params, merchantKey);
        }
        params.put("hmac", hmac);

        // 2. 构建URL编码的请求体
        String body = encodeParams(params);
        String url = baseUrl + apiPath;
        System.out.println("[JoinPay] POST " + url);
        System.out.println("[JoinPay] Params: " + maskSensitiveData(body));

        // 3. 发送HTTPS请求
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        // 写入请求体
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        // 4. 读取响应
        int responseCode = conn.getResponseCode();
        InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String responseBody;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            responseBody = br.lines().collect(Collectors.joining("\n"));
        }
        conn.disconnect();

        System.out.println("[JoinPay] Response: " + responseBody);

        // 5. 解析JSON
        try {
            return JSON.parseObject(responseBody);
        } catch (Exception e) {
            JSONObject result = new JSONObject();
            result.put("ra_Code", "-1");
            result.put("rb_CodeMsg", "非JSON响应: " + responseBody.substring(0, Math.min(200, responseBody.length())));
            return result;
        }
    }

    /**
     * URL编码参数
     */
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

    /**
     * 脱敏打印（隐藏密钥信息）
     */
    private String maskSensitiveData(String data) {
        return data.replaceAll("(merchant_key|rsa_private_key)[^&]*", "$1=***")
                   .replaceAll("(hmac=)[^&]*", "$1***");
    }

    /**
     * 校验回调地址合法性
     *
     * 汇聚支付服务端会校验 p9_NotifyUrl，拒绝以下地址：
     * - localhost / 127.0.0.1（汇聚服务器无法访问你的本机）
     * - 10.x / 172.16-31.x / 192.168.x（内网地址）
     *
     * @param url 回调地址
     * @throws IllegalArgumentException 地址不合法时抛出
     */
    private void validateNotifyUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        String lower = url.trim().toLowerCase();
        String error;

        if (lower.startsWith("http://localhost") || lower.startsWith("https://localhost")) {
            error = "回调地址不能使用 localhost！"
                  + "本地开发请使用内网穿透工具(ngrok/cpolar/frp)，或使用测试服务器公网地址。"
                  + "错误地址: " + url;
        } else if (lower.matches(".*://127\\.0?\\.?0?\\.?[0-9]?.*") || lower.contains("127.1.")) {
            error = "回调地址不能使用 127.0.0.x 回环地址！错误地址: " + url;
        } else if (lower.matches("^https?://(?:10\\.|172\\.(?:1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.).*")) {
            error = "回调地址不能使用内网IP！汇聚服务端无法从外网访问。错误地址: " + url;
        } else if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            error = "回调地址必须以 http:// 或 https:// 开头。当前值: " + url;
        } else {
            return; // 通过
        }
        System.err.println("[JoinPay] 回调地址校验失败: " + error);
        throw new IllegalArgumentException(error);
    }

    // ==================== 请求对象 ====================

    /**
     * 统一支付下单请求
     */
    public static class UnifiedPayRequest {
        public String orderNo;           // 商户订单号
        public String amount;             // 订单金额(元)
        public String productName;        // 商品名称
        public String frpCode;            // 交易类型(FrpCode)
        public String notifyUrl;          // 异步通知URL
        public String tradeMerchantNo;    // 报备商户号
        public String cur;               // 币种(默认1)
        public String productDesc;        // 商品描述
        public String mp;                // 回传参数
        public String returnUrl;          // 跳转URL
        public String subMerchantNo;      // 子商户号
        public String isShowPic;          // 是否展示二维码
        public String openId;            // 微信OpenId
        public String authCode;          // 付款码
        public String appId;              // AppId
        public String terminalNo;         // 终端设备号
        public String transactionModel;   // 支付宝H5模式
        public String buyerId;            // 支付宝买家ID
        public String terminalIp;         // 终端IP
        public String contractId;         // 签约ID
        public String specialInfo;        // 特殊支付参数
    }

    /**
     * 退款请求
     */
    public static class RefundRequest {
        public String orderNo;            // 原支付订单号
        public String refundOrderNo;      // 退款订单号
        public String refundAmount;       // 退款金额
        public String reason;             // 退款原因
        public String notifyUrl;          // 退款通知URL
    }
}
