<?php
/**
 * 汇聚支付 RSA 签名工具
 *
 * 使用 OpenSSL 扩展实现 RSA MD5withRSA 签名（与 RSAUtils.java:76 一致）。
 */
class JoinPayRsaSignature
{
    /**
     * 使用RSA私钥生成签名
     *
     * @param array $params 请求参数（不含hmac）
     * @param string $privateKey 私钥（PEM格式或Base64内容）
     * @return string Base64编码的签名字符串
     * @throws \RuntimeException
     */
    public static function sign(array $params, string $privateKey): string
    {
        $signStr = self::buildSignString($params);

        // 加载私钥
        $pem = self::loadPrivateKey($privateKey);

        // 签名 MD5withRSA（与 RSAUtils.java:76 SIGNATURE_ALGORITHM 一致）
        $result = openssl_sign($signStr, $signature, $pem, OPENSSL_ALGO_MD5);
        if (!$result) {
            throw new \RuntimeException('RSA签名失败');
        }

        return base64_encode($signature);
    }

    /**
     * 使用RSA公钥验证签名
     *
     * @param array $params 包含hmac的完整参数
     * @param string $publicKey 公钥
     * @param string $hmacValue 收到的签名值
     * @return bool 验签是否通过
     */
    public static function verify(array $params, string $publicKey, string $hmacValue): bool
    {
        $signStr = self::buildSignString($params);

        // 空格替换（兼容SignBiz.java:128）
        $hmacValue = str_replace(' ', '+', $hmacValue);

        // 加载公钥
        $pem = self::loadPublicKey($publicKey);

        $sigData = base64_decode($hmacValue, true);
        if ($sigData === false) {
            return false;
        }

        $result = openssl_verify($signStr, $sigData, $pem, OPENSSL_ALGO_MD5);
        return $result === 1;
    }

    /**
     * 构建待签名字符串（与 SignBiz.java:80-99 一致，空值不参与签名）
     */
    public static function buildSignString(array $params): string
    {
        $filtered = [];
        foreach ($params as $k => $v) {
            if (strtolower($k) === 'hmac') continue;
            $val = trim(strval($v ?? ''));
            if ($val !== '') {
                $filtered[$k] = $val;
            }
        }
        ksort($filtered);
        return implode('', $filtered);
    }

    /**
     * 加载私钥资源
     */
    private static function loadPrivateKey(string $keyContent)
    {
        if (strpos($keyContent, '-----BEGIN') !== false) {
            $pem = openssl_pkey_get_private($keyContent);
        } else {
            // Base64解码后构造PEM
            $decoded = base64_decode($keyContent);
            $pemContent = "-----BEGIN PRIVATE KEY-----\n" .
                         chunk_split($decoded, 64, "\n") .
                         "-----END PRIVATE KEY-----";
            $pem = openssl_pkey_get_private($pemContent);
        }
        
        if ($pem === false) {
            throw new \RuntimeException('无法加载RSA私钥');
        }
        return $pem;
    }

    /**
     * 加载公钥资源
     */
    private static function loadPublicKey(string $keyContent)
    {
        if (strpos($keyContent, '-----BEGIN') !== false) {
            $pem = openssl_pkey_get_public($keyContent);
        } else {
            $decoded = base64_decode($keyContent);
            $isX509 = strlen($keyContent) > 1024;
            
            if ($isX509 || strpos($decoded, '-----BEGIN') !== false) {
                $pem = openssl_pkey_get_public($decoded);
            } else {
                // 构造公钥PEM
                $pemContent = "-----BEGIN PUBLIC KEY-----\n" .
                             chunk_split($decoded, 64, "\n") .
                             "-----END PUBLIC KEY-----";
                $pem = openssl_pkey_get_public($pemContent);
            }
        }
        
        if ($pem === false) {
            throw new \RuntimeException('无法加载RSA公钥');
        }
        return $pem;
    }
}
