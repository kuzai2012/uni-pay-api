package ${PACKAGE}.joinpay;

import ${PACKAGE}.joinpay.sdk.JoinPayClient;
import ${PACKAGE}.joinpay.sdk.JoinPaySignature;
import ${PACKAGE}.joinpay.sdk.JoinPayRsaSignature;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 汇聚支付服务类
 *
 * 封装下单、查询、退款、回调验签等核心业务方法
 */
@Service
public class JoinPayService {

    private static final Logger log = LoggerFactory.getLogger(JoinPayService.class);

    @Autowired
    private JoinPayClient joinPayClient;

    @Autowired
    private JoinPayConfig joinPayConfig;

    /**
     * 统一支付下单 - 扫码支付（微信/支付宝/银联）
     *
     * @param orderNo     商户订单号
     * @param amount      订单金额（元）
     * @param productName 商品名称
     * @param frpCode     交易类型（如 WEIXIN_NATIVE、ALIPAY_NATIVE）
     * @return 下单结果
     */
    public JSONObject createScanPayOrder(String orderNo, String amount,
                                          String productName, String frpCode) throws Exception {
        return createScanPayOrder(orderNo, amount, productName, frpCode, null);
    }

    /**
     * 统一支付下单 - 扫码支付（带回调地址）
     */
    public JSONObject createScanPayOrder(String orderNo, String amount,
                                          String productName, String frpCode,
                                          String notifyUrl) throws Exception {
        JoinPayClient.UnifiedPayRequest request = new JoinPayClient.UnifiedPayRequest();
        request.orderNo = orderNo;
        request.amount = amount;
        request.productName = productName;
        request.frpCode = frpCode;
        request.notifyUrl = notifyUrl != null ? notifyUrl : joinPayConfig.getDefaultNotifyUrl();

        log.info("创建扫码支付订单: orderNo={}, amount={}, frpCode={}", orderNo, amount, frpCode);
        return joinPayClient.unifiedPay(request);
    }

    /**
     * 统一支付下单 - 微信公众号/小程序支付
     *
     * @param orderNo     商户订单号
     * @param amount      订单金额（元）
     * @param productName 商品名称
     * @param frpCode     交易类型（WEIXIN_GZH 或 WEIXIN_XCX）
     * @param openId      微信用户OpenId
     * @param appId       微信AppId
     * @return 下单结果
     */
    public JSONObject createWxJsapiOrder(String orderNo, String amount,
                                          String productName, String frpCode,
                                          String openId, String appId) throws Exception {
        JoinPayClient.UnifiedPayRequest request = new JoinPayClient.UnifiedPayRequest();
        request.orderNo = orderNo;
        request.amount = amount;
        request.productName = productName;
        request.frpCode = frpCode;
        request.openId = openId;
        request.appId = appId;
        request.notifyUrl = joinPayConfig.getDefaultNotifyUrl();

        log.info("创建微信JSAPI订单: orderNo={}, openId={}, appId={}", orderNo, openId, appId);
        return joinPayClient.unifiedPay(request);
    }

    /**
     * 统一支付下单 - 付款码支付（被扫）
     *
     * @param orderNo     商户订单号
     * @param amount      订单金额（元）
     * @param productName 商品名称
     * @param frpCode     交易类型（WEIXIN_CARD 或 ALIPAY_CARD）
     * @param authCode    付款码
     * @return 下单结果
     */
    public JSONObject createCardPayOrder(String orderNo, String amount,
                                          String productName, String frpCode,
                                          String authCode) throws Exception {
        JoinPayClient.UnifiedPayRequest request = new JoinPayClient.UnifiedPayRequest();
        request.orderNo = orderNo;
        request.amount = amount;
        request.productName = productName;
        request.frpCode = frpCode;
        request.authCode = authCode;
        request.notifyUrl = joinPayConfig.getDefaultNotifyUrl();

        log.info("创建付款码支付订单: orderNo={}, frpCode={}", orderNo, frpCode);
        return joinPayClient.unifiedPay(request);
    }

