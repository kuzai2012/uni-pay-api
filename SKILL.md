---
name: uni-pay-api
description: 汇聚支付聚合支付API接入助手，涵盖统一支付、订单查询、退款、关单、资金查询，支持微信/支付宝/银联三大渠道和MD5/RSA双签名方式。提供选型/代码示例/业务速查/质量评估/排障五大能力。Use when user mentions "汇聚支付", "JoinPay", "聚合支付", "WEIXIN", "ALIPAY", "UNIONPAY", "扫码支付", "被扫", "公众号支付", "小程序支付", "H5支付", "APP支付", "付款码", or asks to call JoinPay API, check payment status, process refund, query order, close order, or troubleshoot payment issues.
allowed-tools:
disable: false
---

# 汇聚支付聚合支付接入指引

本技能提供汇聚支付(JoinPay)聚合支付API的完整接入能力，覆盖**微信、支付宝、银联**三大主流支付渠道。

---

## 工作模式

本技能支持**双模式运行**，根据用户意图自动切换：

### 模式A：快速接入模式（推荐）

**触发条件**：用户表达"接入"、"集成"、"添加支付功能"、"帮我把支付加上"等集成意图

**核心特征**：
- ✅ **可直接写入用户项目文件**（使用 `write_to_file` / `replace_in_file`）
- ✅ 使用框架级模板生成代码（Controller + Service + Config）
- ✅ **参数前置收集**（在一切操作之前先收齐所有必要参数）
- ✅ 完成后自动触发质量评估

### 模式B：参考咨询模式

**触发条件**：用户问具体技术问题、"查一下"、"给我看示例"、"签名怎么算"等咨询意图

**核心特征**：
- ⛔ **只展示、不写入**（保持原有规则）
- ⛔ **只检索、不生成**（从代码示例文件中检索获取）
- ✅ 分步确认协议保持不变
- ✅ 5大能力完整保留

---

## 全局交互规范

> ‼️ 以下规则适用于本技能所有能力、所有对话轮次，优先级高于各能力的局部规则。

1. **所有问题必须得到用户明确回答后才能继续。** 如果一次提出了多个问题，必须逐一检查每个问题是否都已获得用户的明确答复。对于未回答的问题，必须再次追问，**严禁对未回答的问题自行假设、推断或使用默认值**。

2. **签名方式前置确认**：任何涉及实际API调用或代码生成的场景，须先确认**签名方式**（MD5 或 RSA），已明确则无需重复。两种方式的核心差异见 → [📄 签名与验签规则.md](./references/3-接入指南/签名与验签规则.md)。

3. **分步确认协议**（简单知识问答除外，需要帮用户排查、分析或执行操作时必须遵守）：
   - **① 明确需求**：先理解用户问题，给出初步判断或原因分析，不要一上来就堆参数清单。
   - **② 征得同意**：主动提出下一步能做什么，**等用户明确同意后**才继续，严禁用户没表态就开始收集参数或执行操作。
   - **③ 收集信息**：用户同意后再告知需要哪些信息并逐项收集，收齐才能执行。
   - **④ 执行前确认**：准备执行操作前，简要说明即将做什么，确认用户同意后再执行；涉及线上环境须额外提示风险。

4. **快速接入模式的参数前置收集原则**（仅限模式A）：
   - **必须在项目扫描、模板加载、代码写入之前的第一个动作就是收集参数**
   - **必须一次性收齐所有必要参数**，不要分多轮询问
   - **必须使用 `ask_followup_question` 工具以结构化表单形式收集**，不要逐个文字提问
   - 收齐参数后，将参数值缓存到上下文中供后续所有步骤使用，后续步骤不再重复询问

