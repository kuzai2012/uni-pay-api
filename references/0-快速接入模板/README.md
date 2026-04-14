# 快速接入模板使用指南

本文档供 AI Agent 读取，指导如何使用框架模板为用户生成支付接入代码。

## 模板清单

```
0-快速接入模板/
├── SpringBoot/                    # Spring Boot 框架模板
│   ├── sdk/                       # SDK工具类（必须复制）
│   │   ├── JoinPayClient.java         # HTTP客户端
│   │   ├── JoinPaySignature.java      # MD5签名工具
│   │   └── JoinPayRsaSignature.java   # RSA签名工具
│   ├── JoinPayConfig.java         # 配置类（读取 application.yml）
│   ├── JoinPayService.java        # 核心服务（下单/查询/退款/回调验签）
│   ├── PayController.java         # REST 接口层
│   ├── NotifyController.java      # 异步回调接口
│   └── application.yml.snippet    # 配置片段
└── README.md                      # 本文档
```

## Agent 执行流程

### 1. 检测项目框架

读取项目根目录的关键文件判断框架：

| 文件 | 框架判断 |
|------|----------|
| `pom.xml` 包含 `spring-boot-starter-web` | Spring Boot |
| `build.gradle` 包含 `org.springframework.boot` | Spring Boot |
| `requirements.txt` 包含 `django` | Django |
| `package.json` 包含 `express` | Express |

### 2. 确定写入目录

| 框架 | 基础包路径 | SDK包路径 |
|------|-----------|----------|
| Spring Boot | `src/main/java/{groupId}/joinpay/` | `src/main/java/{groupId}/joinpay/sdk/` |

从 `pom.xml` 中提取 groupId，默认使用 `com.example`。

### 3. 读取模板并替换占位符

占位符替换表：

| 占位符 | 数据来源 | 说明 |
|--------|----------|------|
| `${PACKAGE}` | 项目groupId | Java包名，如 `com.example` |
| `${BASE_URL}` | config.json 或用户输入 | 接口地址 |
| `${MERCHANT_NO}` | config.json 或用户输入 | 商户号 |
| `${MERCHANT_KEY}` | config.json 或用户输入 | MD5密钥 |
| `${RSA_PRIVATE_KEY}` | config.json 或用户输入 | RSA私钥（RSA模式） |
| `${RSA_PUBLIC_KEY}` | config.json 或用户输入 | RSA公钥（RSA模式） |
| `${SIGN_TYPE}` | 用户选择 | MD5 或 RSA |
| `${NOTIFY_URL}` | config.json 或用户输入 | 回调地址 |

### 4. 写入文件

使用 `write_to_file` 工具写入以下文件：

**SDK工具类**（必须）：
```
src/main/java/{groupId}/joinpay/sdk/JoinPayClient.java
src/main/java/{groupId}/joinpay/sdk/JoinPaySignature.java
src/main/java/{groupId}/joinpay/sdk/JoinPayRsaSignature.java
```

**业务代码**：
```
src/main/java/{groupId}/joinpay/JoinPayConfig.java
src/main/java/{groupId}/joinpay/JoinPayService.java
src/main/java/{groupId}/joinpay/PayController.java
src/main/java/{groupId}/joinpay/NotifyController.java
```

配置片段追加到 `src/main/resources/application.yml`。

### 5. 提示依赖

如果使用 Spring Boot 模板，提示用户添加 Maven 依赖：

```xml
<!-- pom.xml 添加 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.15</version>
</dependency>

<!-- Spring Boot Web（如未添加）-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Configuration Properties -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

## 接入后自检清单

代码写入后，Agent 应引导用户检查：

- [ ] `application.yml` 配置是否正确（商户号、密钥、回调地址）
- [ ] Maven 依赖是否添加（fastjson、commons-codec）
- [ ] 回调地址是否外网可访问（生产环境）
- [ ] 是否已测试下单接口 `POST /api/pay/create`
- [ ] 是否已测试回调接收

## API 接口清单

写入完成后，用户可使用以下接口：

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

## 示例对话

```
用户：帮我在Spring Boot项目里接入微信扫码支付，用MD5签名

AI：好的，我来帮你快速接入。

[读取项目 pom.xml，提取 groupId=com.mystore]

[读取模板文件，替换 ${PACKAGE} 为 com.mystore]

[写入SDK工具类]
[写入业务代码]

✅ 接入完成！已写入以下文件：
- src/main/java/com/mystore/joinpay/sdk/JoinPayClient.java
- src/main/java/com/mystore/joinpay/sdk/JoinPaySignature.java
- src/main/java/com/mystore/joinpay/sdk/JoinPayRsaSignature.java
- src/main/java/com/mystore/joinpay/JoinPayConfig.java
- src/main/java/com/mystore/joinpay/JoinPayService.java
- src/main/java/com/mystore/joinpay/PayController.java
- src/main/java/com/mystore/joinpay/NotifyController.java

请完成以下步骤：
1. 在 pom.xml 添加 fastjson 和 commons-codec 依赖
2. 在 application.yml 中添加汇聚支付配置（商户号、密钥、回调地址）
3. 启动项目测试 POST /api/pay/create
```

## 注意事项

1. **签名方式**：MD5模式只需配置 `merchant-key`，RSA模式需配置 `rsa-private-key` 和 `rsa-public-key`
2. **回调地址**：生产环境必须是外网可访问的HTTPS地址
3. **幂等处理**：回调处理逻辑必须做幂等校验，避免重复处理同一订单
4. **安全性**：密钥不要硬编码，建议使用环境变量或配置中心