    /**
     * 订单查询
     *
     * @param orderNo 商户订单号
     * @return 订单信息
     */
    public JSONObject queryOrder(String orderNo) throws Exception {
        log.info("查询订单: orderNo={}", orderNo);
        return joinPayClient.queryOrder(orderNo);
    }

    /**
     * 退款申请
     *
     * @param orderNo        原支付订单号
     * @param refundOrderNo  退款订单号
     * @param refundAmount   退款金额（元）
     * @param reason         退款原因
     * @return 退款结果
     */
    public JSONObject refund(String orderNo, String refundOrderNo,
                              String refundAmount, String reason) throws Exception {
        JoinPayClient.RefundRequest request = new JoinPayClient.RefundRequest();
        request.orderNo = orderNo;
        request.refundOrderNo = refundOrderNo;
        request.refundAmount = refundAmount;
        request.reason = reason;

        log.info("申请退款: orderNo={}, refundOrderNo={}, amount={}", orderNo, refundOrderNo, refundAmount);
        return joinPayClient.refund(request);
    }

    /**
     * 退款查询
     *
     * @param refundOrderNo 退款订单号
     * @return 退款信息
     */
    public JSONObject queryRefund(String refundOrderNo) throws Exception {
        log.info("查询退款: refundOrderNo={}", refundOrderNo);
        return joinPayClient.queryRefund(refundOrderNo);
    }

    /**
     * 关闭订单
     *
     * @param orderNo 商户订单号
     * @param frpCode 交易类型
     * @return 关闭结果
     */
    public JSONObject closeOrder(String orderNo, String frpCode) throws Exception {
        log.info("关闭订单: orderNo={}, frpCode={}", orderNo, frpCode);
        return joinPayClient.closeOrder(orderNo, frpCode, null);
    }

    /**
     * 验证回调签名
     *
     * @param params 回调参数
     * @return 验签是否通过
     */
    public boolean verifyCallbackSign(Map<String, Object> params) {
        String signType = joinPayConfig.getSignType();

        try {
            if ("RSA".equalsIgnoreCase(signType)) {
                // RSA验签
                String rsaPublicKey = joinPayConfig.getRsaPublicKey();
                return JoinPayRsaSignature.verify(params, rsaPublicKey);
            } else {
                // MD5验签
                String merchantKey = joinPayConfig.getMerchantKey();
                return JoinPaySignature.verify(params, merchantKey);
            }
        } catch (Exception e) {
            log.error("验签失败", e);
            return false;
        }
    }

    /**
     * 解析回调参数
     *
     * @param queryParams URL查询参数或表单参数
     * @return 参数Map
     */
    public Map<String, Object> parseCallbackParams(Map<String, String[]> queryParams) {
        Map<String, Object> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), values[0]);
            }
        }
        return params;
    }

    /**
     * 判断订单是否支付成功
     *
     * @param callbackParams 回调参数
     * @return 是否支付成功
     */
    public boolean isPaySuccess(Map<String, Object> callbackParams) {
        Object code = callbackParams.get("ra_Code");
        return code != null && "100".equals(code.toString());
    }

    /**
     * 从回调参数中提取订单信息
     *
     * @param callbackParams 回调参数
     * @return 订单信息
     */
    public Map<String, Object> extractOrderInfo(Map<String, Object> callbackParams) {
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("orderNo", callbackParams.get("r2_OrderNo"));
        orderInfo.put("amount", callbackParams.get("r3_Amount"));
        orderInfo.put("transactionId", callbackParams.get("r5_TrxNo"));
        orderInfo.put("payTime", callbackParams.get("rp_PayTime"));
        orderInfo.put("frpCode", callbackParams.get("r1_FrpCode"));
        return orderInfo;
    }
}
