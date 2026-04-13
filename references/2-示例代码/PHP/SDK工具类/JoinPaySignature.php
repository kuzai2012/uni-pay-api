<?php
/**
 * 汇聚支付 MD5 签名工具
 *
 * 签名算法（与 SignBiz.java 一致）：
 * 1. 排除 hmac 参数
 * 2. 按 key 字母升序排序
 * 3. 拼接所有 value（不含 key）
 * 4. MD5(拼接值 + merchantKey).toUpperCase()
 */
class JoinPaySignature
{
    /**
     * 生成MD5签名（与 SignBiz.java:80-99 一致，空值不参与签名）
     */
    public static function sign(array $params, string $merchantKey): string
    {
        // 排除hmac，过滤空值
        $filtered = [];
        foreach ($params as $k => $v) {
            if (strtolower($k) === 'hmac') {
                continue;
            }
            $val = trim(strval($v ?? ''));
            if ($val !== '') {
                $filtered[$k] = $val;
            }
        }

        // 按key字母排序
        ksort($filtered);

        // 拼接value
        $signStr = implode('', $filtered);

        // MD5(拼接值 + 密钥).toUpperCase()
        return strtoupper(md5($signStr . $merchantKey));
    }

    /**
     * 验证MD5签名
     *
     * @param array $params 包含hmac的完整参数
     * @param string $merchantKey 商户密钥
     * @return bool 验签是否通过
     */
    public static function verify(array $params, string $merchantKey): bool
    {
        $receivedHmac = strtoupper(strval($params['hmac'] ?? ''));
        $calculatedHmac = self::sign($params, $merchantKey);
        return hash_equals($calculatedHmac, $receivedHmac);
    }

    /**
     * 构建待签名字符串（调试用）
     */
    public static function buildSignString(array $params): string
    {
        $filtered = array_filter($params, fn($k) => strtolower($k) !== 'hmac', ARRAY_FILTER_USE_KEY);
        $filtered = array_filter($filtered, fn($v) => trim(strval($v)) !== '');
        ksort($filtered);
        return implode('', $filtered);
    }
}
