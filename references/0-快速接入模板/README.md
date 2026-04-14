# 快速接入模板使用指南

本文档供 AI Agent 读取，指导如何使用框架模板为用户生成汇聚支付接入代码。

> ‼️ **核心原则：参数前置收集**
>
> 在执行本指南的任何步骤之前，**必须先完成 SKILL.md 步骤0的参数收集和校验**。
> 只有当 `__jp_sign_type` / `__jp_merchant_no` 等参数已缓存到上下文中后，方可开始以下步骤。

---

## 模板清单

```
0-快速接入模板/
└── SpringBoot/                    # Spring Boot 框架模板
    ├── sdk/                       # SDK工具类（必须复制）
    │   ├── JoinPayClient.java         # HTTP客户端 + 回调地址校验
    │   ├── JoinPaySignature.java      # MD5签名工具
    │   └── JoinPayRsaSignature.java   # RSA签名工具
    ├── JoinPayConfig.java         # @ConfigurationProperties 配置类
    ├── JoinPayService.java        # 核心服务（下单/查询/退款/回调验签）
    ├── PayController.java         # REST 接口层（7个接口）
    ├── NotifyController.java      # 异步回调接口（支付+退款）
    └── application.yml.snippet    # application.yml 配置片段
```

---

## Agent 执行流程

### 前置条件检查

在开始之前确认以下参数已在上下文缓存中（来自 SKILL.md 步骤0）：

| 缓存变量 | 是否必须 | 来源 |
|---------|---------|------|
| `__jp_sign_type` | ✅ 必填 | ask_followup_question 收集 |
| `__jp_merchant_no` | ✅ 必填 | ask_followup_question 收集 |
| `__jp_base_url` | ✅ 必填 | ask_followup_question 收集 |
| `__jp_notify_url` | ✅ 必填 | ask_followup_question 收集 |
| `__jp_merchant_key` | MD5模式必填 | ask_followup_question 收集 |
| `__jp_rsa_private_key` | RSA模式必填 | ask_followup_question 收集 |
| `__jp_rsa_public_key` | RSA模式建议 | ask_followup_question 收集 |

> **如果任何必填项缺失，立即回到步骤0重新收集，不要用默认值或猜测值继续！**

### 步骤1：检测项目框架

读取项目根目录的关键文件判断框架：

| 文件 | 检测条件 | 框架判断 |
|------|---------|---------|
| `pom.xml` | 包含 `spring-boot-starter-web` | Spring Boot |
| `build.gradle` | 包含 `org.springframework.boot` | Spring Boot |
| `requirements.txt` | 包含 `django` | Django |
| `package.json` | 包含 `express` | Express |

**当前仅支持 Spring Boot 模板**。若检测到其他框架：
- 询问用户是否愿意使用 Spring Boot 模板代码（用户可自行适配）
- 或告知用户暂无对应模板，仅提供参考咨询

### 步骤2：确定包路径

从构建文件中提取 groupId：

| 文件 | 提取方式 | 默认值 |
|------|---------|--------|
| `pom.xml` | `<groupId>` 标签内容 | `com.example` |
| `build.gradle` | `group` 属性 | `com.example` |

最终 Java 包路径 = `{groupId}.joinpay`

### 步骤3：读取并替换占位符

按以下替换表，将**步骤0缓存的实际参数值**写入模板：

| 占位符 | 数据来源 | 示例值 |
|--------|---------|--------|
| `${PACKAGE}` | pom.xml groupId | `com.example` |
| `${BASE_URL}` | `__jp_base_url` | `https://trade.joinpay.cc` |
| `${MERCHANT_NO}` | `__jp_merchant_no` | `888100500008456` |
| `${MERCHANT_KEY}` | `__jp_merchant_key` (MD5) | `f1075a476dff466cb67d348d4669bbd5` |
| `${RSA_PRIVATE_KEY}` | `__jp_rsa_private_key` (RSA) | `-----BEGIN...` |
| `${RSA_PUBLIC_KEY}` | `__jp_rsa_public_key` (RSA) | `-----BEGIN...` |
| `${SIGN_TYPE}` | `__jp_sign_type` | `MD5` 或 `RSA` |
| `${NOTIFY_URL}` | `__jp_notify_url` | `https://example.com/api/pay/notify` |

> **⚠️ 接口地址不可修改**：`${BASE_URL}` 只能是 `https://trade.joinpay.cc`（测试）或 `https://trade.joinpay.com`（生产），不得编造其他地址。

### 步骤4：写入项目（严格顺序）

按以下顺序使用 `write_to_file` 写入：

#### 第1批：SDK工具类（3个文件）

```
src/main/java/{PACKAGE}/joinpay/sdk/JoinPayClient.java
src/main/java/{PACKAGE}/joinpay/sdk/JoinPaySignature.java
src/main/java/{PACKAGE}/joinpay/sdk/JoinPayRsaSignature.java
```

