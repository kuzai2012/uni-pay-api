package ${PACKAGE}.joinpay;

import ${PACKAGE}.joinpay.sdk.JoinPayClient;
import ${PACKAGE}.joinpay.sdk.JoinPaySignature;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 汇聚支付配置类
 *
 * 从 application.yml 读取配置，构建 JoinPayClient Bean
 */
@Configuration
@ConfigurationProperties(prefix = "joinpay")
public class JoinPayConfig {

    /** 汇聚支付网关地址 */
    private String baseUrl = "https://trade.joinpay.com";

    /** 商户号 */
    private String merchantNo;

    /** 商户密钥(MD5签名用) */
    private String merchantKey;

    /** RSA私钥(RSA签名用) */
    private String rsaPrivateKey;

    /** RSA公钥(验签用) */
    private String rsaPublicKey;

    /** 签名方式: MD5 或 RSA */
    private String signType = "MD5";

    /** 默认回调地址 */
    private String defaultNotifyUrl;

    // ==================== Getters & Setters ====================

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getMerchantNo() {
        return merchantNo;
    }

    public void setMerchantNo(String merchantNo) {
        this.merchantNo = merchantNo;
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public void setMerchantKey(String merchantKey) {
        this.merchantKey = merchantKey;
    }

    public String getRsaPrivateKey() {
        return rsaPrivateKey;
    }

    public void setRsaPrivateKey(String rsaPrivateKey) {
        this.rsaPrivateKey = rsaPrivateKey;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public void setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getDefaultNotifyUrl() {
        return defaultNotifyUrl;
    }

    public void setDefaultNotifyUrl(String defaultNotifyUrl) {
        this.defaultNotifyUrl = defaultNotifyUrl;
    }

    // ==================== Bean 定义 ====================

    @Bean
    public JoinPayClient joinPayClient() {
        return new JoinPayClient(
            baseUrl,
            merchantNo,
            merchantKey,
            rsaPrivateKey,
            signType
        );
    }
}