5. **接口地址与参数的稳定性铁律**：
   - > ‼️ **汇聚支付的接口地址、请求参数名、响应字段名是固定不变的。**
   - 测试环境地址恒定为 `https://trade.joinpay.cc`
   - 生产环境地址恒定为 `https://trade.joinpay.com`
   - **接口路径（完整URL = 域名 + 路径）**：
     | 接口名称 | 路径 | 完整测试URL |
     |---------|------|------------|
     | 统一支付下单 | `/tradeRt/uniPay` | `https://trade.joinpay.cc/tradeRt/uniPay` |
     | 订单查询 | `/tradeRt/queryOrder` | `https://trade.joinpay.cc/tradeRt/queryOrder` |
     | 退款 | `/tradeRt/refund` | `https://trade.joinpay.cc/tradeRt/refund` |
     | 退款查询 | `/tradeRt/queryRefund` | `https://trade.joinpay.cc/tradeRt/queryRefund` |
     | 关闭订单 | `/tradeRt/closeOrder` | `https://trade.joinpay.cc/tradeRt/closeOrder` |
     | 资金管控订单查询 | `/tradeRt/queryFundsControlOrder` | `https://trade.joinpay.cc/tradeRt/queryFundsControlOrder` |
   - 所有请求参数名（如 `p0_Version`、`p1_MerchantNo`、`p9_NotifyUrl`）大小写敏感，**不得擅自修改或猜测**
   - 响应字段 `ra_Code` / `rb_CodeMsg` 格式固定
   - Agent 在生成代码或调试问题时，**始终以本 skill 的定义为准**，不得凭记忆或猜测编造参数名或接口路径
   - > 🚫 **禁止行为**：**严禁使用 web_search、web_fetch 等工具搜索汇聚支付官方文档或接口地址**。本 skill 已包含所有必要信息，无需外部查询。若 Agent 尝试搜索外部文档，视为违反本 skill 规则
   - > 📋 **参数固定值约束表（所有签名方式通用，MD5/RSA 均适用）**：
     > ‼️ **以下参数的值为汇聚支付服务端规定的固定常量，Agent 在生成任何代码时必须严格遵守，不得自行编造、猜测或省略。**

     | 参数名 | 版本要求 | 适用接口 | 说明 |
     |--------|---------|----------|------|
     | `p0_Version` | ≥ `2.6`（当前默认 `2.6`） | 统一支付(`/tradeRt/uniPay`)、订单查询(`/tradeRt/queryOrder`) | 支付类接口版本线，随系统升级可递增 |
     | `p0_Version` | ≥ `2.3`（当前默认 `2.3`） | 退款(`/tradeRt/refund`)、退款查询(`/tradeRt/queryRefund`) | 退款类接口版本线，独立迭代 |
     | `p0_Version` | ≥ `1.0`（当前默认 `1.0`） | 关闭订单(`/tradeRt/closeOrder`)、资金管控订单查询(`/tradeRt/queryFundsControlOrder`) | 其他接口版本线 |
     | `p4_Cur` | 固定 `1`（人民币） | 统一支付(`/tradeRt/uniPay`) | 汇聚服务端规定币种标识固定为 `"1"` |

     > 🚫 **禁止行为**：
     > - 禁止将 `p0_Version` 设为上述范围以外的任何值
     > - 禁止将 `p4_Cur` 设为除 `"1"` 以外的任何值（如 `"CNY"` / `"RMB"` / `"156"` 等）
     > - 禁止省略 `p0_Version` 或 `p4_Cur` 参数
     > - Agent 在生成代码时遇到上述参数，必须显式使用上述规定值进行赋值

  - > 📋 **MD5 签名约束（sign_type=MD5 时必须遵守）**：
    > ‼️ **MD5 签名的关键规范，违反将导致服务端验签失败。**
    >
    > | 规范项 | 正确实现 | 错误实现（禁止） |
    > |--------|---------|----------------|
    > | **拼接格式** | 只拼接 **value**，不包含 key 名 | `"key=value&key=value"` 格式 |
    > | **空值处理** | 过滤掉空字符串和 null 值 | 将空值参与拼接 |
    > | **密钥位置** | 在末尾追加密钥后整体 MD5 哈希 | 先对各段分别 MD5 再组合 |
    >
    > **签名字符串构建示例**：
    > ```java
    > // ✅ 正确：只拼接 value（与 SignBiz.java:81-113 一致）
    > // 参数: p0_Version=2.6, p1_MerchantNo=888100500008456, p3_Amount=0.01
    > // 排序后 keys: [p0_Version, p1_MerchantNo, p3_Amount]
    > // 签名串: "2.68881005000084560.01"
    > // 最终 hmac = MD5("2.68881005000084560.01" + merchantKey).toUpperCase()
    >
    > // ❌ 错误：包含 key
    > // 签名串: "p0_Version=2.6&p1_MerchantNo=888100500008456&p3_Amount=0.01"  // 错误！
    > ```

   - > 📋 **RSA 签名约束（sign_type=RSA 时必须遵守）**：
     > ‼️ **RSA 签名的两个关键规范，违反将导致服务端验签失败。**
     >
     > | 规范项 | 正确实现 | 错误实现（禁止） |
     > |--------|---------|----------------|
     > | **签名算法** | `MD5withRSA`（与 RSAUtils.java:76 一致） | `SHA256withRSA` / `SHA1withRSA` 等其他算法 |
     > | **待签名串** | 只拼接 **value**，不包含 key | `"key=" + key + "&value=" + value` 或 `"key=value"` 格式 |
     >
     > **签名字符串构建示例**：
     > ```java
     > // ✅ 正确：只拼接 value
     > // 参数: p0_Version=2.6, p1_MerchantNo=888100500008456, p3_Amount=0.01
     > // 排序后 keys: [p0_Version, p1_MerchantNo, p3_Amount]
     > // 签名串: "2.68881005000084560.01"
     >
     > // ❌ 错误：包含 key
     > // 签名串: "p0_Version=2.6&p1_MerchantNo=888100500008456&p3_Amount=0.01"  // 错误！
     > ```

   - > 📋 **参数命名规范（禁止推断/修改参数名）**：
     > ‼️ **参数名由汇聚支付服务端定义，Agent 不得根据前缀规律自行推断或修改。**
     >
     > | 参数名 | 说明 | 常见错误 |
     > |--------|------|---------|
     > | `q1_FrpCode` | 支付渠道编码（微信/支付宝/银联） | ❌ 禁止写成 `p6_FrpCode`（错误推断为 p 序列延续） |
     > | `qa_TradeMerchantNo` | 报备商户号（服务商模式） | ❌ 禁止写成 `p10_TradeMerchantNo` |
     > | `q3_SubMerchantNo` | 子商户号 | ❌ 禁止写成 `p_SubMerchantNo` |
     >
     > **参数前缀规律说明**：
     > - `p` 前缀：基础参数（p0~p9），如 p0_Version, p1_MerchantNo, p2_OrderNo...
     > - `q` 前缀：扩展参数（q1~q9），如 q1_FrpCode, q3_SubMerchantNo...
     > - `qa` 前缀：服务商模式参数，如 qa_TradeMerchantNo
     >
     > **禁止行为**：禁止将 `q1_FrpCode` 改为任何其他名称（如 `p6_FrpCode` / `frpCode` / `frp_code` 等）

  - > 📋 **响应码成功判定约束（所有接口通用）**：
    > ‼️ **响应码判断的唯一标准，违反将导致业务逻辑错误。**
    >
    > **响应格式前置要求（必须先满足）**：
    >
    > | 规范项 | 正确实现 | 错误实现（禁止） |
    > |--------|---------|-----------------|
    > | **响应格式** | 所有接口统一返回 **JSON** 格式，必须先用 JSON 解析器解析响应体后再取字段 | 将响应当作纯文本、XML、form-encoded 字符串处理 |
    > | **非JSON兜底** | 解析失败时构造 `{"ra_Code":"-1","rb_CodeMsg":"非JSON响应:..."}` 统一错误结构 | 直接抛异常、忽略响应、或按字符串截取 |
    > | **Content-Type** | 请求头设为 `application/x-www-form-urlencoded; charset=utf-8`，但响应体始终是 JSON | 假设响应 Content-Type 与请求相同 |
    >
    > ```python
    > # ✅ 正确：先 JSON 解析，失败时兜底
    > try:
    >     result = json.loads(response_body)
    > except json.JSONDecodeError:
    >     result = {"ra_Code": "-1", "rb_CodeMsg": f"非JSON响应: {response_body[:200]}"}
    > # 然后再判断 ra_Code
    > if result.get("ra_Code") in ("100", 100):
    >     ...
    >
    > # ❌ 错误：未解析直接当字符串用
    > if "success" in response_body:  # 危险！可能误判
    > ```
    >
    > | 响应码字段 | 成功值 | 错误判定（禁止） |
    > |-----------|--------|-----------------|
    > | `ra_Code` | `100` 或 `"100"` | 将 `110`、`200`、`0`、`"success"` 等其他任何值判定为成功 |
    >
    > **代码示例**：
    > ```python
    > # ✅ 正确：严格判断 ra_Code == 100
    > if response.get('ra_Code') == '100' or response.get('ra_Code') == 100:
    >     print("支付成功")
    > else:
    >     print(f"支付失败: {response.get('rb_CodeMsg')}")
    >
    > # ❌ 错误：宽松判断或错误判断
    > if response.get('ra_Code') == '200':  # 错误！200 不是成功码
    > if response.get('ra_Code') != '100':  # 错误！!=100 不代表失败（可能是110等）
    > if response.get('ra_Code', '').startswith('1'):  # 错误！110、10080000等也以1开头
    > ```
    >
    > **常见错误响应码示例**：
    > - `110` → 系统正忙（需稍后重试）
    > - `10080000` → 签名验证失败
    > - `10080001` → 参数为空
    > - `10080005` → 订单号重复
    >
    > **禁止行为**：
    > - 禁止将除 `100`/`"100"` 以外的任何响应码判定为成功
    > - 禁止用 HTTP 状态码（如 200 OK）代替 `ra_Code` 判断业务成功

