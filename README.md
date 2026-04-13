# uni-pay-api

汇聚支付(JoinPay)聚合支付 API 接入助手，基于**五能力架构**设计。

> 覆盖微信、支付宝、银联三大主流支付渠道，支持 MD5 和 RSA 双签名方式。

## 能力矩阵

| 能力 | 说明 | 适用场景 |
|------|------|---------|
| 产品选型 | 渠道对比 + 选型决策树 + FrpCode 映射 | 不知道该用哪种支付方式 |
| 示例代码 | Java / Python / Go / PHP 四语言 | 需要参考接口调用代码 |
| 业务知识速查 | 参数/签名/状态/回调/退款 | 查阅开发细节 |
| 接入质量评估 | 签名/业务逻辑/回调规范性检查 | 上线前自查 |
| 问题排查 | 响应码速查 + FAQ + 排障脚本 | 接口报错或异常 |

## 目录结构

```
uni-pay-api/
├── SKILL.md                              # Skill 定义（AI Agent 加载入口）
├── README.md                             # 本文件
├── config.json                           # 商户配置
├── scripts/
│   ├── uni_pay_client.py                 # 主 API 客户端（MD5签名，7个接口）
│   ├── query_order.py                    # 订单排障脚本
│   └── query_refund.py                   # 退款排障脚本
└── references/
    ├── 1-产品选型/
    │   └── 支付渠道与交易类型对比.md
    ├── 2-示例代码/
    │   ├── 接口索引.md
    │   ├── Java/                         # 10 文件
    │   ├── Python/                       # 7 文件
    │   ├── Go/                           # 3 文件
    │   └── PHP/                          # 3 文件
    ├── 3-接入指南/
    │   ├── 开发必要参数说明.md
    │   ├── 签名与验签规则.md
    │   ├── 订单状态流转.md
    │   ├── 回调通知处理.md
    │   ├── 退款与分账规则.md
    │   └── 接入质量检查清单.md
    └── 4-问题排查/
        ├── 排障手册.md
        ├── 基础支付常见问题.md
        └── 响应码完整列表.md
```

## 支持的 API 接口

| 接口 | 路径 | CLI 命令 |
|------|------|---------|
| 统一支付下单 | `/tradeRt/uniPay` | `pay` |
| 订单查询 | `/tradeRt/queryOrder` | `query` |
| 退款申请 | `/tradeRt/refund` | `refund` |
| 退款查询 | `/tradeRt/queryRefund` | `query_refund` |
| 退款信息查询 | `/tradeRt/queryRefundInfo` | `query_refund_info` |
| 关闭订单 | `/tradeRt/closeOrder` | `close` |
| 资金查询 | `/tradeRt/queryFundsControlOrder` | `query_funds` |

## 支付渠道

| 渠道 | 支付方式 | FrpCode |
|------|---------|---------|
| 微信 | 主扫 / 被扫 / APP / H5 / 公众号 / 小程序 / 收银台 | `WEIXIN_NATIVE` / `WEIXIN_CARD` / `WEIXIN_APP` 等 |
| 支付宝 | 主扫 / 被扫 / APP / H5 / 服务窗 / 收银台 | `ALIPAY_NATIVE` / `ALIPAY_CARD` / `ALIPAY_APP` 等 |
| 银联 | 主扫 / 被扫 / APP / H5 / 收银台 | `UNIONPAY_NATIVE` / `UNIONPAY_CARD` / `UNIONPAY_APP` 等 |

## 签名方式

### MD5

```
排除hmac → 按key排序 → 拼接value → MD5(拼接值 + 密钥).toUpperCase()
```

### RSA

```
排除hmac → 按key排序 → 拼接value → RSA私钥签名(SHA256WithRSA) → Base64
```

## 快速使用

### 1. 配置商户信息

编辑 `config.json`：

```json
{
  "default_merchant_no": "商户编号",
  "merchant_key": "32位MD5密钥",
  "rsa_private_key": "RSA私钥（可选）",
  "rsa_public_key": "RSA公钥（可选）",
  "base_url": "https://trade.joinpay.cc",
  "default_notify_url": "回调通知地址",
  "sign_type": "MD5"
}
```

### 2. CLI 调用示例

```bash
# 微信扫码支付
python3 scripts/uni_pay_client.py pay \
  --order-no "ORD20260413001" \
  --amount "0.01" \
  --product-name "商品名称" \
  --frp-code "WEIXIN_NATIVE"

# 查询订单
python3 scripts/uni_pay_client.py query --order-no "ORD20260413001"

# 申请退款
python3 scripts/uni_pay_client.py refund \
  --order-no "ORD20260413001" \
  --refund-order-no "REF001" \
  --refund-amount "0.01" \
  --reason "用户申请退款"
```

### 3. 排障脚本

```bash
python3 scripts/query_order.py \
  --merchant-no "商户号" \
  --key "密钥" \
  --order-no "订单号"

python3 scripts/query_refund.py \
  --merchant-no "商户号" \
  --key "密钥" \
  --order-no "订单号"
```

## 代码示例

各语言 SDK 工具类可直接复制到项目中使用（改包名即可）：

| 语言 | SDK 类 | 依赖 |
|------|--------|------|
| Java | `JoinPaySignature` / `JoinPayRsaSignature` / `JoinPayClient` | commons-codec, fastjson |
| Python | `joinpay_signature` / `joinpay_rsa_signature` / `joinpay_client` | 无（标准库） |
| Go | `JoinPaySignature` / `JoinPayRsaSignature` / `JoinPayClient` | 无（标准库） |
| PHP | `JoinPaySignature` / `JoinPayRsaSignature` / `JoinPayClient` | curl 扩展 |

> 还有 `JoinPayNativePayTest.java` 零依赖文件，可直接 `javac` + `java` 运行。

## 环境地址

| 环境 | 地址 |
|------|------|
| 测试 | `https://trade.joinpay.cc` |
| 生产 | `https://trade.joinpay.com` |

## 相关链接

- [汇聚支付官网](https://www.joinpay.com)
- [汇聚支付商户后台](https://b.joinpay.com)

## License

参考代码仅供学习和对接参考使用。
