package ${PACKAGE}.joinpay;

import ${PACKAGE}.joinpay.sdk.JoinPayClient;
import ${PACKAGE}.joinpay.sdk.JoinPaySignature;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 汇聚支付接口控制器
 *
 * 提供下单、查询、退款的REST API
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private JoinPayService joinPayService;

    /**
     * 创建扫码支付订单
     *
     * POST /api/pay/create
     *
     * 请求参数：
     * - orderNo: 商户订单号
     * - amount: 订单金额（元）
     * - productName: 商品名称
     * - frpCode: 交易类型（如 WEIXIN_NATIVE、ALIPAY_NATIVE）
     * - notifyUrl: 回调地址（可选）
     *
     * @return 下单结果（包含二维码URL）
     */
    @PostMapping("/create")
    public Map<String, Object> createPayOrder(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String orderNo = params.get("orderNo");
            String amount = params.get("amount");
            String productName = params.get("productName");
            String frpCode = params.get("frpCode");
            String notifyUrl = params.get("notifyUrl");

            // 参数校验
            if (orderNo == null || amount == null || productName == null || frpCode == null) {
                result.put("success", false);
                result.put("message", "缺少必要参数：orderNo, amount, productName, frpCode");
                return result;
            }

            // 调用下单接口
            JSONObject response = joinPayService.createScanPayOrder(
                orderNo, amount, productName, frpCode, notifyUrl
            );

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "下单异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 创建微信JSAPI支付订单（公众号/小程序）
     *
     * POST /api/pay/wxjsapi
     *
     * 请求参数：
     * - orderNo: 商户订单号
     * - amount: 订单金额（元）
     * - productName: 商品名称
     * - frpCode: 交易类型（WEIXIN_GZH 或 WEIXIN_XCX）
     * - openId: 微信用户OpenId
     * - appId: 微信AppId
     *
     * @return 下单结果（包含调起支付的参数）
     */
    @PostMapping("/wxjsapi")
    public Map<String, Object> createWxJsapiOrder(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String orderNo = params.get("orderNo");
            String amount = params.get("amount");
            String productName = params.get("productName");
            String frpCode = params.get("frpCode");
            String openId = params.get("openId");
            String appId = params.get("appId");

            // 参数校验
            if (orderNo == null || amount == null || openId == null || appId == null) {
                result.put("success", false);
                result.put("message", "缺少必要参数：orderNo, amount, openId, appId");
                return result;
            }

            // 调用下单接口
            JSONObject response = joinPayService.createWxJsapiOrder(
                orderNo, amount, productName, frpCode, openId, appId
            );

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "下单异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 创建微信JSAPI支付订单（公众号/小程序）
     *
     * POST /api/pay/wxjsapi
     *
     * 请求参数：
     * - orderNo: 商户订单号
     * - amount: 订单金额（元）
     * - productName: 商品名称
     * - frpCode: 交易类型（WEIXIN_GZH 或 WEIXIN_XCX）
     * - openId: 微信用户OpenId
     * - appId: 微信AppId
     *
     * @return 下单结果（包含调起支付的参数）
     */
    @PostMapping("/wxjsapi")
    public Map<String, Object> createWxJsapiOrder(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String orderNo = params.get("orderNo");
            String amount = params.get("amount");
            String productName = params.get("productName");
            String frpCode = params.get("frpCode");
            String openId = params.get("openId");
            String appId = params.get("appId");

            // 参数校验
            if (orderNo == null || amount == null || openId == null || appId == null) {
                result.put("success", false);
                result.put("message", "缺少必要参数：orderNo, amount, openId, appId");
                return result;
            }

            // 调用下单接口
            JSONObject response = joinPayService.createWxJsapiOrder(
                orderNo, amount, productName, frpCode, openId, appId
            );

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "下单异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 创建付款码支付订单（被扫）
     *
     * POST /api/pay/card
     *
     * 请求参数：
     * - orderNo: 商户订单号
     * - amount: 订单金额（元）
     * - productName: 商品名称
     * - frpCode: 交易类型（WEIXIN_CARD 或 ALIPAY_CARD）
     * - authCode: 付款码
     *
     * @return 支付结果
     */
    @PostMapping("/card")
    public Map<String, Object> createCardPayOrder(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String orderNo = params.get("orderNo");
            String amount = params.get("amount");
            String productName = params.get("productName");
            String frpCode = params.get("frpCode");
            String authCode = params.get("authCode");

            if (orderNo == null || amount == null || authCode == null) {
                result.put("success", false);
                result.put("message", "缺少必要参数：orderNo, amount, authCode");
                return result;
            }

            JSONObject response = joinPayService.createCardPayOrder(
                orderNo, amount, productName, frpCode, authCode
            );

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "支付异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 查询订单
     *
     * GET /api/pay/query?orderNo=xxx
     *
     * @param orderNo 商户订单号
     * @return 订单信息
     */
    @GetMapping("/query")
    public Map<String, Object> queryOrder(@RequestParam String orderNo) {
        Map<String, Object> result = new HashMap<>();

        try {
            JSONObject response = joinPayService.queryOrder(orderNo);

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 申请退款
     *
     * POST /api/pay/refund
     *
     * 请求参数：
     * - orderNo: 原支付订单号
     * - refundOrderNo: 退款订单号
     * - refundAmount: 退款金额（元）
     * - reason: 退款原因
     *
     * @return 退款结果
     */
    @PostMapping("/refund")
    public Map<String, Object> refund(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String orderNo = params.get("orderNo");
            String refundOrderNo = params.get("refundOrderNo");
            String refundAmount = params.get("refundAmount");
            String reason = params.get("reason");

            if (orderNo == null || refundOrderNo == null || refundAmount == null) {
                result.put("success", false);
                result.put("message", "缺少必要参数：orderNo, refundOrderNo, refundAmount");
                return result;
            }

            JSONObject response = joinPayService.refund(
                orderNo, refundOrderNo, refundAmount, reason
            );

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "退款异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 查询退款
     *
     * GET /api/pay/refund/query?refundOrderNo=xxx
     *
     * @param refundOrderNo 退款订单号
     * @return 退款信息
     */
    @GetMapping("/refund/query")
    public Map<String, Object> queryRefund(@RequestParam String refundOrderNo) {
        Map<String, Object> result = new HashMap<>();

        try {
            JSONObject response = joinPayService.queryRefund(refundOrderNo);

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 关闭订单
     *
     * POST /api/pay/close
     *
     * 请求参数：
     * - orderNo: 商户订单号
     * - frpCode: 交易类型
     *
     * @return 关闭结果
     */
    @PostMapping("/close")
    public Map<String, Object> closeOrder(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String orderNo = params.get("orderNo");
            String frpCode = params.get("frpCode");

            if (orderNo == null || frpCode == null) {
                result.put("success", false);
                result.put("message", "缺少必要参数：orderNo, frpCode");
                return result;
            }

            JSONObject response = joinPayService.closeOrder(orderNo, frpCode);

            // 返回原始响应，让商户自行选择需要的字段
            String raCode = response.getString("ra_Code");
            result.put("success", "100".equals(raCode));
            result.put("data", response);  // 原封不动返回汇聚支付的完整响应

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "关闭异常: " + e.getMessage());
        }

        return result;
    }
}