6. **环境边界与错误归因铁律**：
   - > 🔍 **区分环境错误与代码错误**：当代码运行报错时，Agent 应先判断错误类型，再决定处理方式：
     - **环境类错误**：JDK未安装、Maven未配置、Python版本不兼容、依赖包未安装、端口占用、网络不通等 → **禁止修改接口/参数/签名逻辑**，应提供环境修复命令并引导用户执行
     - **代码类错误**：语法错误、空指针、类型不匹配、业务逻辑错误等 → 可排查并修正代码
   - > ⚠️ **环境安装操作需用户确认**：涉及以下操作时，Agent **必须**先展示命令并征得用户同意，不得静默自动执行：
     - 安装运行时（JDK/Python/Node/Go）
     - 安装包管理器（Maven/pip/npm）
     - 安装依赖包（`mvn install`/`pip install`/`npm install`）
     - 启动/重启服务
   - > ✅ **提供可执行的环境修复命令**：环境问题应主动给出具体命令，格式如下：
     ```
     检测到环境问题：[问题描述]
     
     建议执行以下命令修复：
     ```bash
     [具体命令]
     ```
     
     是否执行？确认后我将帮您运行。
     ```
   - > 🚫 **严禁因环境错误修改 SDK 核心逻辑**：以下内容在环境错误场景下**禁止修改**：
     - 接口路径（如 `/tradeRt/uniPay`）
     - 请求参数名（如 `p0_Version`、`hmac`）
     - 签名算法实现
     - SDK 工具类的 HTTP 通信/参数组装/签名逻辑

