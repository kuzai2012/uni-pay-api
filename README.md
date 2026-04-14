# uni-pay-api Skill

汇聚支付聚合支付API的智能接入助手，让一人公司也能快速接入微信、支付宝、银联三大支付渠道。

## 快速安装

```bash
npx skills add kuzai2012/uni-pay-api --yes
```

## 3步跑通

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
帮我在我的项目里接入微信扫码支付
```

### 步骤3：一次性填完参数 → AI自动完成

AI 会**首先**让你填写所有必要参数（签名方式、商户号、接口环境、回调地址、密钥），然后：

1. 扫描项目结构，识别框架
2. 加载对应模板并替换参数
3. 写入 SDK + Controller + Service + Config 代码
4. 输出接入报告 + 下一步指引

> **核心体验**：所有参数在开始时就一次收集完毕，不会写到一半突然问你要商户号或密钥。

---

## 双模式运行

本 Skill 支持**双模式运行**，根据你的意图自动切换：

| 模式 | 触发条件 | 特点 |
|------|----------|------|
| **模式A：快速接入** | "帮我接入"、"集成支付"、"添加支付功能" | AI 直接写入代码到项目，**第一步就收齐所有参数** |
| **模式B：参考咨询** | "查一下"、"给我看示例"、"签名怎么算" | 只展示不写入，按需提供信息 |

## 对话示例

### 快速接入场景（模式A）— 参数前置收集

```
用户：帮我在我的Spring Boot项目里接入汇聚支付

AI：好的！在开始之前，请先完成以下配置信息（一次性收集）：

     [弹出结构化表单]
     ▸ 签名方式：MD5 / RSA
     ▸ 商户号：_________
     ▸ 接口环境：测试环境 / 生产环境
     ▸ 回调地址：_________ （⚠️ 不可用 localhost）
     ▸ MD5密钥：_________

用户：（填写完毕提交）

AI：✅ 参数校验通过
    正在扫描项目... 检测到 Spring Boot (groupId=com.mystore)
    加载模板... 替换占位符... 写入文件...

    ✅ 接入完成！已写入以下8个文件：
    📄 src/main/java/com/mystore/joinpay/sdk/JoinPayClient.java
    📄 src/main/java/com/mystore/joinpay/sdk/JoinPaySignature.java
    📄 src/main/java/com/mystore/joinpay/sdk/JoinPayRsaSignature.java
    📄 src/main/java/com/mystore/joinpay/JoinPayConfig.java
    📄 src/main/java/com/mystore/joinpay/JoinPayService.java
    📄 src/main/java/com/mystore/joinpay/PayController.java
    📄 src/main/java/com/mystore/joinpay/NotifyController.java
    📄 src/main/resources/application.yml (追加配置)

    🔜 下一步：
    1. 在 pom.xml 添加 fastjson 和 commons-codec 依赖
    2. 启动项目测试 POST /api/pay/create
    3. 用真实设备完成一笔小额支付验证回调
```

### 更多快速接入示例

| 你说 | AI 做的事 |
|------|----------|
| "帮我在项目里接入支付宝扫码支付" | 收集参数 → 生成 Spring Boot 代码 → 写入项目 |
| "我要加一个退款接口" | 收集参数 → 补充 RefundController 和 Service |
| "把支付换成RSA签名" | 收集 RSA 密钥对 → 更新配置和签名逻辑 |

### 参考咨询场景（模式B）

```
用户：查一下签名怎么算
AI：[加载签名规则文档并展示]

用户：给我看一下Java的下单示例
AI：[确认签名方式和语言后，展示代码示例]

用户：ra_Code=999是什么错误？
AI：[加载排障手册，定位响应码并给出解决方案]
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
| `/notify/pay` | POST | 支付回调 |
| `/notify/refund` | POST | 退款回调 |

### 统一响应格式

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
  -d '{"orderNo":"TEST001","amount":"0.01","productName":"测试商品","frpCode":"WEIXIN_NATIVE"}'

# 订单查询
curl "http://localhost:8080/api/pay/query?orderNo=TEST001"

# 申请退款
curl -X POST http://localhost:8080/api/pay/refund \
  -H "Content-Type: application/json" \
  -d '{"orderNo":"TEST001","refundOrderNo":"REF001","refundAmount":"0.01","reason":"用户申请退款"}'
```

## 功能总览

| 能力 | 模式 | 说明 |
|------|------|------|
| **快速接入** | A | 一键生成框架级代码并写入项目（**参数前置收集**） |
| **产品选型** | B | 根据场景推荐支付方式(FrpCode) |
| **示例代码** | B | Java/Python/Go/PHP 四语言示例（只展示不写入） |
| **业务速查** | B | 参数、签名、状态流转、回调处理 |
| **质量评估** | A+B | 签名验签、业务逻辑完整性检查 |
| **问题排查** | A+B | 响应码速查、常见问题FAQ |

## 支持的支付方式

| 渠道 | 支付的支付方式 | FrpCode 前缀 |
|------|---------------|-------------|
| **微信** | 扫码(NATIVE)、被扫(CARD)、APP、H5、公众号(GZH)、小程序(XCX) | `WEIXIN_` |
| **支付宝** | 扫码(NATIVE)、被扫(CARD)、APP、H5、服务窗(FWC) | `ALIPAY_` |
| **银联** | 扫码(NATIVE)、被扫(CARD)、APP、H5 | `UNIONPAY_` |

## 文档索引

- [支付渠道与交易类型对比](./references/1-产品选型/支付渠道与交易类型对比.md)
- [签名与验签规则](./references/3-接入指南/签名与验签规则.md)
- [回调通知处理](./references/3-接入指南/回调通知处理.md)
- [接入质量检查清单](./references/3-接入指南/接入质量检查清单.md)
- [排障手册](./ references/4-问题排查/排障手册.md)
- [快速接入模板使用指南](./references/0-快速接入模板/README.md)

## 常见问题

### Q: 回调地址不能用 localhost？

是的。汇聚服务端会**主动校验** `p9_NotifyUrl` 格式，拒绝以下地址：
- `localhost` / `127.0.0.x` — 指向的是汇聚服务器自己的本机
- 内网 IP（`192.168.x.x` / `10.x.x.x` / `172.16-31.x.x`）

**本地开发解决方案**：使用内网穿透工具暴露公网地址：
```bash
ngrok http 8080      # 输出 https://xxxx.ngrok-free.app
cpolar http 8080     # 国内更快
```

### Q: MD5和RSA签名怎么选？

| | MD5 | RSA |
|--|-----|-----|
| 配置复杂度 | 低（只需32位密钥） | 高（需生成密钥对） |
| 安全性 | 中等 | 高 |
| 适用场景 | 快速原型 / 内部系统 | 生产环境 / 对外服务 |

### Q: rc_Result 返回值是什么意思？

| 支付方式 | rc_Result 含义 |
|---------|---------------|
| 扫码(NATIVE) | 二维码URL，生成二维码让用户扫描 |
| 公众号/小程序(XCX) | 调起支付的JSAPI参数 |
| H5支付 | 跳转页面URL或HTML |
| APP支付 | SDK调起支付的参数 |

### Q: 如何测试下单？

```bash
python3 ~/.codebuddy/skills/uni-pay-api/scripts/uni_pay_client.py pay \
  --order-no TEST001 --amount 0.01 --product-name 测试商品 --frp-code WEIXIN_NATIVE
```

> CLI 脚本暂仅支持 MD5 签名模式。RSA 签名请参考能力2中的 Java 代码示例。

## License

MIT
