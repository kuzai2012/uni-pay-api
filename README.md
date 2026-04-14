# uni-pay-api Skill

汇聚支付聚合支付API的智能接入助手，让一人公司也能快速接入微信、支付宝、银联三大支付渠道。

## 快速安装

```bash
npx skills add kuzai2012/uni-pay-api --yes
```

## 3步跑通

### 步骤1：配置商户信息

编辑 `~/.codebuddy/skills/uni-pay-api/config.json`：

```json
{
  "default_merchant_no": "你的商户号",
  "merchant_key": "你的MD5密钥(32位)",
  "base_url": "https://trade.joinpay.cc"
}
```

> 测试环境用 `trade.joinpay.cc`，生产环境用 `trade.joinpay.com`

### 步骤2：告诉AI你的需求

```
帮我在Spring Boot项目里接入微信扫码支付
```

### 步骤3：AI自动完成

AI会自动：
1. 扫描你的项目结构
2. 生成 Controller + Service + Config 代码
3. 写入项目对应目录
4. 输出接入报告

## 支持的支付方式

| 渠道 | 支持的支付方式 |
|------|---------------|
| **微信** | 扫码(NATIVE)、被扫(CARD)、APP、H5、公众号(GZH)、小程序(XCX) |
| **支付宝** | 扫码(NATIVE)、被扫(CARD)、APP、H5、服务窗(FWC) |
| **银联** | 扫码(NATIVE)、被扫(CARD)、APP、H5 |

## 对话示例

### 快速接入场景

```
用户：帮我接入汇聚支付
AI：好的，我来帮你快速接入。请先告诉我：
    1. 你的项目是用什么框架？（Spring Boot / Django / Express）
    2. 签名方式：MD5 还是 RSA？
    3. 商户号是多少？
    4. 密钥是什么？
    5. 回调地址填什么？

用户：Spring Boot，MD5签名，商户号888100500008456，密钥是xxx，回调地址 http://xxx/notify

AI：[自动生成并写入代码]
    ✅ 已写入以下文件：
    - src/main/java/com/example/joinpay/JoinPayConfig.java
    - src/main/java/com/example/joinpay/JoinPayService.java
    - src/main/java/com/example/joinpay/PayController.java
    - src/main/java/com/example/joinpay/NotifyController.java
```

### 参考咨询场景

```
用户：查一下签名怎么算
AI：[加载签名规则文档并展示]

用户：给我看一下Java的下单示例
AI：[加载并展示代码示例]
```

## 功能总览

| 能力 | 说明 |
|------|------|
| **快速接入** | 一键生成框架级代码并写入项目 |
| **产品选型** | 根据场景推荐支付方式 |
| **示例代码** | Java/Python/Go/PHP 四语言示例 |
| **业务速查** | 参数、签名、状态流转、回调处理 |
| **质量评估** | 签名验签、业务逻辑完整性检查 |
| **问题排查** | 响应码速查、常见问题FAQ |

## 签名方式

支持双签名方式：
- **MD5**：简单易用，适合快速接入
- **RSA**：安全性更高，适合对安全要求较高的场景

## 文档

- [支付渠道与交易类型对比](./references/1-产品选型/支付渠道与交易类型对比.md)
- [签名与验签规则](./references/3-接入指南/签名与验签规则.md)
- [回调通知处理](./references/3-接入指南/回调通知处理.md)
- [接入质量检查清单](./references/3-接入指南/接入质量检查清单.md)
- [排障手册](./references/4-问题排查/排障手册.md)

## 常见问题

### Q: 如何测试下单？

```bash
python3 ~/.codebuddy/skills/uni-pay-api/scripts/uni_pay_client.py pay \
  --order-no TEST001 \
  --amount 0.01 \
  --product-name 测试商品 \
  --frp-code WEIXIN_NATIVE
```

### Q: 回调地址怎么配置？

回调地址需要外网可访问，可以使用 ngrok 或云服务器暴露本地端口。

### Q: MD5和RSA签名怎么选？

- **MD5**：快速接入，配置简单，适合内部系统
- **RSA**：安全性高，适合对外服务，需要生成密钥对

## License

MIT
