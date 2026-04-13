#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
汇聚支付聚合支付 API CLI 客户端
用于通过命令行调用聚合支付接口（支付/查询/退款/关单/资金查询）
不依赖第三方库，仅使用 Python 标准库
"""

import argparse
import hashlib
import json
import os
import sys
import urllib.parse
import urllib.request
import ssl

# Skill base directory (config.json 所在目录)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SKILL_DIR = os.path.dirname(SCRIPT_DIR)
CONFIG_PATH = os.path.join(SKILL_DIR, "config.json")

# 当前接口版本号
CURRENT_VERSION = "2.6"

# 接口路径
API_PATHS = {
    "pay": "/tradeRt/uniPay",
    "query": "/tradeRt/queryOrder",
    "refund": "/tradeRt/refund",
    "query_refund": "/tradeRt/queryRefund",
    "query_refund_info": "/tradeRt/queryRefundInfo",
    "close": "/tradeRt/closeOrder",
    "query_funds": "/tradeRt/queryFundsControlOrder",
}


def load_config():
    """加载配置文件"""
    if not os.path.exists(CONFIG_PATH):
        print(json.dumps({"error": f"配置文件不存在: {CONFIG_PATH}"}, ensure_ascii=False))
        sys.exit(1)
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def sign(params, key):
    """
    HMAC-MD5 签名算法（与 SignBiz.java 逻辑一致）
    1. 排除 hmac 参数
    2. 按 key 字母升序排序
    3. 拼接 value（不拼接 key）
    4. MD5(拼接值 + key).toUpperCase()
    """
    filtered = {}
    for k, v in params.items():
        if k.lower() == "hmac":
            continue
        val = str(v).strip() if v is not None else ""
        if val != "":
            filtered[k] = val

    sorted_keys = sorted(filtered.keys())
    sign_str = "".join(filtered[k] for k in sorted_keys)
    hmac_val = hashlib.md5((sign_str + key).encode("utf-8")).hexdigest().upper()
    return hmac_val


def send_request(base_url, api_path, params, merchant_key):
    """
    发送 POST 请求到聚合支付网关
    返回 (response_dict, error_message)
    """
    # 生成签名
    hmac_val = sign(params, merchant_key)
    params["hmac"] = hmac_val

    # URL-encode 参数
    encoded_data = urllib.parse.urlencode(params).encode("utf-8")

    url = base_url.rstrip("/") + api_path

    try:
        # 忽略 SSL 证书验证（测试环境可能用自签名证书）
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        req = urllib.request.Request(url, data=encoded_data, method="POST")
        req.add_header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
        with urllib.request.urlopen(req, timeout=30, context=ctx) as resp:
            body = resp.read().decode("utf-8")
            # 尝试 JSON 解析
            try:
                return json.loads(body), None
            except json.JSONDecodeError:
                return {"raw_response": body, "ra_Code": "-1", "rb_CodeMsg": "非JSON响应"}, None
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace") if e.fp else ""
        return None, f"HTTP {e.code}: {body[:500]}"
    except urllib.error.URLError as e:
        return None, f"网络错误: {e.reason}"
    except Exception as e:
        return None, f"请求异常: {str(e)}"


# ==================== 各接口实现 ====================

def cmd_pay(args, config):
    """统一支付请求"""
    params = {}
    params["p0_Version"] = getattr(args, "version", CURRENT_VERSION)
    params["p1_MerchantNo"] = args.merchant_no or config["default_merchant_no"]
    params["p2_OrderNo"] = args.order_no
    params["p3_Amount"] = args.amount
    params["p4_Cur"] = getattr(args, "cur", "1")
    params["p5_ProductName"] = args.product_name
    params["p9_NotifyUrl"] = args.notify_url or config.get("default_notify_url", "")
    params["q1_FrpCode"] = args.frp_code
    params["qa_TradeMerchantNo"] = args.trade_merchant_no or config.get("default_trade_merchant_no", "")

    # 可选参数
    if hasattr(args, "product_desc") and args.product_desc:
        params["p6_ProductDesc"] = args.product_desc
    if hasattr(args, "mp") and args.mp:
        params["p7_Mp"] = args.mp
    if hasattr(args, "return_url") and args.return_url:
        params["p8_ReturnUrl"] = args.return_url
    if hasattr(args, "sub_merchant_no") and args.sub_merchant_no:
        params["q3_SubMerchantNo"] = args.sub_merchant_no
    if hasattr(args, "is_show_pic") and args.is_show_pic:
        params["q4_IsShowPic"] = args.is_show_pic
    if hasattr(args, "open_id") and args.open_id:
        params["q5_OpenId"] = args.open_id
    if hasattr(args, "auth_code") and args.auth_code:
        params["q6_AuthCode"] = args.auth_code
    if hasattr(args, "app_id") and args.app_id:
        params["q7_AppId"] = args.app_id
    if hasattr(args, "terminal_no") and args.terminal_no:
        params["q8_TerminalNo"] = args.terminal_no
    if hasattr(args, "transaction_model") and args.transaction_model:
        params["q9_TransactionModel"] = args.transaction_model
    if hasattr(args, "buyer_id") and args.buyer_id:
        params["qb_buyerId"] = args.buyer_id
    if hasattr(args, "terminal_ip") and args.terminal_ip:
        params["ql_TerminalIp"] = args.terminal_ip
    if hasattr(args, "contract_id") and args.contract_id:
        params["qm_ContractId"] = args.contract_id
    if hasattr(args, "special_info") and args.special_info:
        params["qn_SpecialInfo"] = args.special_info
    if hasattr(args, "extra") and args.extra:
        extra = json.loads(args.extra) if isinstance(args.extra, str) else args.extra
        params.update(extra)

    return send_request(config["base_url"], API_PATHS["pay"], params, config["merchant_key"])


def cmd_query(args, config):
    """订单查询"""
    params = {
        "p0_Version": getattr(args, "version", CURRENT_VERSION),
        "p1_MerchantNo": args.merchant_no or config["default_merchant_no"],
        "p2_OrderNo": args.order_no,
    }
    return send_request(config["base_url"], API_PATHS["query"], params, config["merchant_key"])


def cmd_refund(args, config):
    """退款申请"""
    params = {
        "p0_Version": getattr(args, "version", "2.3"),
        "p1_MerchantNo": args.merchant_no or config["default_merchant_no"],
        "p2_OrderNo": args.order_no,
        "p3_RefundOrderNo": args.refund_order_no,
        "p4_RefundAmount": args.refund_amount,
        "p5_RefundReason": args.reason,
    }
    if hasattr(args, "notify_url") and args.notify_url:
        params["p6_NotifyUrl"] = args.notify_url
    if hasattr(args, "alt_ref_info") and args.alt_ref_info:
        params["p7_AltRefInfo"] = args.alt_ref_info
    if hasattr(args, "funds_account") and args.funds_account:
        params["pa_FundsAccount"] = args.funds_account
    return send_request(config["base_url"], API_PATHS["refund"], params, config["merchant_key"])


def cmd_query_refund(args, config):
    """退款查询"""
    params = {
        "p0_Version": getattr(args, "version", "2.3"),
        "p1_MerchantNo": args.merchant_no or config["default_merchant_no"],
        "p2_RefundOrderNo": args.refund_order_no,
    }
    return send_request(config["base_url"], API_PATHS["query_refund"], params, config["merchant_key"])


def cmd_query_refund_info(args, config):
    """退款信息查询（根据支付订单号查退款情况）"""
    params = {
        "p1_MerchantNo": args.merchant_no or config["default_merchant_no"],
        "p2_OrderNo": args.order_no,
    }
    return send_request(config["base_url"], API_PATHS["query_refund_info"], params, config["merchant_key"])


def cmd_close(args, config):
    """关闭订单"""
    params = {
        "p1_MerchantNo": args.merchant_no or config["default_merchant_no"],
        "p2_OrderNo": args.order_no,
        "p3_FrpCode": args.frp_code,
    }
    if hasattr(args, "trx_no") and args.trx_no:
        params["p4_TrxNo"] = args.trx_no
    return send_request(config["base_url"], API_PATHS["close"], params, config["merchant_key"])


def cmd_query_funds(args, config):
    """资金查询（发货管理订单查询）"""
    params = {
        "p0_Version": getattr(args, "version", CURRENT_VERSION),
        "p1_MerchantNo": args.merchant_no or config["default_merchant_no"],
        "p2_OrderNo": args.order_no,
    }
    return send_request(config["base_url"], API_PATHS["query_funds"], params, config["merchant_key"])


# ==================== CLI 入口 ====================

def main():
    parser = argparse.ArgumentParser(description="汇聚支付聚合支付 API CLI 客户端")
    parser.add_argument("--config", default=CONFIG_PATH, help="配置文件路径")
    subparsers = parser.add_subparsers(dest="command", help="接口命令")

    # --- pay ---
    pay_parser = subparsers.add_parser("pay", help="统一支付请求")
    pay_parser.add_argument("--merchant-no", help="商户编号（默认用配置文件）")
    pay_parser.add_argument("--order-no", required=True, help="商户订单号")
    pay_parser.add_argument("--amount", required=True, help="订单金额（元，如0.01）")
    pay_parser.add_argument("--product-name", required=True, help="商品名称")
    pay_parser.add_argument("--frp-code", required=True, help="交易类型（如WEIXIN_GZH）")
    pay_parser.add_argument("--notify-url", help="异步通知地址")
    pay_parser.add_argument("--trade-merchant-no", help="报备商户号")
    pay_parser.add_argument("--version", default=CURRENT_VERSION, help="接口版本号")
    pay_parser.add_argument("--cur", default="1", help="币种，默认1-人民币")
    pay_parser.add_argument("--product-desc", help="商品描述")
    pay_parser.add_argument("--mp", help="公用回传参数")
    pay_parser.add_argument("--return-url", help="页面跳转地址")
    pay_parser.add_argument("--sub-merchant-no", help="子商户编号")
    pay_parser.add_argument("--is-show-pic", help="是否展示二维码(填1)")
    pay_parser.add_argument("--open-id", help="微信Openid（公众号/小程序必填）")
    pay_parser.add_argument("--auth-code", help="付款码（被扫必填）")
    pay_parser.add_argument("--app-id", help="APPID")
    pay_parser.add_argument("--terminal-no", help="终端设备号（被扫必填）")
    pay_parser.add_argument("--transaction-model", help="支付宝H5模式(MODEL1/MODEL2/MODEL3)")
    pay_parser.add_argument("--buyer-id", help="支付宝买家用户号")
    pay_parser.add_argument("--terminal-ip", help="终端IP（被扫必填）")
    pay_parser.add_argument("--contract-id", help="签约ID（云微小程序）")
    pay_parser.add_argument("--special-info", help="特殊支付参数JSON")
    pay_parser.add_argument("--extra", help="额外参数JSON，如'{\"q5_OpenId\":\"xxx\"}'")

    # --- query ---
    query_parser = subparsers.add_parser("query", help="订单查询")
    query_parser.add_argument("--merchant-no", help="商户编号")
    query_parser.add_argument("--order-no", required=True, help="商户订单号")
    query_parser.add_argument("--version", default=CURRENT_VERSION, help="版本号")

    # --- refund ---
    refund_parser = subparsers.add_parser("refund", help="退款申请")
    refund_parser.add_argument("--merchant-no", help="商户编号")
    refund_parser.add_argument("--order-no", required=True, help="原支付订单号")
    refund_parser.add_argument("--refund-order-no", required=True, help="退款订单号")
    refund_parser.add_argument("--refund-amount", required=True, help="退款金额")
    refund_parser.add_argument("--reason", required=True, help="退款原因")
    refund_parser.add_argument("--notify-url", help="退款通知地址")
    refund_parser.add_argument("--alt-ref-info", help="分账退款信息")
    refund_parser.add_argument("--funds-account", help="退款资金账户")
    refund_parser.add_argument("--version", default="2.3", help="退款接口版本号")

    # --- query_refund ---
    qr_parser = subparsers.add_parser("query_refund", help="退款查询")
    qr_parser.add_argument("--merchant-no", help="商户编号")
    qr_parser.add_argument("--refund-order-no", required=True, help="退款订单号")
    qr_parser.add_argument("--version", default="2.3", help="版本号")

    # --- query_refund_info ---
    qri_parser = subparsers.add_parser("query_refund_info", help="退款信息查询（按支付订单号）")
    qri_parser.add_argument("--merchant-no", help="商户编号")
    qri_parser.add_argument("--order-no", required=True, help="原支付订单号")

    # --- close ---
    close_parser = subparsers.add_parser("close", help="关闭订单")
    close_parser.add_argument("--merchant-no", help="商户编号")
    close_parser.add_argument("--order-no", required=True, help="商户订单号")
    close_parser.add_argument("--frp-code", required=True, help="交易类型")
    close_parser.add_argument("--trx-no", help="交易流水号")

    # --- query_funds ---
    qf_parser = subparsers.add_parser("query_funds", help="资金查询（发货管理）")
    qf_parser.add_argument("--merchant-no", help="商户编号")
    qf_parser.add_argument("--order-no", required=True, help="商户订单号")
    qf_parser.add_argument("--version", default=CURRENT_VERSION, help="版本号")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    # 加载配置
    config = load_config()

    # 检查密钥是否已配置
    if not config.get("merchant_key") or config["merchant_key"].startswith("x"):
        print(json.dumps({
            "error": "商户密钥未配置",
            "hint": f"请编辑 {CONFIG_PATH}，将 merchant_key 替换为你的真实密钥",
        }, ensure_ascii=False, indent=2))
        sys.exit(1)

    # 分发命令
    cmd_map = {
        "pay": cmd_pay,
        "query": cmd_query,
        "refund": cmd_refund,
        "query_refund": cmd_query_refund,
        "query_refund_info": cmd_query_refund_info,
        "close": cmd_close,
        "query_funds": cmd_query_funds,
    }

    result, error = cmd_map[args.command](args, config)
    if error:
        print(json.dumps({"error": error}, ensure_ascii=False, indent=2))
        sys.exit(1)
    else:
        print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