---

## 能力概览

| 能力 | 模式 | 说明 |
|------|------|------|
| **能力0：快速接入** | A | 一键生成框架级代码并写入项目 |
| **能力1：产品选型** | B | 根据场景推荐交易类型(FrpCode) |
| **能力2：示例代码** | B | 四语言代码示例（只展示不写入） |
| **能力3：业务知识速查** | B | 参数、签名、状态流转、回调处理 |
| **能力4：接入质量评估** | A+B | 签名验签、业务逻辑完整性检查 |
| **能力5：问题排查** | A+B | 响应码速查、常见问题FAQ、排障脚本 |

---

## 能力0：快速接入

> 用户表达接入意图时触发，**首先收集参数，然后扫描项目并生成框架级代码**。

### ⚡ 步骤0：前置参数收集（最重要！）

> ‼️ **这是快速接入模式的第一个步骤，必须在任何其他操作之前完成。**
>
> ‼️ **不要先扫描项目再问参数！不要先生成代码再补参数！**

当用户触发快速接入模式且已获得用户同意后，**立即**使用 `ask_followup_question` 以**单次结构化表单**收集以下全部必要信息：

#### 必须收集的参数清单

| # | 参数字段 | 必填 | 说明 | 选项/格式要求 |
|---|---------|------|------|--------------|
| 1 | `sign_type` | ✅ | 签名方式 | 单选：`MD5` / `RSA` |
| 2 | `merchant_no` | ✅ | 商户号 | 纯数字字符串 |
| 3 | `base_url` | ✅ | 接口环境 | 单选：`测试环境(https://trade.joinpay.cc)` / `生产环境(https://trade.joinpay.com)` |
| 4 | `notify_url` | ✅ | 异步回调地址 | URL格式，**不得为 localhost/127.0.0.1/内网IP** |
| 5 | `merchant_key` | 条件必填 | MD5密钥 | 仅 MD5 模式必填，32位字符串 |
| 6 | `rsa_private_key` | 条件必填 | RSA私钥 | 仅 RSA 模式必填，PKCS8 PEM 格式 |
| 7 | `rsa_public_key` | 条件必填 | RSA公钥 | 仅 RSA 模式建议填写，PKCS8 PEM 格式 |
| 8 | `trade_merchant_no` | ❌ 可选 | 报备商户号 | 服务商模式时填写 |

#### 信息收集流程（混合模式）

