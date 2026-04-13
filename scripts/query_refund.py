#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
汇聚支付 排障辅助脚本 - 退款查询

用于排查退款进度、退款失败原因等问题。
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


def query_refund(config, refund_order_no, version="2.3"):
    params = {
        "p0_Version": version,
        "p1_MerchantNo": config.get("default_merchant_no", ""),
        "p2_RefundOrderNo": refund_order_no,
    }
    merchant_key = config.get("merchant_key", "")
    hmac_val = sign(params, merchant_key)
    params["hmac"] = hmac_val

    encoded_data = urllib.parse.urlencode(params).encode("utf-8")
    url = config["base_url"].rstrip("/") + "/tradeRt/queryRefund"

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


def analyze_refund_result(result):
    """分析退款查询结果"""
    analysis = {
        "refund_order_no": result.get("r2_RefundOrderNo", ""),
        "refund_amount": result.get("r3_RefundAmount", ""),
        "refund_trx_no": result.get("r4_RefundTrxNo", ""),
        "complete_time": result.get("r5_RefundCompleteTime", ""),
        "status_raw": result.get("ra_Status", ""),
        "code": result.get("rb_Code", ""),
        "msg": result.get("rc_CodeMsg", ""),
    }
    
    code = analysis["code"]
    
    if code == "100":
        status = analysis["status_raw"]
        if status and status.upper() in ("SUCCESS", "100"):
            analysis["status_text"] = "✅ 退款成功"
            analysis["suggestion"] = (
                "退款已成功！\n"
                "注意: 实际到账时间取决于银行处理速度:\n"
                "  - 微信支付: 通常1~3个工作日\n"
                "  - 支付宝: 可能实时到账或1~3个工作日\n"
                "  - 银联: 通常1~5个工作日"
            )
        elif status and status.upper() in ("PROCESSING", "REFUNDING"):
            analysis["status_text"] = "🔄 退款中"
            analysis["suggestion"] = (
                "退款正在处理中，渠道尚未返回最终结果。\n"
                "建议: 等待一段时间后再次查询。通常15分钟内会有更新。"
            )
        else:
            analysis["status_text"] = f"⏳ 退单状态: {status}"
            analysis["suggestion"] = "等待进一步处理"
    elif code.startswith("2009"):
        error_hints = {
            "20090001": "原支付订单不存在 — 请先确认原订单号是否正确",
            "20090002": "原订单未支付 — 未支付的订单无法退款",
            "20090003": "退款金额有误 — 检查格式(两位小数)",
            "20090004": "退款金额超出可退金额 — 用queryRefundInfo查剩余可退金额",
            "20090005": "签名验证失败 — 检查密钥和签名算法",
            "20090007": "退款订单号重复 — 更换新单号或视为retry",
            "20090011": "退款金额为0 — 必须大于0.01元",
            "20090012": "原订单已全额退款 — 无法再次退款",
            "20090013": "退款原因不能为空 — 填写退款原因",
        }
        hint = error_hints.get(code, "详见响应码完整列表")
        analysis["status_text"] = f"❌ 退款失败 ({code})"
        analysis["suggestion"] = f"{analysis['msg']}\n→ {hint}"
    else:
        analysis["status_text"] = f"❌ 查询异常 ({code})"
        analysis["suggestion"] = analysis["msg"]

    return analysis


def main():
    parser = argparse.ArgumentParser(description="汇聚支付 - 退款查询排障脚本")
    parser.add_argument("--refund-order-no", required=True, help="退款订单号")
    parser.add_argument("--merchant-no", help="商户编号（默认用配置文件）")
    parser.add_argument("--version", default="2.3", help="版本号(默认2.3)")
    args = parser.parse_args()

    config = load_config()
    if args.merchant_no:
        config["default_merchant_no"] = args.merchant_no

    print(f"\n{'='*50}")
    print(f"汇聚支付 - 退款查询排障")
    print(f"{'='*50}")
    print(f"退款订单号: {args.refund_order_no}")

    result, error = query_refund(config, args.refund_order_no, args.version)

    if error:
        print(f"\n❌ 请求失败: {error}")
        sys.exit(1)

    analysis = analyze_refund_result(result)

    print(f"\n{'─'*50}")
    print(f"【基础信息】")
    print(f"  响应码:       {analysis['code']}")
    print(f"  状态:         {analysis['status_text']}")

    print(f"\n【退款详情】")
    print(f"  退款金额:     {analysis['refund_amount'] or '(空)'} 元")
    print(f"  退款流水号:   {analysis['refund_trx_no'] or '(空)'}")
    print(f"  完成时间:     {analysis['complete_time'] or '(空)'}")

    print(f"\n{'─'*50}")
    print(f"【分析与建议】")
    print(analysis["suggestion"])
    print(f"{'='*50}\n")


if __name__ == "__main__":
    main()
