#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""3. 退款申请示例"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from SDK工具类.joinpay_client import JoinPayClient


def main():
    client = JoinPayClient(
        base_url="https://trade.joinpay.com",
        merchant_no="your_merchant_no",
        merchant_key="your_merchant_key_32chars",
        sign_type="MD5"
    )

    response = client.refund(
        order_no="ORDER20260413001",
        refund_order_no="REF_ORDER20260413001",
        refund_amount="0.01",
        reason="用户申请退款",
        notify_url="https://your-domain.com/refund_notify",
    )

    rb_code = response.get("rb_Code", "")
    if rb_code == "100":
        refund_trx_no = response.get("r5_RefundTrxNo", "")
        print(f"✅ 退款申请已提交! 流水号: {refund_trx_no}")
        print("注意: 退款到账需1~3个工作日")
    else:
        rc_msg = response.get("rc_CodeMsg", "")
        print(f"❌ 退款失败! code={rb_code}, msg={rc_msg}")

        # 常见错误提示:
        # 20090002 - 原订单未支付 → 先确认原订单状态
        # 20090004 - 退款金额超限 → 用queryRefundInfo查剩余可退


if __name__ == "__main__":
    main()