> ⚠️ **关键约束**：`ask_followup_question` 是**多选题工具**，不支持自由文本输入。因此必须采用「选项参数用工具 + 文本参数用对话」的混合模式：

**第一步：调用 ask_followup_question 收集选项型参数**

Agent 构建一次 `ask_followup_question` 调用，仅放入有明确选项的参数：

```json
{
  "title": "汇聚支付快速接入 - 基础配置",
  "questions": [
    {
      "id": "sign_type",
      "question": "请选择签名方式：",
      "options": ["MD5（简单快捷）", "RSA（安全性更高）"],
      "multiSelect": false
    },
    {
      "id": "base_url",
      "question": "请选择接口环境：",
      "options": [
        "测试环境 https://trade.joinpay.cc",
        "生产环境 https://trade.joinpay.com"
      ],
      "multiSelect": false
    }
  ]
}
```

**第二步：通过直接对话逐项收集文本输入型参数**

收到用户的选项回答后，Agent 通过**普通对话消息**逐个询问文本输入参数，每问一个等用户回复后再问下一个：

| 顺序 | 参数ID | 问题文案 | 输入提示 |
|------|--------|---------|---------|
| 1 | merchant_no | `请输入您的汇聚支付商户号（在商户后台「账户信息」中查看）：` | 纯数字字符串 |
| 2 | notify_url | `请输入异步回调通知地址（⚠️ 不得使用 localhost/127.0.0.1/内网IP）：` | 格式如 `https://your-domain.com/api/pay/notify`<br/>本地开发需先用 ngrok/cpolar 获取公网地址 |
| 3a | merchant_key | `请输入MD5签名密钥（32位，在商户后台「秘钥管理」中获取）：` | 仅MD5模式时询问 |
| 3b | rsa_private_key | `请输入RSA私钥（PKCS8 PEM格式，含 -----BEGIN/END----- 标记）：` | 仅RSA模式时询问 |

> **交互要求**：
> - 每次只问**一个问题**，等待用户回复后再问下一个
> - 用户回复后**立即校验**（见下方校验规则），不合格当场让用户修正
> - **禁止一次性列出所有问题**，避免用户遗漏或混淆
>
> 若用户选择 RSA 签名方式，第 3 步替换为 rsa_private_key 的收集。

#### 参数校验规则

收集到参数后，Agent **必须当场校验**，不合格立即让用户修正：

| 校验项 | 规则 | 不合格时的提示 |
|--------|------|---------------|
| merchant_no | 非空、纯数字 | `"商户号不能为空，请在汇聚商户后台 → 账户信息中查看"` |
| base_url | 以 https:// 开头 | `"接口地址格式错误"` |
| notify_url | 非 localhost/127.x/内网IP | `"❌ 回调地址不能使用 localhost 或内网IP！本地开发请先用 ngrok/cpolar 等内网穿透工具暴露公网地址，再将穿透后的地址填入此处"` |
| notify_url | 以 http(s):// 开头 | `"回调地址必须是有效的URL格式"` |
| merchant_key(MD5) | 非空 | `"MD5密钥不能为空，请在商户后台「秘钥管理」中获取"` |
| rsa_private_key(RSA) | 含 BEGIN PRIVATE KEY / END 标记 | `"RSA私钥格式不正确，应为 PKCS8 PEM 格式（含 -----BEGIN/END----- 标记）"` |

> **所有校验通过后方可进入步骤1。任何一项不通过都应立即拦截，不继续后续步骤。**

#### 参数缓存

校验通过后将参数缓存供后续步骤引用：

```
__jp_sign_type         = "MD5" | "RSA"
__jp_merchant_no       = 商户号
__jp_base_url          = 接口地址(全称)
__jp_notify_url        = 回调地址
__jp_merchant_key      = MD5密钥 (MD5模式)
__jp_rsa_private_key   = RSA私钥 (RSA模式)
__jp_rsa_public_key    = RSA公钥 (RSA模式)
__jp_trade_merchant_no = 报备商户号 (可选)
```

### 支持的框架模板

| 框架 | 模板位置 | 包含文件 |
|------|----------|----------|
| Spring Boot | `references/0-快速接入模板/SpringBoot/` | Config + Service + Controller + NotifyController + SDK工具类 |

### 步骤1：扫描项目识别框架 + 环境预检

> 此步骤在步骤0完成后执行。

**1a. 框架识别**

