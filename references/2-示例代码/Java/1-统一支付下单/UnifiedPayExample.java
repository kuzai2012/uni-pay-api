package com.joinpay.sdk.example;

import com.alibaba.fastjson.JSONObject;
import com.joinpay.sdk.JoinPayClient;

/**
 * 1. 统一支付下单示例
 *
 * 演示如何调用汇聚支付统一支付接口，支持微信/支付宝/银联各支付方式。
 * 关键参数: orderNo(唯一), amount(元), productName, frpCode, notifyUrl, tradeMerchantNo
 */
public class UnifiedPayExample {

    public static void main(String[] args) throws Exception {
        // ===== 初始化客户端 =====
        JoinPayClient client = new JoinPayClient(
            "https://trade.joinpay.com",   // 生产环境地址
            "your_merchant_no",            // 商户号
            "your_merchant_key_32chars",   // MD5密钥
            null,                           // RSA私钥(MD5模式不需要)
            "MD5"                           // 签名方式: MD5 或 RSA
        );

        // ===== 构建请求 =====
        JoinPayClient.UnifiedPayRequest request = new JoinPayClient.UnifiedPayRequest();
        request.orderNo        = "ORDER" + System.currentTimeMillis();  // 唯一订单号
        request.amount         = "0.01";                               // 金额(元)
        request.productName    = "测试商品";                            // 商品名称
        request.frpCode        = "WEIXIN_GZH";                         // 交易类型
        request.notifyUrl      = "https://your-domain.com/notify";     // 回调地址
        request.tradeMerchantNo = "your_trade_merchant_no";             // 报备商户号

        // --- 渠道特定参数（根据frpCode选择性传入） ---
        // 微信公众号(GZH): 必传 openId + appId
        request.openId = "oXXXXXXXXXXXXXXXXX";  // 用户OpenId
        request.appId  = "wxXXXXXXXXXXXXXXXX";  // 公众号AppId

        // 微信被扫(CARD): 必传 authCode + terminalNo + terminalIp
        // request.authCode   = "134567890123456789";
        // request.terminalNo = "TERMINAL001";
        // request.terminalIp = "192.168.1.100";

        // 支付宝H5: 可选 transactionModel (MODEL1/MODEL2/MODEL3)
        // request.transactionModel = "MODEL1";

        // ===== 发起下单 =====
        JSONObject response = client.unifiedPay(request);

        // ===== 处理结果 =====
        String ra_Code = response.getString("ra_Code");
        if ("100".equals(ra_Code)) {
            // ✅ 下单成功
            String trxNo   = response.getString("r7_TrxNo");       // 平台交易流水号
            String result  = response.getString("rc_Result");      // 二维码URL/HTML/跳转链接等
            String pic     = response.getString("rd_Pic");          // base64二维码图片(如q4=1)

            System.out.println("✅ 下单成功!");
            System.out.println("交易流水号: " + trxNo);
            System.out.println("结果数据:  " + result);

            // 根据不同支付方式处理rc_Result:
            if ("WEIXIN_NATIVE".equals(request.frpCode)) {
                // 主扫: result是二维码URL，展示给用户扫
                System.out.println("请展示二维码给用户扫描: " + result);
            } else if ("WEIXIN_GZH".equals(request.frpCode) || "WEIXIN_XCX".equals(request.frpCode)) {
                // 公众号/小程序: result是调起支付的JSON
                System.out.println("调起支付参数: " + result);
            } else if ("ALIPAY_H5".equals(request.frpCode)) {
                // H5: result可能是HTML或重定向URL
                System.out.println("H5支付页面: " + result);
            }
        } else {
            // ❌ 下单失败
            String rb_CodeMsg = response.getString("rb_CodeMsg");
            System.out.println("❌ 下单失败! code=" + ra_Code + ", msg=" + rb_CodeMsg);
            System.out.println("排查建议: 查看[📄排障手册]中的响应码速查表");
        }
    }
}
