package joinpaysdk

import (
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/pem"
	"errors"
	"fmt"
	"sort"
	"strings"
)

// JoinPayRsaSignature 汇聚支付 RSA 签名工具
type JoinPayRsaSignature struct{}

// Sign 使用RSA私钥生成签名 (SHA256WithRSA)
func (r *JoinPayRsaSignature) Sign(params map[string]string, privateKeyPEM string) (string, error) {
	signStr := r.buildSignString(params)

	privKey, err := parsePrivateKey(privateKeyPEM)
	if err != nil {
		return "", fmt.Errorf("解析私钥失败: %v", err)
	}

	hash := sha256.Sum256([]byte(signStr))
	signature, err := rsa.SignPKCS1v15(rand.Reader, privKey, crypto.SHA256, hash[:])
	if err != nil {
		return "", err
	}

	return base64.StdEncoding.EncodeToString(signature), nil
}

// Verify 使用RSA公钥验证签名
func (r *JoinPayRsaSignature) Verify(params map[string]string, publicKeyPEM string, hmacValue string) (bool, error) {
	signStr := r.buildSignString(params)

	// 兼容 SignBiz.java:128 空格处理
	hmacValue = strings.Replace(hmacValue, " ", "+", -1)

	sigBytes, err := base64.StdEncoding.DecodeString(hmacValue)
	if err != nil {
		return false, err
	}

	// 尝试加载公钥
	pubKey, err := loadPublicKey(publicKeyPEM)
	if err != nil {
		return false, err
	}

	hash := sha256.Sum256([]byte(signStr))
	err = rsa.VerifyPKCS1v15(pubKey, crypto.SHA256, hash[:], sigBytes)
	if err != nil {
		return false, nil // 验签失败不报错，返回false
	}

	return true, nil
}

func (r *JoinPayRsaSignature) buildSignString(params map[string]string) string {
	var keys []string
	for k := range params {
		if strings.ToLower(k) == "hmac" {
			continue
		}
		if v := strings.TrimSpace(params[k]); v != "" {
			keys = append(keys, k)
		}
	}
	sort.Strings(keys)

	var buf strings.Builder
	for _, k := range keys {
		buf.WriteString(strings.TrimSpace(params[k]))
	}
	return buf.String()
}

func parsePrivateKey(pemStr string) (*rsa.PrivateKey, error) {
	if strings.Contains(pemStr, "-----BEGIN") {
		block, _ := pem.Decode([]byte(pemStr))
		if block == nil {
			return nil, errors.New("无法解析PEM格式的私钥")
		}
		key, err := x509.ParsePKCS8PrivateKey(block.Bytes)
		if err != nil {
			return nil, err
		}
		return key.(*rsa.PrivateKey), nil
	}
	// Base64 编码的原始密钥
	keyBytes, err := base64.StdEncoding.DecodeString(pemStr)
	if err != nil {
		return nil, err
	}
	key, err := x509.ParsePKCS8PrivateKey(keyBytes)
	if err != nil {
		return nil, err
	}
	return key.(*rsa.PrivateKey), nil
}

func loadPublicKey(pemStr string) (*rsa.PublicKey, error) {
	if strings.Contains(pemStr, "-----BEGIN") {
		block, _ := pem.Decode([]byte(pemStr))
		if block == nil {
			return nil, errors.New("无法解析PEM格式的公钥")
		}
		pubInterface, err := x509.ParsePKIXPublicKey(block.Bytes)
		if err != nil {
			return nil, err
		}
		return pubInterface.(*rsa.PublicKey), nil
	}
	// Base64 编码的原始密钥
	keyBytes, err := base64.StdEncoding.DecodeString(pemStr)
	if err != nil {
		return nil, err
	}
	pubInterface, err := x509.ParsePKIXPublicKey(keyBytes)
	if err != nil {
		return nil, err
	}
	return pubInterface.(*rsa.PublicKey), nil
}
