# -*- coding: utf-8 -*-
"""
汇聚支付 MD5 签名工具

签名算法（与 SignBiz.java 一致）：
1. 排除 hmac 参数
2. 按 key 字母升序排序
3. 拼接所有 value（不含 key）
4. MD5(拼接值 + merchantKey).toUpperCase()
"""

import hashlib


def sign(params: dict, merchant_key: str) -> str:
    """生成MD5签名

    Args:
        params: 请求参数字典（不包含hmac）
        merchant_key: 商户密钥

    Returns:
        签名值（大写）
    """
    # 排除hmac，按key排序
    filtered = {}
    for k, v in params.items():
        if k.lower() == "hmac":
            continue
        val = str(v).strip() if v is not None else ""
        if val != "":
            filtered[k] = val

    sorted_keys = sorted(filtered.keys())
    sign_str = "".join(filtered[k] for k in sorted_keys)
    return hashlib.md5((sign_str + merchant_key).encode("utf-8")).hexdigest().upper()


def verify(params: dict, merchant_key: str) -> bool:
    """验证MD5签名

    Args:
        params: 包含hmac的完整参数字典
        merchant_key: 商户密钥

    Returns:
        True=验签通过
    """
    received_hmac = str(params.get("hmac", "")).upper()
    calculated_hmac = sign(params, merchant_key)
    return calculated_hmac == received_hmac


def build_sign_string(params: dict) -> str:
    """构建待签名字符串（用于调试）"""
    filtered = {k: v for k, v in params.items() if k.lower() != "hmac" and v}
    sorted_keys = sorted(filtered.keys())
    return "".join(str(filtered[k]).strip() for k in sorted_keys)
