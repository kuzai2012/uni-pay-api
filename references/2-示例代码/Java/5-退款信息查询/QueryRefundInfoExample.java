package com.joinpay.sdk.example;

import com.alibaba.fastjson.JSONObject;
import com.joinpay.sdk.JoinPayClient;

/**
 * 5. 退款信息查询示例
 *
 * 根据支付订单号查询该订单的所有退款记录，
 * 包括剩余可退金额、每次退款的详情等。适合对账使用。
 */
public class QueryRefundInfoExample {

    public static void main(String[] args) throws Exception {
        JoinPayClient client = new JoinPayClient(
            "https://trade.joinpay.com",
            "your_merchant_no",
            "your_merchant_key_32chars",
            null,
            "MD5"
        );

        // 用原支付订单号查询所有退款记录
        String orderNo = "ORDER20260413001";
        JSONObject response = client.queryRefundInfo(orderNo);

        String rb_code = response.getString("rb_Code");
        if ("100".equals(rb_code)) {
            String orderAmount          = response.getString("r3_OrderAmount");       // 原订单金额
            String remainOrderAmount    = response.getString("r5_RemainOrderAmount"); // 剩余可退金额
            String marketAmount         = response.getString("r4_MarketAmount");      // 营销金额
            String remainMarketAmount   = response.getString("r6_RemainMarketAmount"); // 剩余可退营销金
            String refundInfoList       = response.getString("r7_RefundInfo");         // 退款记录(JSON数组)

            System.out.println("=== 退款信息汇总 ===");
            System.out.println("原订单金额:       " + orderAmount);
            System.out.println("剩余可退本金:     " + remainOrderAmount);
            System.out.println("营销金额:         " + marketAmount);
            System.out.println("剩余可退营销金:   " + remainMarketAmount);
            System.out.println("退款记录列表:\n" + formatJson(refundInfoList));
        } else {
            String rc_msg = response.getString("rc_CodeMsg");
            System.out.println("查询失败: " + rc_msg);
            if ("20090022".equals(rb_code)) {
                System.out.println("提示: 该订单从未有过退款记录");
            }
        }
    }

    private static String formatJson(String json) {
        if (json == null || json.isEmpty()) return "(空)";
        try {
            // 简单格式化: 压缩多余空白后输出
            return json.replaceAll("\\s+", " ");
        } catch (Exception e) {
            return json;
        }
    }
}
