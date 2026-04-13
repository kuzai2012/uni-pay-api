# -*- coding: utf-8 -*-
"""
汇聚支付 RSA 签名工具

使用 cryptography 库实现 RSA SHA256WithRSA 签名。
如未安装: pip install cryptography
"""

try:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import padding
    CRYPTO_AVAILABLE = True
except ImportError:
    CRYPTO_AVAILABLE = False


def sign(params: dict, private_key_pem: str) -> str:
    """使用RSA私钥生成签名

    Args:
        params: 请求参数字典（不包含hmac）
        private_key_pem: PKCS8格式私钥（PEM格式字符串或Base64内容）

    Returns:
        Base64编码的签名字符串
    """
    if not CRYPTO_AVAILABLE:
        raise ImportError(
            "需要安装cryptography库: pip install cryptography，"
            "或使用内置rsa库: pip install rsa"
        )

    import base64

    sign_str = _build_sign_string(params)

    # 加载私钥
    private_key = serialization.load_pem_private_key(
        private_key_pem.encode("utf-8") if "-----BEGIN" in private_key_pem
        else base64.b64decode(private_key_pem),
        password=None,
    )

    # 签名 (SHA256WithRSA / PKCS1v15)
    signature = private_key.sign(
        sign_str.encode("utf-8"),
        padding.PKCS1v15(),
        hashes.SHA256(),
    )

    return base64.b64encode(signature).decode("utf-8")


def verify(params: dict, public_key_pem: str, hmac_value: str) -> bool:
    """使用RSA公钥验证签名

    Args:
        params: 包含hmac的完整参数字典
        public_key_pem: 公钥（PEM格式或Base64）
        hmac_value: 收到的签名值

    Returns:
        True=验签通过
    """
    if not CRYPTO_AVAILABLE:
        raise ImportError("需要安装cryptography库: pip install cryptography")

    import base64

    sign_str = _build_sign_string(params)

    # 处理空格替换（兼容SignBiz.java:128）
    hmac_value = hmac_value.replace(" ", "+")

    # 加载公钥
    is_x509 = len(public_key_pem) > 1024
    try:
        if is_x509 or "-----BEGIN" in public_key_pem:
            public_key = serialization.load_pem_public_key(public_key_pem.encode("utf-8"))
        else:
            public_key = serialization.load_der_public_key(base64.b64decode(public_key_pem))
    except Exception:
        public_key = serialization.load_pem_public_key(public_key_pem.encode("utf-8"))

    # 验签
    try:
        public_key.verify(
            base64.b64decode(hmac_value),
            sign_str.encode("utf-8"),
            padding.PKCS1v15(),
            hashes.SHA256(),
        )
        return True
    except Exception:
        return False


def _build_sign_string(params: dict) -> str:
    """构建待签名字符串（与MD5方式一致）"""
    filtered = {
        k: str(v).strip()
        for k, v in params.items()
        if k.lower() != "hmac" and v is not None and str(v).strip()
    }
    sorted_keys = sorted(filtered.keys())
    return "".join(filtered[k] for k in sorted_keys)
