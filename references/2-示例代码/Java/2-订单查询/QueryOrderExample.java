package com.joinpay.sdk.example;

import com.alibaba.fastjson.JSONObject;
import com.joinpay.sdk.JoinPayClient;

/**
 * 2. 订单查询示例
 *
 * 用于查询订单的支付状态和详细信息。
 */
public class QueryOrderExample {

    public static void main(String[] args) throws Exception {
        JoinPayClient client = new JoinPayClient(
            "https://trade.joinpay.com",
            "your_merchant_no",
            "your_merchant_key_32chars",
            null,
            "MD5"
        );

        // 查询指定订单
        String orderNo = "ORDER20260413001";
        JSONObject response = client.queryOrder(orderNo);

        String ra_Code = response.getString("ra_Code");
        if ("100".equals(ra_Code)) {
            String status    = response.getString("ra_Status");   // 订单状态
            String payTime   = response.getString("rc_PayTime");  // 支付时间
            String fee       = response.getString("rd_Fee");       // 手续费
            String bankType  = response.getString("re_BankType");  // 银行卡类型

            System.out.println("订单号: " + orderNo);
            System.out.println("状态:   " + translateStatus(status));
            System.out.println("支付时间: " + payTime);
            System.out.println("手续费:  " + fee);
        } else {
            String msg = response.getString("rb_CodeMsg");
            System.out.println("查询失败: " + msg);
        }
    }

    private static String translateStatus(String status) {
        switch (status != null ? status : "") {
            case "100": return "已支付(SUCCESS)";
            default:   return status; // CREATED/CLOSED/FAILED 等
        }
    }
}
