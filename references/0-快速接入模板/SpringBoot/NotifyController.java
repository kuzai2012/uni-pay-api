package ${PACKAGE}.joinpay;

import ${PACKAGE}.joinpay.sdk.JoinPayClient;
import ${PACKAGE}.joinpay.sdk.JoinPaySignature;
import ${PACKAGE}.joinpay.sdk.JoinPayRsaSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 汇聚支付回调通知控制器
 *
 * 接收汇聚支付的异步支付结果通知
 *
 * 重要说明：
 * 1. 回调地址需要外网可访问
 * 2. 必须验签后才处理业务
 * 3. 必须返回 success 表示接收成功
 * 4. 需要做幂等处理，避免重复处理同一笔订单
 */
@RestController
@RequestMapping("/notify")
public class NotifyController {

    private static final Logger log = LoggerFactory.getLogger(NotifyController.class);

    @Autowired
    private JoinPayService joinPayService;

    /**
     * 支付结果异步通知
     *
     * POST /notify/pay
     *
     * 汇聚支付会在支付成功后调用此接口通知商户
     * 商户需要验签并处理业务逻辑
     *
     * @param request HTTP请求
     * @return success 或 fail
     */
    @PostMapping("/pay")
    public String payNotify(HttpServletRequest request) {
        log.info("收到支付回调通知");

        try {
            // 1. 解析回调参数
            Map<String, Object> params = parseRequestParams(request);
            log.info("回调参数: {}", params);

            // 2. 验证签名
            boolean signValid = joinPayService.verifyCallbackSign(params);
            if (!signValid) {
                log.error("回调验签失败!");
                return "fail";
            }

            // 3. 判断支付结果
            if (!joinPayService.isPaySuccess(params)) {
                log.warn("支付失败: {}", params.get("rb_CodeMsg"));
                return "fail";
            }

            // 4. 提取订单信息
            Map<String, Object> orderInfo = joinPayService.extractOrderInfo(params);
            String orderNo = (String) orderInfo.get("orderNo");
            String amount = (String) orderInfo.get("amount");
            String transactionId = (String) orderInfo.get("transactionId");

            log.info("支付成功: orderNo={}, amount={}, transactionId={}", orderNo, amount, transactionId);

            // 5. 业务处理（请根据实际业务修改）
            // TODO: 处理订单状态更新、发货等业务逻辑
            // 注意：需要做幂等处理，避免重复处理
            handlePaySuccess(orderInfo);

            // 6. 返回成功
            return "success";

        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            return "fail";
        }
    }

    /**
     * 退款结果异步通知
     *
     * POST /notify/refund
     *
     * @param request HTTP请求
     * @return success 或 fail
     */
    @PostMapping("/refund")
    public String refundNotify(HttpServletRequest request) {
        log.info("收到退款回调通知");

        try {
            Map<String, Object> params = parseRequestParams(request);

            // 验签
            boolean signValid = joinPayService.verifyCallbackSign(params);
            if (!signValid) {
                log.error("退款回调验签失败!");
                return "fail";
            }

            String refundOrderNo = (String) params.get("r2_RefundOrderNo");
            String refundStatus = (String) params.get("r4_RefundStatus");

            log.info("退款结果: refundOrderNo={}, status={}", refundOrderNo, refundStatus);

            // TODO: 处理退款结果
            handleRefundResult(params);

            return "success";

        } catch (Exception e) {
            log.error("处理退款回调异常", e);
            return "fail";
        }
    }

    /**
     * 解析HTTP请求参数
     */
    private Map<String, Object> parseRequestParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();

        // 从表单参数中获取
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }

        return params;
    }

    /**
     * 处理支付成功业务逻辑
     *
     * 请根据实际业务修改此方法
     */
    private void handlePaySuccess(Map<String, Object> orderInfo) {
        String orderNo = (String) orderInfo.get("orderNo");
        String amount = (String) orderInfo.get("amount");
        String transactionId = (String) orderInfo.get("transactionId");

        // TODO: 实现你的业务逻辑
        // 示例：
        // 1. 检查订单是否已处理（幂等）
        // 2. 更新订单状态为"已支付"
        // 3. 记录支付流水
        // 4. 触发后续业务（发货、开通会员等）

        log.info("处理支付成功业务: orderNo={}, transactionId={}", orderNo, transactionId);
    }

    /**
     * 处理退款结果
     */
    private void handleRefundResult(Map<String, Object> params) {
        String refundOrderNo = (String) params.get("r2_RefundOrderNo");
        String refundStatus = (String) params.get("r4_RefundStatus");

        // TODO: 实现退款结果处理逻辑
        log.info("处理退款结果: refundOrderNo={}, status={}", refundOrderNo, refundStatus);
    }
}
