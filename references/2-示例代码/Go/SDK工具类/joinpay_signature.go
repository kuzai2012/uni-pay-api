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
	"sort"
	"strings"
	"time"
)

// JoinPaySignature 汇聚支付 MD5 签名工具
//
// 签名算法（与 SignBiz.java 一致）：
// 1. 排除 hmac 参数
// 2. 按 key 字母升序排序
// 3. 拼接所有 value（不含 key）
// 4. MD5(拼接值 + merchantKey).toUpperCase()
type JoinPaySignature struct{}

// Sign 生成MD5签名（与 SignBiz.java:80-99 一致，空值不参与签名）
func (s *JoinPaySignature) Sign(params map[string]string, merchantKey string) string {
	var keys []string
	for k := range params {
		if strings.ToLower(k) == "hmac" {
			continue
		}
		keys = append(keys, k)
	}
	sort.Strings(keys)

	var buf bytes.Buffer
	for _, k := range keys {
		if v := strings.TrimSpace(params[k]); v != "" {
			buf.WriteString(v)
		}
	}

	hash := md5.Sum([]byte(buf.String() + merchantKey))
	return strings.ToUpper(hex.EncodeToString(hash[:]))
}

// Verify 验证MD5签名
func (s *JoinPaySignature) Verify(params map[string]string, merchantKey string) bool {
	receivedHmac := strings.ToUpper(params["hmac"])
	calculatedHmac := s.Sign(params, merchantKey)
	return calculatedHmac == receivedHmac
}
