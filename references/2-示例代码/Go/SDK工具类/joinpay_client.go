package joinpaysdk

import (
	"bytes"
	"crypto/md5"
	"crypto/tls"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"regexp"
	"sort"
	"strings"
	"time"
)

// JoinPayClient 汇聚支付 HTTP 客户端
type JoinPayClient struct {
	BaseURL      string
	MerchantNo   string
	MerchantKey  string
	RSAPrivateKey string
	SignType     string // "MD5" or "RSA"
	Version      string
}

// NewJoinPayClient 创建客户端实例
func NewJoinPayClient(baseURL, merchantNo, merchantKey, rsaPrivateKey, signType string) *JoinPayClient {
	if signType == "" {
		signType = "MD5"
	}
	return &JoinPayClient{
		BaseURL:       strings.TrimRight(baseURL, "/"),
		MerchantNo:    merchantNo,
		MerchantKey:   merchantKey,
		RSAPrivateKey: rsaPrivateKey,
		SignType:      strings.ToUpper(signType),
		Version:       "2.6",
	}
}

// UnifiedPayRequest 统一支付下单请求
type UnifiedPayRequest struct {
	OrderNo         string
	Amount          string
	ProductName     string
	FrpCode         string
	NotifyURL       string
	TradeMerchantNo string
	Cur             string
	ProductDesc     string
	Mp              string
	ReturnURL       string
	SubMerchantNo   string
	IsShowPic       string
	OpenId          string
	AuthCode        string
	AppId           string
	TerminalNo      string
	TransactionModel string
	BuyerId         string
	TerminalIp      string
	ContractId      string
	SpecialInfo     string
}

// APIResponse 统一响应结构
type APIResponse map[string]interface{}

// String 获取字符串值
func (r APIResponse) String(key string) string {
	if v, ok := r[key]; ok && v != nil {
		return fmt.Sprintf("%v", v)
	}
	return ""
}

// Int 获取整数值
func (r APIResponse) Int(key string) int {
	if v, ok := r[key].(float64); ok {
		return int(v)
	}
	return 0
}

// ==================== 公开API方法 ====================

// UnifiedPay 统一支付下单
func (c *JoinPayClient) UnifiedPay(req UnifiedPayRequest) (APIResponse, error) {
	// 校验回调地址（汇聚服务端会拒绝 localhost/127.0.0.1 等内网地址）
	if err := validateNotifyUrl(req.NotifyURL); err != nil {
		return APIResponse{"ra_Code": "-1", "rb_CodeMsg": err.Error()}, err
	}

	params := map[string]string{
		"p0_Version":        c.Version,
		"p1_MerchantNo":     c.MerchantNo,
		"p2_OrderNo":        req.OrderNo,
		"p3_Amount":         req.Amount,
		"p4_Cur":            defaultIfEmpty(req.Cur, "1"),
		"p5_ProductName":    req.ProductName,
		"p9_NotifyUrl":      req.NotifyURL,
		"q1_FrpCode":        req.FrpCode,
		"qa_TradeMerchantNo": defaultIfEmpty(req.TradeMerchantNo, ""),
	}
	setIfNotEmpty(params, "p6_ProductDesc", req.ProductDesc)
	setIfNotEmpty(params, "p7_Mp", req.Mp)
	setIfNotEmpty(params, "p8_ReturnUrl", req.ReturnURL)
	setIfNotEmpty(params, "q3_SubMerchantNo", req.SubMerchantNo)
	setIfNotEmpty(params, "q4_IsShowPic", req.IsShowPic)
	setIfNotEmpty(params, "q5_OpenId", req.OpenId)
	setIfNotEmpty(params, "q6_AuthCode", req.AuthCode)
	setIfNotEmpty(params, "q7_AppId", req.AppId)
	setIfNotEmpty(params, "q8_TerminalNo", req.TerminalNo)
	setIfNotEmpty(params, "q9_TransactionModel", req.TransactionModel)
	setIfNotEmpty(params, "qb_buyerId", req.BuyerId)
	setIfNotEmpty(params, "ql_TerminalIp", req.TerminalIp)
	setIfNotEmpty(params, "qm_ContractId", req.ContractId)
	setIfNotEmpty(params, "qn_SpecialInfo", req.SpecialInfo)

	return c.post("/tradeRt/uniPay", params)
}

// QueryOrder 订单查询
func (c *JoinPayClient) QueryOrder(orderNo string) (APIResponse, error) {
	params := map[string]string{
		"p0_Version":    c.Version,
		"p1_MerchantNo": c.MerchantNo,
		"p2_OrderNo":    orderNo,
	}
	return c.post("/tradeRt/queryOrder", params)
}

// Refund 退款申请
func (c *JoinPayClient) Refund(orderNo, refundOrderNo, refundAmount, reason, notifyURL string) (APIResponse, error) {
	params := map[string]string{
		"p0_Version":        "2.3",
		"p1_MerchantNo":     c.MerchantNo,
		"p2_OrderNo":        orderNo,
		"p3_RefundOrderNo":  refundOrderNo,
		"p4_RefundAmount":   refundAmount,
		"p5_RefundReason":   reason,
	}
	setIfNotEmpty(params, "p6_NotifyUrl", notifyURL)
	return c.post("/tradeRt/refund", params)
}

