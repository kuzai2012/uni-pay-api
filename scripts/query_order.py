#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
汇聚支付 排障辅助脚本 - 订单查询

用于排查支付结果、回调丢失等问题。
基于 uni_pay_client.py 的签名逻辑，增加了结果分析和状态解读。
"""

import argparse
import json
import os
import sys
import urllib.parse
import urllib.request
import ssl

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SKILL_DIR = os.path.dirname(SCRIPT_DIR)
CONFIG_PATH = os.path.join(SKILL_DIR, "config.json")


def load_config():
    if not os.path.exists(CONFIG_PATH):
        print(json.dumps({"error": f"配置文件不存在: {CONFIG_PATH}"}, ensure_ascii=False))
        sys.exit(1)
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def sign(params, key):
    """MD5签名（与SignBiz.java一致）"""
    filtered = {}
    for k, v in params.items():
        if k.lower() == "hmac":
            continue
        val = str(v).strip() if v is not None else ""
        if val != "":
            filtered[k] = val
    sorted_keys = sorted(filtered.keys())
    sign_str = "".join(filtered[k] for k in sorted_keys)
    return __import__('hashlib').md5((sign_str + key).encode("utf-8")).hexdigest().upper()


def query_order(config, order_no, version="2.6"):
    """查询订单并返回分析后的结构化结果"""
    params = {
        "p0_Version": version,
        "p1_MerchantNo": config.get("default_merchant_no", ""),
        "p2_OrderNo": order_no,
    }

    merchant_key = config.get("merchant_key", "")
    hmac_val = sign(params, merchant_key)
    params["hmac"] = hmac_val

    encoded_data = urllib.parse.urlencode(params).encode("utf-8")
    url = config["base_url"].rstrip("/") + "/tradeRt/queryOrder"

    try:
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        req = urllib.request.Request(url, data=encoded_data, method="POST")
        req.add_header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
        with urllib.request.urlopen(req, timeout=30, context=ctx) as resp:
            body = resp.read().decode("utf-8")
            return json.loads(body), None
    except Exception as e:
        return None, f"请求异常: {str(e)}"


def analyze_result(result):
    """分析查询结果，返回人类可读的解读"""
    analysis = {
        "order_no": result.get("r2_OrderNo", ""),
        "status_raw": result.get("ra_Status", ""),
        "code": result.get("ra_Code", ""),
        "msg": result.get("rb_CodeMsg", ""),
        "trx_no": result.get("r7_TrxNo", ""),
        "amount": result.get("r3_Amount", ""),
        "pay_time": result.get("rc_PayTime", ""),
        "fee": result.get("rd_Fee", ""),
        "bank_type": result.get("re_BankType", ""),
        "frp_code": result.get("rf_FrpCode", ""),
        "buyer_id": result.get("rg_BuyerId", ""),
    }

    # 状态解读
    status = analysis["status_raw"]
    code = analysis["code"]
    
    if code == "100" and status == "100":
        analysis["status_text"] = "✅ 已支付成功"
        analysis["suggestion"] = (
            "订单已正常支付完成。如果用户未收到发货通知，请检查：\n"
            "  1. 回调地址是否可达\n"
            "  2. 回调处理逻辑是否有异常\n"
            "  3. 是否已做幂等处理（可能回调已到达但重复）"
        )
    elif code == "10080020":
        analysis["status_text"] = "⚠️ 查询不到交易记录"
        analysis["suggestion"] = (
            "该订单号在系统中不存在：\n"
            "  1. 确认订单号是否正确\n"
            "  2. 确认商户号是否正确\n"
            "  3. 如果刚下单，可能存在延迟，建议30秒后重试"
        )
    elif code == "10080021":
        analysis["status_text"] = "📕 交易已关闭"
        analysis["suggestion"] = (
            "订单已被关闭（可能是超时自动关闭或主动关单）：\n"
            "  1. 如需重新发起支付，使用新的订单号重新下单\n"
            "  2. 如需确认是否需要退款，检查原订单状态"
        )
    elif code != "100":
        analysis["status_text"] = f"❌ 查询失败 ({code})"
        analysis["suggestion"] = f"错误信息: {analysis['msg']}\n建议查看排障手册中的响应码速查表"
    else:
        status_map = {
            "CREATED": "🔄 已创建待支付",
            "NOTPAY": "⏳ 待支付",
            "CLOSED": "📕 已关闭",
            "FAILED": "❌ 支付失败",
            "REFUNDING": "💰 退款中",
            "REFUND": "↩️ 已退款",
        }
        analysis["status_text"] = status_map.get(status, f"未知状态({status})")
        analysis["suggestion"] = "根据当前状态决定下一步操作"

    return analysis


def main():
    parser = argparse.ArgumentParser(description="汇聚支付 - 订单查询排障脚本")
    parser.add_argument("--order-no", required=True, help="商户订单号")
    parser.add_argument("--merchant-no", help="商户编号（默认用配置文件）")
    parser.add_argument("--version", default="2.6", help="版本号")
    args = parser.parse_args()

    config = load_config()
    
    # 覆盖商户号
    if args.merchant_no:
        config["default_merchant_no"] = args.merchant_no

    print(f"\n{'='*50}")
    print(f"汇聚支付 - 订单查询排障")
    print(f"{'='*50}")
    print(f"订单号: {args.order_no}")

    result, error = query_order(config, args.order_no, args.version)
    
    if error:
        print(f"\n❌ 请求失败: {error}")
        sys.exit(1)

    analysis = analyze_result(result)

    print(f"\n{'─'*50}")
    print(f"【基础信息】")
    print(f"  响应码:   {analysis['code']}")
    print(f"  状态:     {analysis['status_text']}")

    print(f"\n【交易详情】")
    print(f"  交易流水号: {analysis['trx_no'] or '(空)'}")
    print(f"  金额:     {analysis['amount'] or '(空)'} 元")
    print(f"  支付时间: {analysis['pay_time'] or '(空)'}")
    print(f"  手续费:   {analysis['fee'] or '(空)'} 元")
    print(f"  支付渠道: {analysis['frp_code'] or '(空)'}")

    print(f"\n{'─'*50}")
    print(f"【分析与建议】")
    print(analysis["suggestion"])
    print(f"{'='*50}\n")


if __name__ == "__main__":
    main()
