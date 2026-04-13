package com.joinpay.sdk.example;

import com.alibaba.fastjson.JSONObject;
import com.joinpay.sdk.JoinPayClient;

/**
 * 4. 退款查询示例
 *
 * 根据退款订单号查询单笔退款的状态和详情。
 */
public class QueryRefundExample {

    public static void main(String[] args) throws Exception {
        JoinPayClient client = new JoinPayClient(
            "https://trade.joinpay.com",
            "your_merchant_no",
            "your_merchant_key_32chars",
            null,
            "MD5"
        );

        String refundOrderNo = "REF_ORDER20260413001";
        JSONObject response = client.queryRefund(refundOrderNo);

        String rb_code = response.getString("rb_Code");
        if ("100".equals(rb_code)) {
            String refundAmount       = response.getString("r3_RefundAmount");      // 退款金额
            String refundTrxNo        = response.getString("r4_RefundTrxNo");        // 退款流水号
            String refundCompleteTime = response.getString("r5_RefundCompleteTime"); // 完成时间
            String ra_status          = response.getString("ra_Status");            // 退款状态

            System.out.println("退款单号:   " + refundOrderNo);
            System.out.println("退款金额:   " + refundAmount);
            System.out.println("退款流水号: " + refundTrxNo);
            System.out.println("完成时间:   " + refundCompleteTime);
            System.out.println("状态:       " + translateRefundStatus(ra_status));
        } else {
            String rc_msg = response.getString("rc_CodeMsg");
            System.out.println("查询失败: " + rc_msg);
        }
    }

    private static String translateRefundStatus(String status) {
        switch (status != null ? status : "") {
            case "SUCCESS":     return "退款成功";
            case "PROCESSING":  return "退款中";
            case "FAIL":        return "退款失败";
            default:            return status;
        }
    }
}
