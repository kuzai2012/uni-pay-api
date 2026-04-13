# -*- coding: utf-8 -*-
"""
汇聚支付 HTTP 客户端封装

支持 MD5 和 RSA 双签名模式。
复用 uni_pay_client.py 的核心逻辑，增加 RSA 支持和面向对象接口。
"""

import json
import urllib.parse
import urllib.request
import ssl
from typing import Dict, Optional

from .joinpay_signature import sign as md5_sign
from .joinpay_signature import verify as md5_verify
from .joinpay_rsa_signature import sign as rsa_sign
from .joinpay_rsa_signature import verify as rsa_verify


class JoinPayClient:
    """汇聚支付API客户端"""

    API_PATHS = {
        "unified_pay": "/tradeRt/uniPay",
        "query_order": "/tradeRt/queryOrder",
        "refund": "/tradeRt/refund",
        "query_refund": "/tradeRt/queryRefund",
        "query_refund_info": "/tradeRt/queryRefundInfo",
        "close_order": "/tradeRt/closeOrder",
        "query_funds": "/tradeRt/queryFundsControlOrder",
    }

    def __init__(
        self,
        base_url: str,
        merchant_no: str,
        merchant_key: str = "",
        rsa_private_key: str = "",
        sign_type: str = "MD5",
        version: str = "2.6",
    ):
        self.base_url = base_url.rstrip("/")
        self.merchant_no = merchant_no
        self.merchant_key = merchant_key
        self.rsa_private_key = rsa_private_key
        self.sign_type = sign_type.upper()
        self.version = version

    def unified_pay(self, **kwargs) -> dict:
        """统一支付下单"""
        params = self._build_unified_params(**kwargs)
        return self._post(self.API_PATHS["unified_pay"], params)

    def query_order(self, order_no: str) -> dict:
        """订单查询"""
        return self._post(self.API_PATHS["query_order"], {
            "p0_Version": self.version,
            "p1_MerchantNo": self.merchant_no,
            "p2_OrderNo": order_no,
        })

    def refund(self, order_no: str, refund_order_no: str, refund_amount: str,
               reason: str, notify_url: str = "") -> dict:
        """退款申请"""
        params = {
            "p0_Version": "2.3",
            "p1_MerchantNo": self.merchant_no,
            "p2_OrderNo": order_no,
            "p3_RefundOrderNo": refund_order_no,
            "p4_RefundAmount": refund_amount,
            "p5_RefundReason": reason,
        }
        if notify_url:
            params["p6_NotifyUrl"] = notify_url
        return self._post(self.API_PATHS["refund"], params)

    def query_refund(self, refund_order_no: str) -> dict:
        """退款查询"""
        return self._post(self.API_PATHS["query_refund"], {
            "p0_Version": "2.3",
            "p1_MerchantNo": self.merchant_no,
            "p2_RefundOrderNo": refund_order_no,
        })

    def query_refund_info(self, order_no: str) -> dict:
        """退款信息查询（按支付订单号查全部退款记录）"""
        return self._post(self.API_PATHS["query_refund_info"], {
            "p1_MerchantNo": self.merchant_no,
            "p2_OrderNo": order_no,
        })

    def close_order(self, order_no: str, frp_code: str, trx_no: str = "") -> dict:
        """关闭订单"""
        params = {
            "p1_MerchantNo": self.merchant_no,
            "p2_OrderNo": order_no,
            "p3_FrpCode": frp_code,
        }
        if trx_no:
            params["p4_TrxNo"] = trx_no
        return self._post(self.API_PATHS["close_order"], params)

    def query_funds(self, order_no: str) -> dict:
        """资金查询"""
        return self._post(self.API_PATHS["query_funds"], {
            "p0_Version": self.version,
            "p1_MerchantNo": self.merchant_no,
            "p2_OrderNo": order_no,
        })

    # ==================== 内部方法 ====================

    def _build_unified_params(self, **kwargs) -> Dict[str, str]:
        """构建统一支付下单参数"""
        p = {
            "p0_Version": kwargs.get("version", self.version),
            "p1_MerchantNo": self.merchant_no,
            "p2_OrderNo": kwargs.get("order_no", ""),
            "p3_Amount": kwargs.get("amount", ""),
            "p4_Cur": kwargs.get("cur", "1"),
            "p5_ProductName": kwargs.get("product_name", ""),
            "p9_NotifyUrl": kwargs.get("notify_url", ""),
            "q1_FrpCode": kwargs.get("frp_code", ""),
            "qa_TradeMerchantNo": kwargs.get("trade_merchant_no", ""),
        }
        # 可选参数
        optional_map = {
            "product_desc": "p6_ProductDesc", "mp": "p7_Mp", "return_url": "p8_ReturnUrl",
            "sub_merchant_no": "q3_SubMerchantNo", "is_show_pic": "q4_IsShowPic",
            "open_id": "q5_OpenId", "auth_code": "q6_AuthCode", "app_id": "q7_AppId",
            "terminal_no": "q8_TerminalNo", "transaction_model": "q9_TransactionModel",
            "buyer_id": "qb_buyerId", "terminal_ip": "ql_TerminalIp",
            "contract_id": "qm_ContractId", "special_info": "qn_SpecialInfo",
        }
        for kwarg_key, param_name in optional_map.items():
            value = kwargs.get(kwarg_key)
            if value is not None and value != "":
                p[param_name] = value
        return {k: v for k, v in p.items() if v != ""}

    def _sign_params(self, params: dict) -> str:
        """根据配置的签名方式生成签名"""
        if self.sign_type == "RSA":
            return rsa_sign(params, self.rsa_private_key)
        return md5_sign(params, self.merchant_key)

    def _post(self, api_path: str, params: dict) -> dict:
        """发送POST请求并返回JSON结果"""
        hmac_val = self._sign_params(params)
        params_with_sig = dict(params)
        params_with_sig["hmac"] = hmac_val

        encoded_data = urllib.parse.urlencode(params_with_sig).encode("utf-8")
        url = self.base_url + api_path

        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE

        req = urllib.request.Request(url, data=encoded_data, method="POST")
        req.add_header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")

        try:
            with urllib.request.urlopen(req, timeout=30, context=ctx) as resp:
                body = resp.read().decode("utf-8")
                return json.loads(body)
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="replace") if e.fp else ""
            return {"ra_Code": str(e.code), "rb_CodeMsg": f"HTTP Error: {body[:200]}"}
        except urllib.error.URLError as e:
            return {"ra_Code": "-1", "rb_CodeMsg": f"网络错误: {e.reason}"}
        except Exception as e:
            return {"ra_Code": "-1", "rb_CodeMsg": f"请求异常: {str(e)}"}