读取项目根目录关键文件识别框架：
- `pom.xml` 含 `spring-boot-starter-web` → Spring Boot
- `build.gradle` 含 `org.springframework.boot` → Spring Boot
- `requirements.txt` 含 `django` → Django
- `package.json` 含 `express` → Express

若检测到的框架无对应模板，告知用户当前仅支持 Spring Boot，询问是否继续。

**1b. 运行时环境预检**

框架识别后，同步检测目标语言的基础运行环境是否就绪（**仅做检测报告，不执行任何安装操作**）：

| 目标语言 | 检测项 | 未就绪时的标注 |
|---------|--------|--------------|
| Java | `JAVA_HOME` 是否设置、`java --version` 可用 | 标注 `⚠️ JDK未安装或JAVA_HOME未配置` |
| Python | `python3 --version` 可用 | 标注 `⚠️ Python3 未安装` |
| Node.js | `node --version` 可用 | 标注 `⚠️ Node.js 未安装` |

> **处理规则**：
> - 环境已就绪 → 正常进入步骤2
> - 环境未就绪 → **不中断流程**，继续完成代码写入，但在步骤4报告中明确标注 `⚠️ 环境待确认`
> - **禁止**自动执行安装命令或尝试调用接口验证

### 步骤2：加载模板并替换占位符

读取 `references/0-快速接入模板/{框架}/` 下模板文件，用**步骤0缓存值**替换占位符：

| 占位符 | 数据来源 | 说明 |
|--------|----------|------|
| `${PACKAGE}` | pom.xml groupId | Java包名，如 `com.example` |
| `${BASE_URL}` | `__jp_base_url` | 接口地址 |
| `${MERCHANT_NO}` | `__jp_merchant_no` | 商户号 |
| `${MERCHANT_KEY}` | `__jp_merchant_key` | MD5密钥 |
| `${RSA_PRIVATE_KEY}` | `__jp_rsa_private_key` | RSA私钥 |
| `${RSA_PUBLIC_KEY}` | `__jp_rsa_public_key` | RSA公钥 |
| `${SIGN_TYPE}` | `__jp_sign_type` | 签名方式 |
| `${NOTIFY_URL}` | `__jp_notify_url` | 回调地址 |

### 步骤3：写入项目

按顺序写入：

1. **SDK工具类** → `{pkg}/joinpay/sdk/JoinPayClient.java` 等3个文件
2. **配置类** → `{pkg}/joinpay/JoinPayConfig.java`
3. **服务层** → `{pkg}/joinpay/JoinPayService.java`
4. **Controller** → `{pkg}/joinpay/PayController.java`
5. **回调Controller** → `{pkg}/joinpay/NotifyController.java`
6. **配置追加** → `application.yml` 追加 joinpay 配置段

### 步骤4：输出接入报告

```
✅ 接入完成报告
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
框架：Spring Boot 2.x
签名方式：{MD5|RSA}
接口环境：{测试|生产}
代码状态：✅ 已写入（共8个文件）
环境状态：{✅ 已就绪 | ⚠️ 待确认}

已写入文件：
  📄 src/main/java/{pkg}/joinpay/sdk/JoinPayClient.java
  📄 src/main/java/{pkg}/joinpay/sdk/JoinPaySignature.java
  📄 src/main/java/{pkg}/joinpay/sdk/JoinPayRsaSignature.java
  📄 src/main/java/{pkg}/joinpay/JoinPayConfig.java
  📄 src/main/java/{pk}/joinpay/JoinPayService.java
  📄 src/main/java/{pkg}/joinpay/PayController.java
  📄 src/main/java/{pkg}/joinpay/NotifyController.java
  📄 src/main/resources/application.yml (追加配置)

{若步骤1b环境检测通过}
🎉 环境已就绪，可执行以下操作开始测试：
1. pom.xml 添加 fastjson + commons-codec 依赖
2. 执行 mvn clean compile 验证编译
3. 启动项目，测试 POST /api/pay/create
4. 用真实设备完成一笔小额支付并验证回调

{若步骤1b环境检测未通过}
⚠️ 环境待确认 - 代码已就绪但运行时环境未就绪
请先手动确认以下环境项后，再执行上述测试步骤：
- [ ] Java: 确认 JAVA_HOME 已设置且 java --version 可用
- [ ] Maven: 确认 mvn --version 可用
- [ ] 依赖: 执行 mvn install 安装项目依赖
```

> **Agent 权限边界**：Agent 负责代码写入和报告输出。依赖安装、服务启动、接口调用均需用户手动操作或明确授权后由 Agent 辅助执行。

