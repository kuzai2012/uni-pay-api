package com.joinpay.sdk.example;

import com.alibaba.fastjson.JSONObject;
import com.joinpay.sdk.JoinPayClient;

/**
 * 7. 资金查询示例
 *
 * 查询发货管理订单的资金状态，包括冻结金额、解冻金额、已退金额等信息。
 * 主要用于微信发货管理的资管订单。
 */
public class QueryFundsExample {

    public static void main(String[] args) throws Exception {
        JoinPayClient client = new JoinPayClient(
            "https://trade.joinpay.com",
            "your_merchant_no",
            "your_merchant_key_32chars",
            null,
            "MD5"
        );

        String orderNo = "ORDER20260413001";
        JSONObject response = client.queryFunds(orderNo);

        String rb_code = response.getString("rb_Code");
        if ("100".equals(rb_code)) {
            String trxNo          = response.getString("r3_TrxNo");          // 交易流水号
            String orderAmount    = response.getString("r4_OrderAmount");    // 订单金额
            String fee            = response.getString("r5_Fee");            // 手续费
            String freezeAmount   = response.getString("r6_FreezeAmount");  // 冻结金额
            String refundAmount   = response.getString("r7_RefundAmount");  // 已退金额
            String unfreezeAmount = response.getString("r8_UnfreezeAmount");// 解冻金额
            String unfreezeTime   = response.getString("r9_UnfreezeTime");  // 解冻时间
            String ra_status      = response.getString("ra_Status");       // 当前状态

            System.out.println("=== 资金查询结果 ===");
            System.out.println("订单号:     " + orderNo);
            System.out.println("交易流水号: " + trxNo);
            System.out.println("订单金额:   " + orderAmount);
            System.out.println("手续费:     " + fee);
            System.out.println("冻结金额:   " + freezeAmount);
            System.out.println("已退金额:   " + refundAmount);
            System.out.println("解冻金额:   " + unfreezeAmount);
            System.out.println("解冻时间:   " + unfreezeTime);
            System.out.println("当前状态:   " + ra_status);
        } else {
            String rc_msg = response.getString("rc_CodeMsg");
            System.out.println("查询失败: " + rc_msg);
        }
    }
}
