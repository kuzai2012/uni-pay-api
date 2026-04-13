#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""2. 订单查询示例"""

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

    response = client.query_order("ORDER20260413001")

    ra_code = response.get("ra_Code", "")
    if ra_code == "100":
        status = response.get("ra_Status", "")
        pay_time = response.get("rc_PayTime", "")
        fee = response.get("rd_Fee", "")

        print(f"订单状态: {'已支付' if status == '100' else status}")
        print(f"支付时间: {pay_time}")
        print(f"手续费:   {fee}")
    else:
        print(f"查询失败: {response.get('rb_CodeMsg', '')}")


if __name__ == "__main__":
    main()