// QueryRefund 退款查询
func (c *JoinPayClient) QueryRefund(refundOrderNo string) (APIResponse, error) {
	params := map[string]string{
		"p0_Version":    "2.3",
		"p1_MerchantNo": c.MerchantNo,
		"p2_RefundOrderNo": refundOrderNo,
	}
	return c.post("/tradeRt/queryRefund", params)
}

// QueryRefundInfo 退款信息查询
func (c *JoinPayClient) QueryRefundInfo(orderNo string) (APIResponse, error) {
	params := map[string]string{
		"p1_MerchantNo": c.MerchantNo,
		"p2_OrderNo":    orderNo,
	}
	return c.post("/tradeRt/queryRefundInfo", params)
}

// CloseOrder 关闭订单
func (c *JoinPayClient) CloseOrder(orderNo, frpCode, trxNo string) (APIResponse, error) {
	params := map[string]string{
		"p0_Version":    "1.0", // 关单接口独立版本线，当前 ≥ 1.0
		"p1_MerchantNo": c.MerchantNo,
		"p2_OrderNo":    orderNo,
		"p3_FrpCode":    frpCode,
	}
	setIfNotEmpty(params, "p4_TrxNo", trxNo)
	return c.post("/tradeRt/closeOrder", params)
}

// QueryFunds 资金查询
func (c *JoinPayClient) QueryFunds(orderNo string) (APIResponse, error) {
	params := map[string]string{
		"p0_Version":    "1.0", // 资金管控查询独立版本线，当前 ≥ 1.0
		"p1_MerchantNo": c.MerchantNo,
		"p2_OrderNo":    orderNo,
	}
	return c.post("/tradeRt/queryFundsControlOrder", params)
}

// ==================== 内部实现 ====================

func (c *JoinPayClient) post(apiPath string, params map[string]string) (APIResponse, error) {
	// 签名
	var hmacVal string
	if c.SignType == "RSA" {
		rsaSigner := &JoinPayRsaSignature{}
		var err error
		hmacVal, err = rsaSigner.Sign(params, c.RSAPrivateKey)
		if err != nil {
			return APIResponse{"ra_Code": "-1", "rb_CodeMsg": "签名失败: " + err.Error()}, err
		}
	} else {
		signer := &JoinPaySignature{}
		hmacVal = signer.Sign(params, c.MerchantKey)
	}
	params["hmac"] = hmacVal

	// 构建请求体
	formData := url.Values{}
	for k, v := range params {
		formData.Set(k, v)
	}

	urlStr := c.BaseURL + apiPath

	// 创建HTTP Client（跳过SSL验证，测试环境用）
	httpClient := &http.Client{Timeout: 30 * time.Second}
	transport := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	httpClient.Transport = transport

	resp, err := httpClient.Post(urlStr, "application/x-www-form-urlencoded", strings.NewReader(formData.Encode()))
	if err != nil {
		return APIResponse{"ra_Code": "-1", "rb_CodeMsg": "网络错误: " + err.Error()}, err
	}
	defer resp.Body.Close()

	bodyBytes, _ := io.ReadAll(resp.Body)

	var result APIResponse
	if err := json.Unmarshal(bodyBytes, &result); err != nil {
		return APIResponse{
			"ra_Code":    "-1",
			"rb_CodeMsg": "非JSON响应: " + truncateString(string(bodyBytes), 200),
		}, nil
	}

	return result, nil
}

// ==================== 工具函数 ====================

// validateNotifyUrl 校验回调地址合法性
//
// 汇聚支付服务端会校验 p9_NotifyUrl，拒绝以下地址：
//   - localhost / 127.0.0.1（汇聚服务器无法访问你的本机）
//   - 10.x / 172.16-31.x / 192.168.x（内网地址）
func validateNotifyUrl(url string) error {
	if url == "" {
		return nil // 允许为空
	}
	lower := strings.ToLower(strings.TrimSpace(url))

	if strings.HasPrefix(lower, "http://localhost") || strings.HasPrefix(lower, "https://localhost") {
		return fmt.Errorf("回调地址不能使用 localhost！"+
			"本地开发请使用内网穿透工具(ngrok/cpolar/frp)，或使用公网地址。错误地址: %s", url)
	}

	// 127.0.0.x 回环地址
	if regexp.MustCompile(`.*://127\.0?\.?0?\.?[0-9]?.*`).MatchString(lower) || strings.Contains(lower, "127.1.") {
		return fmt.Errorf("回调地址不能使用 127.0.0.x 回环地址！错误地址: %s", url)
	}

	// 内网地址段
	if regexp.MustCompile(`^https?://(?:10\.|172\.(?:1[6-9]|2[0-9]|3[01])\.|192\.168\.)`).MatchString(lower) {
		return fmt.Errorf("回调地址不能使用内网IP！汇聚服务端无法从外网访问。错误地址: %s", url)
	}

	if !strings.HasPrefix(lower, "http://") && !strings.HasPrefix(lower, "https://") {
		return fmt.Errorf("回调地址必须以 http:// 或 https:// 开头。当前值: %s", url)
	}

	return nil
}

func setIfNotEmpty(m map[string]string, key, value string) {
	if value != "" {
		m[key] = value
	}
}

func defaultIfEmpty(value, fallback string) string {
	if value == "" {
		return fallback
	}
	return value
}

func truncateString(s string, maxLen int) string {
	runes := []rune(s)
	if len(runes) > maxLen {
		return string(runes[:maxLen])
	}
	return s
}