### 完整流程示意

```
用户："帮我在我的项目里接入微信扫码支付"

  Agent 执行：
  ┌─────────────────────────────────────┐
  │ ① 确认意图，征得用户同意              │
  ├─────────────────────────────────────┤
  │ ② ⚡ 步骤0：分阶段收集参数            │
  │   a) ask_followup_question          │
  │      → 签名方式(MD5/RSA)、接口环境    │
  │   b) 直接对话逐项提问                │
  │      → 商户号 → 回调地址 → 密钥      │
  │      （每问一个，等回复后再问下一个）   │
  ├─────────────────────────────────────┤
  │ ③ 当场校验参数合法性                  │
  │   （回调地址非localhost/密钥32位等）  │
  ├─────────────────────────────────────┤
  │ ④ 步骤1：扫描项目 → 识别Spring Boot   │
  │   + 运行时环境预检（JDK/Maven等）     │
  ├─────────────────────────────────────┤
  │ ⑤ 步骤2：加载模板 + 替换占位符        │
  │   （用步骤0缓存的实际参数值）          │
  ├─────────────────────────────────────┤
  │ ⑥ 步骤3：写入8个文件到项目            │
  ├─────────────────────────────────────┤
  │ ⑦ 步骤4：输出接入报告（含环境状态）    │
  └─────────────────────────────────────┘
```

### 模板使用指南

详见 → [📄 快速接入模板使用指南.md](./references/0-快速接入模板/README.md)

---

## CLI 脚本执行层

本技能保留 `scripts/uni_pay_client.py` 作为底层API调用工具（支持MD5签名）。在需要实际调用API验证时使用：

```bash
python3 <skill_dir>/scripts/uni_pay_client.py <command> [args...]
```

**命令列表：**
| 命令 | 说明 | 核心必填参数 |
|------|------|-------------|
| `pay` | 统一支付下单 | --order-no, --amount, --product-name, --frp-code |
| `query` | 订单查询 | --order-no |
| `refund` | 退款申请 | --order-no, --refund-order-no, --refund-amount, --reason |
| `query_refund` | 退款查询 | --refund-order-no |
| `query_refund_info` | 退款信息查询 | --order-no |
| `close` | 关闭订单 | --order-no, --frp-code |
| `query_funds` | 资金查询 | --order-no |

> RSA签名请参考能力2中的代码示例实现，CLI脚本暂仅支持MD5模式。

---

## 能力1：产品选型（模式B）

> 用户问「该用哪种支付方式」或比较各渠道区别时 → 加载对应文档，确定FrpCode后再按需加载示例代码。

### 三大渠道总览

| 渠道 | 前缀 | 支持的支付方式 | 典型场景 |
|------|------|---------------|---------|
| 微信 | WEIXIN_ | 主扫(NATIVE)、被扫(CARD)、APP、APP3、H5、H5+、公众号(GZH)、小程序(XCX)、收银台(SYT)、小程序插件(CJXCX) | 社交生态内支付 |
| 支付宝 | ALIPAY_ | 主扫(NATIVE)、被扫(CARD)、APP、H5、服务窗(FWC)、收银台(SYT) | 电商/线下收款 |
| 银联 | UNIONPAY_ | 主扫(NATIVE)、被扫(CARD)、APP、H5、收银台(SYT)、云微小程序(WXMP) | 大额支付/银行卡 |

- 完整渠道对比 + 选型决策树 + FrpCode映射表 + 各方式额外参数 → [📄 支付渠道与交易类型对比.md](./references/1-产品选型/支付渠道与交易类型对比.md)

---

## 能力2：示例代码（模式B）

> 用户要某个接口的代码示例时 → 确认签名方式和开发语言，加载对应的代码文件。
>
> ‼️ **只检索、不生成。** 必须从代码示例文件中检索获取。
>
> ‼️ **只展示、不写入。** 代码示例仅用于讲解 API 调用结构和签名流程，严禁直接写入用户项目。在对话中展示代码，让用户自行复制适配。
>
> ‼️ **先交互、后输出。** 提供代码前必须先确认签名方式（MD5/RSA）、开发语言和具体接口，每次只输出一个接口；提供完代码后主动推荐接入质量评估。
>
> ‼️ **支付方式仅「下单」接口需确认，其他接口无需询问支付方式。** 查单、关单、退款、回调处理等通用接口只需确认签名方式和开发语言。

