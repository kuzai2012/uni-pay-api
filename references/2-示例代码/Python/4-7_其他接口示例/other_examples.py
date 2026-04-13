#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
4~7. 其他接口示例（退款查询/退款信息查询/关闭订单/资金查询）

由于代码模式一致，这里合并展示四个接口的调用方式。
实际使用时可拆分为独立文件。
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from SDK工具类.joinpay_client import JoinPayClient


def demo_query_refund(client):
    """4. 退款查询 — 根据退款订单号查询单笔退款详情"""
    resp = client.query_refund("REF_ORDER20260413001")
    rb_code = resp.get("rb_Code", "")
    if rb_code == "100":
        print(f"退款金额: {resp.get('r3_RefundAmount', '')}")
        print(f"完成时间: {resp.get('r5_RefundCompleteTime', '')}")
        print(f"状态:     {resp.get('ra_Status', '')}")
    else:
        print(f"查询失败: {resp.get('rc_CodeMsg', '')}")


def demo_query_refund_info(client):
    """5. 退款信息查询 — 根据支付订单号查询全部退款记录（适合对账）"""
    resp = client.query_refund_info("ORDER20260413001")
    rb_code = resp.get("rb_Code", "")
    if rb_code == "100":
        print(f"原订单金额:       {resp.get('r3_OrderAmount', '')}")
        print(f"剩余可退本金:     {resp.get('r5_RemainOrderAmount', '')}")
        print(f"退款记录列表:\n{resp.get('r7_RefundInfo', '')}")
    else:
        print(f"查询失败: {resp.get('rc_CodeMsg', '')}")


def demo_close_order(client):
    """6. 关闭订单 — 关闭未支付的订单"""
    resp = client.close_order(
        order_no="ORDER20260413001",
        frp_code="WEIXIN_GZH",  # 必须与下单时的FrpCode一致
        trx_no=""              # 可选：交易流水号
    )
    rb_code = resp.get("rb_Code", "")
    if rb_code == "100":
        print(f"✅ 关单成功! 状态: {resp.get('ra_Status', '')}")
    else:
        print(f"❌ 关单失败! code={rb_code}, msg={resp.get('rc_CodeMsg', '')}")
        # 10083006 - 订单已支付或已关闭(无法关单)
        # 10083007 - 订单不存在


def demo_query_funds(client):
    """7. 资金查询 — 发货管理订单的资金状态查询"""
    resp = client.query_funds("ORDER20260413001")
    rb_code = resp.get("rb_Code", "")
    if rb_code == "100":
        print(f"订单金额:   {resp.get('r4_OrderAmount', '')}")
        print(f"手续费:     {resp.get('r5_Fee', '')}")
        print(f"冻结金额:   {resp.get('r6_FreezeAmount', '')}")
        print(f"已退金额:   {resp.get('r7_RefundAmount', '')}")
        print(f"当前状态:   {resp.get('ra_Status', '')}")
    else:
        print(f"查询失败: {resp.get('rc_CodeMsg', '')}")


def main():
    client = JoinPayClient(
        base_url="https://trade.joinpay.com",
        merchant_no="your_merchant_no",
        merchant_key="your_merchant_key_32chars",
        sign_type="MD5"
    )

    print("=== 4. 退款查询 ===")
    demo_query_refund(client)

    print("\n=== 5. 退款信息查询 ===")
    demo_query_refund_info(client)

    print("\n=== 6. 关闭订单 ===")
    demo_close_order(client)

    print("\n=== 7. 资金查询 ===")
    demo_query_funds(client)


if __name__ == "__main__":
    main()
