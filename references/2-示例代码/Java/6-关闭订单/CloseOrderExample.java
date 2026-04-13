package com.joinpay.sdk.example;

import com.alibaba.fastjson.JSONObject;
import com.joinpay.sdk.JoinPayClient;

/**
 * 6. 关闭订单示例
 *
 * 关闭未支付的订单。已支付成功的订单无法关闭（只能退款）。
 * 适用场景: 用户超时未支付、主动取消、库存释放等。
 */
public class CloseOrderExample {

    public static void main(String[] args) throws Exception {
        JoinPayClient client = new JoinPayClient(
            "https://trade.joinpay.com",
            "your_merchant_no",
            "your_merchant_key_32chars",
            null,
            "MD5"
        );

        String orderNo = "ORDER20260413001";
        String frpCode = "WEIXIN_GZH";   // 必须与下单时的FrpCode一致

        JSONObject response = client.closeOrder(orderNo, frpCode, null);

        String rb_code = response.getString("rb_Code");
        if ("100".equals(rb_code)) {
            String ra_status = response.getString("ra_Status");
            System.out.println("✅ 关单成功! 订单号: " + orderNo + ", 状态: " + ra_status);
        } else {
            String rc_msg = response.getString("rc_CodeMsg");
            System.out.println("❌ 关单失败! code=" + rb_code + ", msg=" + rc_msg);

            // 常见错误:
            // 10083006 - 订单已支付或已关闭(无法关单)
            // 10083007 - 订单不存在
            // 10083009 - 重复关单
        }
    }
}