- 接口索引（四语言 × 7接口 映射）→ [📄 接口索引.md](./references/2-示例代码/接口索引.md)

### 加载策略

1. 先确认**签名方式**（MD5 / RSA）和**开发语言**（Java / Python / Go / PHP）
2. 读 `接口索引.md` 定位目标文件路径
3. 按需加载具体代码文件
4. 不要一次性加载所有文件

---

## 能力3：业务知识速查（模式B）

> 用户问参数获取、签名算法、订单状态、退款规则等业务知识时 → 加载对应文档。

- 开发必要参数（商户号/报备商户号/AppId/OpenId/买家ID）：
  - [📄 开发必要参数说明.md](./references/3-接入指南/开发必要参数说明.md)
- 签名算法详解（MD5 + RSA 双签名规则及四语言实现要点）：
  - [📄 签名与验签规则.md](./references/3-接入指南/签名与验签规则.md)
- 订单状态 / 关单 / 终态：
  - [📄 订单状态流转.md](./references/3-接入指南/订单状态流转.md)
- 回调通知处理规范（notify_url / 验签 / 幂等 / 重试）：
  - [📄 回调通知处理.md](./references/3-接入指南/回调通知处理.md)
- 退款规则 / 分账说明：
  - [📄 退款与分账规则.md](./references/3-接入指南/退款与分账规则.md)

> **加载策略**：按关键词匹配文档，一次只加载需要的文档。

---

## 能力4：接入质量评估

> 用户准备上线或想检查代码隐患时 → 加载以下文档。
>
> ‼️ **只检查用户实际使用的功能模块。** 未使用的功能不检查、不提及。

- 签名验签检查（MD5/RSA 双模式）：
  - [📄 签名与验签规则.md](./references/3-接入指南/签名与验签规则.md) — 第4节"常见错误"
- 业务逻辑完整性（含金融系统专家人设 + 检查清单）：
  - [📄 接入质量检查清单.md](./references/3-接入指南/接入质量检查清单.md)
- 回调处理规范性：
  - [📄 回调通知处理.md](./references/3-接入指南/回调通知处理.md)

---

## 能力5：问题排查

> 用户遇到报错或接口调用异常时 → 按下方分流加载。

- 响应码 TOP 20 速查 + 定位流程 + 分步排查：
  - [📄 排障手册.md](./references/4-问题排查/排障手册.md)
- 基础支付常见问题（签名失败/下单异常/回调/退款FAQ）：
  - [📄 基础支付常见问题.md](./references/4-问题排查/基础支付支付常见问题.md)
- 完整响应码列表（含退款专用响应码）：
  - [📄 响应码完整列表.md](./references/4-问题排查/响应码完整列表.md)
- 排障辅助脚本（排障手册中标注的场景）：`scripts/query_order.py` 和 `scripts/query_refund.py`

> **加载策略**：
>
> - **路径A（有响应码）**→ 读 `排障手册.md`，提取响应码匹配 TOP 20 速查表直接给出方案；未命中再按手册各章节排查，仍未解决再加载 `基础支付常见问题` 兜底。
> - **路径B（无响应码但有异常描述）**→ 加载 `基础支付常见问题` 匹配关键词。未命中再加载 `排障手册` 兜底。
> - **路径C（需查询订单状态）**→ 引导用户使用排障辅助脚本 `query_order.py` 或 `query_refund.py`。
>
> **脚本使用规范**：脚本采用签名模式，引导用户提供商户号、密钥等信息后传入脚本。执行前需按分步确认协议征得同意。

---

## 配置说明

配置文件位于 skill 根目录 `config.json`：

```json
{
  "default_merchant_no": "商户编号",
  "merchant_key": "32位商户密钥(MD5签名用)",
  "rsa_private_key": "RSA私钥(RSA签名用)",
  "rsa_public_key": "RSA公钥(验签用)",
  "base_url": "https://trade.joinpay.com",
  "default_notify_url": "http://your-server.com/notify",
  "default_trade_merchant_no": "报备商户号",
  "sign_type": "MD5"
}
```

> 在任何API调用之前，检查 config.json 是否已配置有效的商户密钥。若 `merchant_key` 以 "x" 开头或为空，提示用户先完成配置。

### 最小可用配置（快速测试）

只需配置以下3个字段即可跑通测试：
1. `default_merchant_no` — 商户号
2. `merchant_key` — MD5密钥（32位）
3. `base_url` — 接口地址（测试环境用 `https://trade.joinpay.cc`，生产环境用 `https://trade.joinpay.com`）
