#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
1. 统一支付下单示例

演示如何调用汇聚支付统一支付接口。
关键参数: orderNo(唯一), amount(元), productName, frpCode, notifyUrl, tradeMerchantNo
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from SDK工具类.joinpay_client import JoinPayClient


def main():
    # ===== 初始化客户端 =====
    client = JoinPayClient(
        base_url="https://trade.joinpay.com",
        merchant_no="your_merchant_no",
        merchant_key="your_merchant_key_32chars",
        rsa_private_key="",  # RSA模式时填写
        sign_type="MD5",    # MD5 或 RSA
        version="2.6",
    )

    # ===== 构建请求 =====
    response = client.unified_pay(
        order_no=f"ORDER{__import__('time').time()}",  # 唯一订单号
        amount="0.01",                               # 金额(元)
        product_name="测试商品",                        # 商品名称
        frp_code="WEIXIN_GZH",                         # 交易类型
        notify_url="https://your-domain.com/notify",     # 回调地址
        trade_merchant_no="your_trade_merchant_no",     # 报备商户号
        
        # --- 渠道特定参数（根据frpCode选择性传入）---
        open_id="oXXXXXXXXXXXXXXXXX",   # 微信公众号必传
        app_id="wxXXXXXXXXXXXXXXXX",   # 微信AppId
    )

    # ===== 处理结果 =====
    ra_code = response.get("ra_Code", "")
    if ra_code == "100":
        # ✅ 下单成功
        trx_no = response.get("r7_TrxNo", "")       # 平台交易流水号
        result = response.get("rc_Result", "")      # 二维码URL/HTML/跳转链接等
        pic = response.get("rd_Pic", "")            # base64二维码图片

        print(f"✅ 下单成功!")
        print(f"交易流水号: {trx_no}")
        print(f"结果数据:  {result}")

        # 根据不同支付方式处理 rc_Result:
        if "WEIXIN_NATIVE" == "WEIXIN_GZH":  # 实际用变量
            pass  # result是二维码URL，展示给用户扫
    else:
        # ❌ 下单失败
        rb_msg = response.get("rb_CodeMsg", "")
        print(f"❌ 下单失败! code={ra_code}, msg={rb_msg}")
        print("排查建议: 查看[📄排障手册]中的响应码速查表")


if __name__ == "__main__":
    main()
