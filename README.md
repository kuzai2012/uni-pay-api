# uni-pay-api Skill

汇聚支付聚合支付API的智能接入助手，让一人公司也能快速接入微信、支付宝、银联三大支付渠道。

## 快速安装

```bash
npx skills add kuzai2012/uni-pay-api --yes
```

## 双模式运行

本 Skill 支持**双模式运行**，根据你的意图自动切换：

| 模式 | 触发条件 | 特点 |
|------|----------|------|
| **模式A：快速接入** | "帮我接入"、"集成支付"、"添加支付功能" | AI 直接写入代码到项目 |
| **模式B：参考咨询** | "查一下"、"给我看示例"、"签名怎么算" | 只展示不写入，按需提供信息 |

## 3步跑通（模式A）

### 步骤1：配置商户信息（最小3个字段）

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
帮我在Spring Boot项目里接入微信扫码支付，用MD5签名
```

### 步骤3：AI自动完成

AI 会自动：
1. 扫描项目结构，识别框架
2. 生成 SDK + Controller + Service + Config 代码
3. 写入项目对应目录
4. 输出接入报告

**写入文件**：
```
src/main/java/{groupId}/joinpay/
├── sdk/
│   ├── JoinPayClient.java
│   ├── JoinPaySignature.java
│   └── JoinPayRsaSignature.java
├── JoinPayConfig.java
├── JoinPayService.java
├── PayController.java
└── NotifyController.java
```

## Spring Boot API 接口

### 接口清单

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/pay/create` | POST | 扫码支付下单 |
| `/api/pay/wxjsapi` | POST | 微信公众号/小程序支付 |
| `/api/pay/card` | POST | 付款码支付 |
| `/api/pay/query` | GET | 订单查询 |
| `/api/pay/refund` | POST | 申请退款 |
| `/api/pay/refund/query` | GET | 退款查询 |
| `/api/pay/close` | POST | 关闭订单 |

### 统一响应格式

所有接口统一返回原始响应，**商户自行提取所需字段**：

```json
{
  "success": true,
  "data": {
    "ra_Code": 100,
    "r2_OrderNo": "TEST001",
    "r3_Amount": "0.01",
    "r7_TrxNo": "201026041410217406",
    "rc_Result": "https://trade.joinpay.cc/tradeRt/...",
    "hmac": "..."
  }
}
```

### 请求示例

```bash
# 扫码支付下单
curl -X POST http://localhost:8080/api/pay/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "TEST001",
    "amount": "0.01",
    "productName": "测试商品",
    "frpCode": "WEIXIN_NATIVE"
  }'

# 订单查询
curl "http://localhost:8080/api/pay/query?orderNo=TEST001"

# 申请退款
curl -X POST http://localhost:8080/api/pay/refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "TEST001",
    "refundOrderNo": "REF001",
    "refundAmount": "0.01",
    "reason": "用户申请退款"
  }'
```

## 支持的支付方式

| 渠道 | 支持的支付方式 |
|------|---------------|
| **微信** | 扫码(NATIVE)、被扫(CARD)、APP、H5、公众号(GZH)、小程序(XCX) |
| **支付宝** | 扫码(NATIVE)、被扫(CARD)、APP、H5、服务窗(FWC) |
| **银联** | 扫码(NATIVE)、被扫(CARD)、APP、H5 |

## 对话示例

### 快速接入场景（模式A）

```
用户：帮我在Spring Boot项目里接入汇聚支付
AI：好的，请提供以下信息：
    1. 签名方式：MD5 还是 RSA？
    2. 商户号是多少？
    3. 密钥是什么？
    4. 回调地址填什么？

用户：MD5签名，商户号888100500008456，密钥是xxx，回调 http://xxx/notify

AI：[扫描项目 pom.xml，提取 groupId=com.mystore]
    [加载模板，替换占位符，写入文件]

    ✅ 已写入以下文件：
    - src/main/java/com/mystore/joinpay/sdk/JoinPayClient.java
    - src/main/java/com/mystore/joinpay/sdk/JoinPaySignature.java
    - src/main/java/com/mystore/joinpay/sdk/JoinPayRsaSignature.java
    - src/main/java/com/mystore/joinpay/JoinPayConfig.java
    - src/main/java/com/mystore/joinpay/JoinPayService.java
    - src/main/java/com/mystore/joinpay/PayController.java
    - src/main/java/com/mystore/joinpay/NotifyController.java

    请完成以下步骤：
    1. 在 pom.xml 添加 fastjson 和 commons-codec 依赖
    2. 在 application.yml 中配置商户号和密钥
    3. 启动项目测试 POST /api/pay/create
```

### 参考咨询场景（模式B）

```
用户：查一下签名怎么算
AI：[加载签名规则文档并展示]

用户：给我看一下Java的下单示例
AI：[加载并展示代码示例]
```

## 功能总览

| 能力 | 模式 | 说明 |
|------|------|------|
| **快速接入** | A | 一键生成框架级代码并写入项目 |
| **产品选型** | B | 根据场景推荐支付方式 |
| **示例代码** | B | Java/Python/Go/PHP 四语言示例 |
| **业务速查** | B | 参数、签名、状态流转、回调处理 |
| **质量评估** | A+B | 签名验签、业务逻辑完整性检查 |
| **问题排查** | A+B | 响应码速查、常见问题FAQ |

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
- [快速接入模板使用指南](./references/0-快速接入模板/README.md)

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

### Q: 响应字段 rc_Result 是什么？

`rc_Result` 是下单成功后返回的核心字段：
- **扫码支付**：二维码URL，生成二维码让用户扫描
- **公众号/小程序**：调起支付的JSAPI参数
- **H5支付**：跳转页面URL或HTML

### Q: 如何提取需要的响应字段？

Spring Boot 模板返回完整原始响应，从 `data` 中提取：

```javascript
// 前端示例
const response = await fetch('/api/pay/create', {...});
const result = await response.json();

if (result.success) {
  const qrCodeUrl = result.data.rc_Result;      // 二维码URL
  const trxNo = result.data.r7_TrxNo;           // 平台流水号
  const amount = result.data.r3_Amount;         // 订单金额
}
```

## License

MIT
