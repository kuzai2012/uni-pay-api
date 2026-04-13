package com.joinpay.sdk.example;

import com.alibaba.fastjson.JSONObject;
import com.joinpay.sdk.JoinPayClient;

/**
 * 3. 退款申请示例
 *
 * 发起退款操作。支持部分退款，可多次退款直至全额。
 */
public class RefundExample {

    public static void main(String[] args) throws Exception {
        JoinPayClient client = new JoinPayClient(
            "https://trade.joinpay.com",
            "your_merchant_no",
            "your_merchant_key_32chars",
            null,
            "MD5"
        );

        // 构建退款请求
        JoinPayClient.RefundRequest request = new JoinPayClient.RefundRequest();
        request.orderNo       = "ORDER20260413001";           // 原支付订单号
        request.refundOrderNo = "REF_ORDER20260413001";       // 退款订单号(唯一!)
        request.refundAmount  = "0.01";                        // 退款金额
        request.reason        = "用户申请退款";                 // 退款原因
        request.notifyUrl     = "https://your-domain.com/refund_notify";

        // 发起退款
        JSONObject response = client.refund(request);
        String rb_code = response.getString("rb_Code");

        if ("100".equals(rb_code)) {
            // ✅ 退款成功
            String refundTrxNo = response.getString("r5_RefundTrxNo");
            System.out.println("✅ 退款申请已提交! 流水号: " + refundTrxNo);
            System.out.println("注意: 退款到账需1~3个工作日");
        } else {
            // ❌ 退款失败
            String rc_msg = response.getString("rc_CodeMsg");
            System.out.println("❌ 退款失败! code=" + rb_code + ", msg=" + rc_msg);

            // 常见错误处理建议:
            // 20090002 - 原订单未支付 → 先确认原订单状态
            // 20090004 - 退款金额超限 → 用queryRefundInfo查剩余可退
            // 20090007 - 退单号重复 → 更换新的退款单号
        }
    }
}