> 这些文件从 `SpringBoot/sdk/` 目录读取，**不做占位符替换**（它们通过配置类获取参数）。

#### 第2批：业务代码（4个文件）

读取模板文件，替换 `${PLACEHOLDER}` 后写入：

```
src/main/java/{PACKAGE}/joinpay/JoinPayConfig.java       ← 替换 ${PACKAGE}
src/main/java/{PACKAGE}/joinpay/JoinPayService.java      ← 替换 ${PACKAGE}
src/main/java/{PACKAGE}/joinpay/PayController.java       ← 替换 ${PACKAGE}
src/main/java/{PACKAGE}/joinpay/NotifyController.java    ← 替换 ${PACKAGE}
```

#### 第3批：配置追加

将 `application.yml.snippet` 内容**追加**（不是覆盖）到项目的 `src/main/resources/application.yml` 文件末尾。

> 如果 application.yml 已有 `joinpay:` 配置段，应提示用户手动合并，不要自动覆盖。

### 步骤5：输出接入报告

写入完成后，向用户展示：

```markdown
✅ 接入完成报告
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
框架：Spring Boot 2.x
签名方式：{MD5|RSA}
接口环境：{测试|生产}

已写入文件（共8个）：
  📄 src/main/java/{pkg}/joinpay/sdk/JoinPayClient.java
  📄 src/main/java/{pkg}/joinpay/sdk/JoinPaySignature.java
  📄 src/main/java/{pkg}/joinpay/sdk/JoinPayRsaSignature.java
  📄 src/main/java/{pkg}/joinpay/JoinPayConfig.java
  📄 src/main/java/{pkg}/joinpay/JoinPayService.java
  📄 src/main/java/{pkg}/joinpay/PayController.java
  📄 src/main/java/{pkg}/joinpay/NotifyController.java
  📄 src/main/resources/application.yml (追加配置)

🔜 下一步：
1. 在 pom.xml 添加 Maven 依赖（见下方）
2. 启动项目，测试 POST /api/pay/create
3. 用真实设备完成一笔小额支付并验证回调
```

### 步骤6：提示依赖

提醒用户添加以下 Maven 依赖到 `pom.xml`：

```xml
<!-- fastjson（JSON解析） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>

<!-- commons-codec（编码工具） -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.15</version>
</dependency>

<!-- spring-boot-starter-web（如项目中没有） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- spring-boot-configuration-processor（配置属性提示） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

---

## API 接口清单

写入完成后，用户可使用以下接口：

| 接口 | 方法 | 说明 | 请求参数关键字段 |
|------|------|------|----------------|
| `/api/pay/create` | POST | 扫码支付下单 | orderNo, amount, productName, frpCode |
| `/api/pay/wxjsapi` | POST | 微信公众号/小程序支付 | orderNo, amount, productName, frpCode, openId, appId |
| `/api/pay/card` | POST | 付款码支付（被扫） | orderNo, amount, productName, frpCode, authCode |
| `/api/pay/query` | GET | 订单查询 | orderNo |
| `/api/pay/refund` | POST | 申请退款 | orderNo, refundOrderNo, refundAmount, reason |
| `/api/pay/refund/query` | GET | 退款查询 | refundOrderNo |
| `/api/pay/close` | POST | 关闭订单 | orderNo, frpCode |
| `/notify/pay` | POST | 支付结果异步回调 | （汇聚服务端POST调用） |
| `/notify/refund` | POST | 退款结果异步回调 | （汇聚服务端POST调用） |

---

## 接入后自检清单

代码写入后，引导用户逐项检查：

- [ ] `application.yml` 中 joinpay 配置段的商户号、密钥、回调地址是否正确
- [ ] pom.xml 是否已添加 fastjson 和 commons-codec 依赖
- [ ] 项目是否能正常编译启动（无编译错误）
- [ ] 回调地址是否外网可访问（生产环境必须）
- [ ] 是否已用 Postman/curl 测试下单接口 `POST /api/pay/create`
- [ ] 是否已完成一笔真实小额支付的端到端验证（含回调）

---

## 注意事项

1. **签名方式与密钥匹配**：`sign-type: MD5` 时只需配 `merchant-key`；`sign-type: RSA` 时需配 `rsa-private-key`
2. **回调地址校验**：所有模板代码均已内置回调地址校验，会拦截 localhost/127.0.0.1/内网IP 并给出明确错误提示
3. **幂等处理**：NotifyController 中的业务处理方法（handlePaySuccess）需要用户根据实际业务实现幂等逻辑
4. **安全性**：生产环境中密钥建议使用环境变量或配置中心管理，不要明文写在 yml 中
5. **接口地址不变性**：汇聚支付接口地址固定为 `trade.joinpay.cc`（测试）/ `trade.joinpay.com`（生产），请求参数名大小写敏感，不得擅自修改
