<?php
/**
 * 汇聚支付 HTTP 客户端封装
 *
 * 支持 MD5 和 RSA 双签名模式。
 */
class JoinPayClient
{
    private $baseUrl;
    private $merchantNo;
    private $merchantKey;
    private $rsaPrivateKey;
    private $signType;
    private $version;

    /**
     * @param string $baseUrl      网关地址
     * @param string $merchantNo   商户号
     * @param string $merchantKey  MD5密钥
     * @param string $rsaPrivateKey RSA私钥(PEM格式)
     * @param string $signType     签名方式: MD5 或 RSA
     * @param string $version      版本号
     */
    public function __construct(
        string $baseUrl,
        string $merchantNo,
        string $merchantKey = '',
        string $rsaPrivateKey = '',
        string $signType = 'MD5',
        string $version = '2.6'
    ) {
        $this->baseUrl = rtrim($baseUrl, '/');
        $this->merchantNo = $merchantNo;
        $this->merchantKey = $merchantKey;
        $this->rsaPrivateKey = $rsaPrivateKey;
        $this->signType = strtoupper($signType);
        $this->version = $version;
    }

    // ==================== 公开API方法 ====================

    /**
     * 统一支付下单
     *
     * @param array $params 支付参数
     * @return array 响应结果
     */
    public function unifiedPay(array $params): array
    {
        $request = array_merge([
            'p0_Version' => $this->version,
            'p1_MerchantNo' => $this->merchantNo,
            'p9_NotifyUrl' => '',
            'qa_TradeMerchantNo' => '',
        ], $params);
        
        return $this->post('/tradeRt/uniPay', $request);
    }

    /**
     * 订单查询
     */
    public function queryOrder(string $orderNo): array
    {
        return $this->post('/tradeRt/queryOrder', [
            'p0_Version' => $this->version,
            'p1_MerchantNo' => $this->merchantNo,
            'p2_OrderNo' => $orderNo,
        ]);
    }

    /**
     * 退款申请
     */
    public function refund(string $orderNo, string $refundOrderNo, 
                          string $refundAmount, string $reason,
                          string $notifyUrl = ''): array
    {
        $params = [
            'p0_Version' => '2.3',
            'p1_MerchantNo' => $this->merchantNo,
            'p2_OrderNo' => $orderNo,
            'p3_RefundOrderNo' => $refundOrderNo,
            'p4_RefundAmount' => $refundAmount,
            'p5_RefundReason' => $reason,
        ];
        if ($notifyUrl !== '') {
            $params['p6_NotifyUrl'] = $notifyUrl;
        }
        return $this->post('/tradeRt/refund', $params);
    }

    /**
     * 退款查询
     */
    public function queryRefund(string $refundOrderNo): array
    {
        return $this->post('/tradeRt/queryRefund', [
            'p0_Version' => '2.3',
            'p1_MerchantNo' => $this->merchantNo,
            'p2_RefundOrderNo' => $refundOrderNo,
        ]);
    }

    /**
     * 退款信息查询（按支付订单号查全部退款记录）
     */
    public function queryRefundInfo(string $orderNo): array
    {
        return $this->post('/tradeRt/queryRefundInfo', [
            'p1_MerchantNo' => $this->merchantNo,
            'p2_OrderNo' => $orderNo,
        ]);
    }

    /**
     * 关闭订单
     */
    public function closeOrder(string $orderNo, string $frpCode,
                                string $trxNo = ''): array
    {
        $params = [
            'p0_Version' => '1.0',  // 关单接口独立版本线，当前 ≥ 1.0
            'p1_MerchantNo' => $this->merchantNo,
            'p2_OrderNo' => $orderNo,
            'p3_FrpCode' => $frpCode,
        ];
        if ($trxNo !== '') {
            $params['p4_TrxNo'] = $trxNo;
        }
        return $this->post('/tradeRt/closeOrder', $params);
    }

    /**
     * 资金查询
     */
    public function queryFunds(string $orderNo): array
    {
        return $this->post('/tradeRt/queryFundsControlOrder', [
            'p0_Version' => '1.0',  // 资金管控查询独立版本线，当前 ≥ 1.0
            'p1_MerchantNo' => $this->merchantNo,
            'p2_OrderNo' => $orderNo,
        ]);
    }

    // ==================== 内部实现 ====================

    /**
     * 执行POST请求
     */
    protected function post(string $apiPath, array $params): array
    {
        // 过滤空值并签名
        $params = $this->filterEmpty($params);

        // 校验回调地址（汇聚服务端会拒绝 localhost/127.0.0.1 等内网地址）
        $this->validateNotifyUrl($params['p9_NotifyUrl'] ?? '');
        
        // 生成签名
        if ($this->signType === 'RSA') {
            $hmac = \JoinPayRsaSignature::sign($params, $this->rsaPrivateKey);
        } else {
            $hmac = \JoinPaySignature::sign($params, $this->merchantKey);
        }
        $params['hmac'] = $hmac;

        // URL编码参数
        $encoded = http_build_query($params, '', '&');
        $url = $this->baseUrl . $apiPath;

        // 发送请求
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => $encoded,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_SSL_VERIFYPEER => false, // 测试环境跳过验证
            CURLOPT_HTTPHEADER => ['Content-Type: application/x-www-form-urlencoded; charset=utf-8'],
        ]);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

        if ($error || $response === false) {
            return ['ra_Code' => '-1', 'rb_CodeMsg' => 'CURL错误: ' . $error];
        }

        $result = json_decode($response, true);
        if (!is_array($result)) {
            return [
                'ra_Code' => '-1', 
                'rb_CodeMsg' => '非JSON响应: ' . mb_substr((string)$response, 0, 200),
            ];
        }

        return $result;
    }

    /**
     * 过滤空值
     */
    protected function filterEmpty(array $params): array
    {
        return array_filter($params, function ($v) {
            if ($v === null || $v === '') return false;
            if (is_string($v) && trim($v) === '') return false;
            return true;
        });
    }

    /**
     * 校验回调地址合法性
     *
     * 汇聚支付服务端会校验 p9_NotifyUrl，拒绝以下地址：
     * - localhost / 127.0.0.1（汇聚服务器无法访问你的本机）
     * - 10.x / 172.16-31.x / 192.168.x（内网地址）
     *
     * @param string $url 回调地址
     * @throws \InvalidArgumentException 地址不合法时抛出
     */
    protected function validateNotifyUrl(string $url): void
    {
        if ($url === '' || trim($url) === '') return;

        $lower = strtolower(trim($url));

        if (str_starts_with($lower, 'http://localhost') || str_starts_with($lower, 'https://localhost')) {
            throw new \InvalidArgumentException(
                '回调地址不能使用 localhost！本地开发请使用内网穿透工具(ngrok/cpolar/frp)，或使用测试服务器公网地址。错误地址: ' . $url
            );
        }

        if (preg_match('#.*://127\.0?\.?0?\.?[0-9]?.*#', $lower) || str_contains($lower, '127.1.')) {
            throw new \InvalidArgumentException('回调地址不能使用 127.0.0.x 回环地址！错误地址: ' . $url);
        }

        if (preg_match('#^https?://(?:10\.|172\.(?:1[6-9]|2[0-9]|3[01])\.|192\.168\.)#', $lower)) {
            throw new \InvalidArgumentException('回调地址不能使用内网IP！汇聚服务端无法从外网访问。错误地址: ' . $url);
        }

        if (!str_starts_with($lower, 'http://') && !str_starts_with($lower, 'https://')) {
            throw new \InvalidArgumentException('回调地址必须以 http:// 或 https:// 开头。当前值: ' . $url);
        }
    }
}
