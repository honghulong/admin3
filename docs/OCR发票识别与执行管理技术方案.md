# OCR 发票识别与执行管理技术方案

## 1. 文档控制

| 项目 | 内容 |
|---|---|
| 文档名称 | OCR 发票识别与执行管理技术方案 |
| 文档性质 | OCR 发票识别相关需求、设计、技术选型、接口、验证和结论的唯一主文档 |
| 适用项目 | `admin3`，以及被 `admin3` 调用的 `D:\hongmengProjects\ocr-invoice-mcp` 本地 OCR 服务 |
| 当前版本 | V1.0 |
| 最新更新 | 2026-07-24 |
| 管理约定 | 除测试脚本、初始化 SQL 和代码内注释外，OCR 相关需求类、设计类、技术方案类、验证类文档统一维护在本文档中 |

## 2. 背景与目标

现有系统已有一套 OCR 发票识别服务，用于报销流程中的发票入库。近期需要确认 `D:\hongmengProjects\ocr-invoice-mcp` 服务是否因误改受到影响，同时扩展 `admin3` 的 OCR 能力，引入 Qwen、Kimi 等第三方大模型作为可配置识别引擎。

本轮目标如下：

- 验证本地 OCR 服务可用性，确认误改是否造成识别偏差。
- 调研多类型发票的字段结构和入库兼容性。
- 在 `admin3` 新增 OCR 执行管理模块，由管理员配置 OCR 引擎和提示词。
- 报销流程按当前启用配置自动路由到内部 OCR、Qwen、Kimi 或其他 OpenAI-compatible Provider。
- 统一大模型返回结果到系统内部 `invoice_fields` 标准结构。
- 建立需求、设计、实现、验证之间的一体化记录，符合小项目 CMMI 简化管理要求。

## 3. 范围与假设

### 3.1 范围内

- `admin3` OCR 执行管理模块。
- `ocr_engine_config` 配置表。
- 管理端 OCR 引擎配置页面、菜单和权限。
- 内部 OCR、Qwen、Kimi Provider。
- 图片、扫描件和双层 PDF 的路由策略。
- 发票字段标准化、字段别名归一和结果写回报销单。
- 本地 OCR 服务 `ocr-invoice-mcp` 的配置验证和回归测试记录。
- OCR 相关单元测试、集成冒烟和业务冒烟记录。

### 3.2 范围外

- 测试脚本源码不合并进本文档，仍按代码资产维护。
- 真实发票图片不进入文档和版本库，只记录脱敏后的字段、耗时和结论。
- 不在本轮强制引入 Spring AI 或 LangGraph4j；后续需要时再作为适配器替换。

## 4. 需求说明

### 4.1 本地 OCR 服务验证

验证对象是 `D:\hongmengProjects\ocr-invoice-mcp`，不是 `admin3` 内部调用代码。

要求：

- 检查当前 OCR 服务代码相较误改前的差异。
- 对现有 OCR 服务执行回归测试。
- 输出服务是否可用、是否存在识别偏差、是否需要修复。

### 4.2 多类型发票兼容性

需要覆盖以下票据类型：

- 火车票 / 高铁票。
- 餐饮发票。
- 住宿 / 酒店发票。
- 公交 / 地铁票。
- 出租车票、机票行程单、增值税普通/专用发票、网约车发票、通行费 OFD/XML 等扩展类型。

判断标准：

- 是否能正常入库报销。
- 是否缺少字段或格式不匹配。
- 是否需要扩展发票字段或转为待确认状态。

### 4.3 OCR 执行管理

管理员可配置：

- 引擎类型：内部 OCR、Qwen、Kimi、其他 OpenAI-compatible。
- API Key。
- Base URL。
- 模型名称。
- 调用协议。
- 提示词。
- 启用状态。
- 异常回退策略。

报销流程中 OCR 调用无需人工选择，应自动按当前启用配置路由。

## 5. 技术选型

| 维度 | 当前选择 | 原因 |
|---|---|---|
| 引擎抽象 | `OcrProvider` + `OcrExecutionService` | 改动小，能统一内部 OCR、Qwen、Kimi 和未来 OpenAI-compatible 引擎 |
| AI 框架 | 暂不强制引入 Spring AI | 当前仅需标准 HTTP OpenAI-compatible 调用，直接使用 Java `HttpClient` 更轻 |
| 配置存储 | `ocr_engine_config` 数据库表 | 支持多 Provider、多环境/租户字段、启用状态、回退策略和提示词 |
| API Key 安全 | AES-GCM 加密存储 | 查询接口不返回明文，仅返回 `apiKeyConfigured` |
| Qwen 接入 | `qwen3.5-ocr` | 已通过真实发票样本验证，速度优于 Kimi |
| Kimi 接入 | `kimi-k2.6` | 官方 K2.6 Vision 验证可用；`kimi-k3` 体验金不可用 |
| PDF 策略 | 双层 PDF 优先结构化文本 | 降低成本和延迟，避免可文本提取 PDF 走大模型 |
| 超时策略 | 外部模型 45 秒 | 避免页面长时间等待；比原 120 秒更符合业务体验 |

当前推荐：Qwen 作为默认使用引擎；Kimi 作为可选引擎继续观察准确率、耗时和成本。

## 6. 系统设计

### 6.1 模块位置

- 后端配置接口：`admin3-server/src/main/java/tech/wetech/admin3/controller/OcrEngineConfigController.java`
- 配置模型：`admin3-server/src/main/java/tech/wetech/admin3/sys/model/OcrEngineConfig.java`
- 配置仓储：`admin3-server/src/main/java/tech/wetech/admin3/sys/repository/OcrEngineConfigRepository.java`
- Provider 抽象和实现：`admin3-server/src/main/java/tech/wetech/admin3/sys/service/ocr/`
- 报销 OCR 主流程：`admin3-server/src/main/java/tech/wetech/admin3/sys/service/OcrService.java`
- 管理端页面：`admin3-ui/src/views/ocr-engine-config-list.vue`
- 管理端 API：`admin3-ui/src/api/ocr-engine-config.ts`
- 管理端路由：`admin3-ui/src/router/index.ts`
- 初始化脚本：`admin3-server/src/main/resources/V008__ocr_engine_management.sql`

### 6.2 路由流程

1. 报销附件上传后进入 `OcrService.recognizeAttachment`。
2. 双层 PDF 优先提取 PDF 内嵌文本，能识别有效金额时不调用 Qwen/Kimi。
3. 图片、JPG、PNG、扫描件进入 `OcrExecutionService.recognizeBase64`。
4. `OcrExecutionService` 读取当前启用的 `OcrEngineConfig`。
5. 根据 `providerType` 路由到对应 `OcrProvider`。
6. Provider 返回统一 JSON，包括 `status`、`source_type`、`ocr_text`、`invoice_fields`。
7. `OcrService` 解析标准字段并写回报销单和发票字段。
8. 外部大模型失败且 `fallbackEnabled=true` 时回退到内部 OCR。

当前执行策略已简化为“启用哪个引擎就调用哪个引擎”；启用一个配置时关闭其他配置。

### 6.3 后端接口

基础路径：`/ocr-engine-configs`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ocr-engine-configs` | 查询配置列表，不返回 API Key 明文 |
| POST | `/ocr-engine-configs` | 新增配置 |
| PUT | `/ocr-engine-configs/{id}` | 更新配置；`apiKey` 为空时保留原密文 |
| DELETE | `/ocr-engine-configs/{id}` | 删除配置 |
| POST | `/ocr-engine-configs/{id}:markAsDefault` | 兼容旧命名，实际语义为启用该引擎 |

请求示例：

```json
{
  "name": "Qwen OCR",
  "providerType": "QWEN",
  "baseUrl": "https://llm-4t53i4opya4nag5m.cn-beijing.maas.aliyuncs.com/compatible-mode/v1",
  "modelName": "qwen3.5-ocr",
  "apiKey": "sk-xxx",
  "protocol": "OPENAI_COMPATIBLE",
  "tenantId": "default",
  "environment": "local",
  "prompt": "请识别图片中的中国发票，并只返回一个 JSON 对象。",
  "enabled": true,
  "isDefault": true,
  "fallbackEnabled": false
}
```

统一返回示例：

```json
{
  "status": "ok",
  "source_type": "QWEN",
  "ocr_text": "...",
  "invoice_fields": {
    "invoice_no": "72551412",
    "total_amount": "1125.14"
  }
}
```

### 6.4 安全设计

- API Key 存储在 `ocr_engine_config.api_key_cipher_text`。
- 加密方式为 AES-GCM。
- 生产环境应设置 `ADMIN3_OCR_SECRET_KEY`。
- 查询接口只返回 `apiKeyConfigured=true/false`。
- 不在日志中打印 API Key。
- 真实发票样本不外发测试，除非用户明确授权；模型可用性测试优先使用合成无敏感图片。

### 6.5 管理端与权限

- 菜单入口：“系统管理 / OCR 执行管理”。
- 权限资源：`ocr-engine:view`、`ocr-engine:create`、`ocr-engine:update`、`ocr-engine:delete`。
- 仅管理员角色分配该菜单和动作权限。
- 初始化脚本负责创建菜单、按钮权限和管理员角色授权。

## 7. 字段标准

### 7.1 标准字段

内部标准结构保持 `invoice_fields`，核心字段包括：

- `invoice_type`
- `invoice_no`
- `invoice_code`
- `date`
- `buyer_name`
- `buyer_tax_id`
- `buyer_address_phone`
- `buyer_bank_info`
- `seller_name`
- `seller_tax_id`
- `seller_address_phone`
- `seller_bank_info`
- `total_amount`
- `amount_before_tax`
- `tax_amount`
- `tax_rate`
- `check_code`
- `machine_no`
- `payee`
- `reviewer`
- `issuer`
- `remarks`
- `start_place`
- `end_place`

交通扩展字段建议：

- `ticket_no`
- `train_no`
- `flight_no`
- `passenger_name`
- `departure_time`
- `arrival_time`
- `seat_class`

### 7.2 字段别名归一

当前已覆盖的别名包括：

- `invoice_number`、`invoice_num` -> `invoice_no`
- `invoice_date` -> `date`
- `drawer` -> `issuer`
- `amount`、`total_tax_included_amount` -> `total_amount`
- `tax_excluded_amount` -> `amount_before_tax`
- `buyer_address`、`buyer_phone`、`buyer_address_and_phone`、`buyer_address_tel` -> `buyer_address_phone`
- `buyer_bank`、`buyer_account`、`buyer_bank_account`、`buyer_bank_and_account` -> `buyer_bank_info`
- `seller_address`、`seller_phone`、`seller_address_and_phone`、`seller_address_tel` -> `seller_address_phone`
- `seller_bank`、`seller_account`、`seller_bank_account`、`seller_bank_and_account` -> `seller_bank_info`

### 7.3 金额清洗

已知问题：`test-invoice2.jpg` 经 Kimi 识别后写回 `amount=楼101.10`。

后续规则：

- `total_amount`、`tax_amount`、`amount_before_tax` 必须统一清洗。
- 可从 `楼101.10` 提取为 `101.10`。
- 非数字、非小数点、非负号字符应过滤。
- 原始 OCR 文本必须保留在 `ocr_text` 或原始结果中，便于追溯。

## 8. 发票类型兼容性

### 8.1 已验证样本

| 类型 | 样本 | 结果 | 兼容性 |
|---|---|---|---|
| 增值税电子普通发票 | `D:\hongmengProjects\ocr-invoice-mcp\invoice.png` | 字段数 19，黄金样例校验通过 | 可直接入库和报销 |
| 网约车/滴滴发票 | `D:\hongmengProjects\ocr-invoice-mcp\tests\fixtures\didi_invoice_pdf\2026-02-26_didi-invoice_26117000000328150997.pdf` | 字段数 9，黄金样例校验通过 | 发票可入库，行程字段需补充 |

### 8.2 类型结构

| 类型 | 关键字段 | 版式特征 | 兼容性 |
|---|---|---|---|
| 增值税普通/专用发票 | 发票号、代码、日期、购销方税号、价税合计 | 标准票面，字段稳定 | 可直接兼容 |
| 餐饮发票 | 发票号、日期、销售方、金额、税额 | 多为 VAT 发票，商品明细体现餐饮 | 可兼容，费用类型需按销售方/明细归类 |
| 住宿/酒店发票 | 发票号、日期、销售方、金额、税额、备注 | 常见 VAT 票，备注可能含住宿天数 | 可兼容，住宿业务字段需扩展 |
| 网约车/出租车 | 发票号、日期、销售方、金额、行程信息 | 发票与行程单常分离 | 发票可入库，行程字段需补充 |
| 火车票/高铁票 | 票号、车次、出发站、到达站、乘车日期、票价 | 非传统 VAT 版式，强行程属性 | 需扩展标准字段后兼容 |
| 公交/地铁票 | 日期、线路/站点、金额 | 字段少，票面小且无购销方税号 | 无法完整映射现有发票表，建议待确认 |
| 机票行程单 | 票号、旅客、航班、航段、票价、税费 | 交通行程单结构 | 需扩展行程字段 |
| 通行费 OFD/XML | 发票号、日期、入口/出口或站点、金额 | 结构化文件优先 | 可兼容，地点字段需扩展 |

## 9. 本地 OCR 服务验证

### 9.1 差异检查

- 核心识别入口 `src/ocr_server.py` 原本无未提交算法差异。
- `requirements.txt` 曾将 `paddlex[ocr]` 从可选依赖改为强制安装，导致 `health` 和 `/ocr` 初始化 KIE/Table 可选模型。
- `scripts/install_service.bat`、`scripts/run_server.bat` 已改为读取 `config/service.json`，但服务端原先未完整消费配置。

### 9.2 修复内容

- `paddlex[ocr]` 恢复为可选依赖。
- `config/service.json` 设置 `enable_kie=false`、`enable_table=false`。
- `src/ocr_server.py` 读取 `config/service.json`，默认关闭 KIE/Table，仅保留 PaddleOCR + 正则字段提取。
- `/ocr` 增加 `max_concurrent` 限流。
- 安装脚本重装并启动 `OcrInvoiceSvc`。

### 9.3 验证结果

`GET http://127.0.0.1:8765/health`：

- `paddleocr=true`
- `kie_engine=false`
- `table_engine=false`

`python tests\test_api.py --ocr invoice.png --runs 1`：

- 耗时：8.0 秒。
- 文本块：51。
- 字段数：19。
- 黄金样例字段校验通过。

`python tests\test_api.py --ocr tests\fixtures\didi_invoice_pdf\2026-02-26_didi-invoice_26117000000328150997.pdf --runs 1`：

- 耗时：5.7 秒。
- 文本块：55。
- 字段数：9。
- 黄金样例字段校验通过。

结论：本地 OCR 服务可用。误改主要影响是启用了不需要的 PaddleX KIE/Table 能力，造成识别耗时显著变长甚至超时；关闭后未发现黄金样例字段偏差。

## 10. 大模型实测记录

### 10.1 外部模型可用性

| 测试对象 | 输入 | 结果 | 耗时 | 结论 |
|---|---|---|---|---|
| Kimi `kimi-k2.6` | 本地合成无敏感测试图，base64 data URL | HTTP 200，正确识别 `TEST-20260724`、`12.34`、`2026-07-24` | 约 13.5 秒 | 模型、账号权限和协议可用 |
| Kimi `kimi-k3` | `test-invoice.png` | HTTP 404，模型不可用或无权限 | 约 14 秒后失败并回退 | 不适合作为当前默认模型 |
| Kimi `moonshot-v1-8k-vision-preview` | `test-invoice.png` | HTTP 404，模型不可用或无权限 | 约 14 秒后失败 | 不适用 |
| Kimi 错误 API Key | 图片样本 | HTTP 401 | 约 1-31 秒 | 认证失败，应提示管理员检查 Key |
| Qwen `qwen3.5-ocr` | `test-invoice.png` | 成功识别并写回报销单 | 约 16-18 秒 | 当前发票样本上速度优于 Kimi |

### 10.2 报销单 `testqianwen` 运行记录

| 附件 | 文件 | Provider | 结果 | OCR 耗时 | 写回结果 |
|---|---|---|---|---|---|
| 202 | `test-invoice.png` | Qwen | 成功 | 约 18.41 秒 | `invoiceNo=72551412, amount=1125.14` |
| 209 | `test-invoice.png` | Kimi | 成功 | 约 25.47 秒 | `invoiceNo=72551412, amount=1125.14` |
| 210 | `test-invoice2.jpg` | Kimi | 成功 | 约 21.51 秒 | `invoiceNo=97386849, amount=楼101.10` |

## 11. 测试管理

### 11.1 测试资产原则

测试脚本、测试数据构造和自动化用例按代码资产分开维护，不合并进本文档。本文档只维护测试目标、测试用例关系、执行命令、执行结果和结论。

### 11.2 测试用例关系

| 测试层级 | 用例/命令 | 覆盖目标 |
|---|---|---|
| 单元测试 | `OpenAiCompatibleOcrProviderTest.normalizeResponseExtractsJsonMessageContent` | 大模型返回 Markdown JSON 时能解析为内部 `invoice_fields` |
| 单元测试 | `OpenAiCompatibleOcrProviderTest.normalizeResponseMapsProviderAliasesAndNullText` | 字段别名映射、`null` 文本归一为空 |
| 单元测试 | `OpenAiCompatibleOcrProviderTest.kimiRequestDisablesThinkingAndUsesShortTimeout` | Kimi 请求体包含 `thinking.disabled`，外部调用超时为 45 秒，图片使用 base64 data URL |
| 单元测试 | `OcrSecretCipherTest` | API Key 加密/解密可用，不明文存储 |
| 单元测试 | `OcrServiceTest` | PDF/图片 OCR 字段提取、金额识别、票据类型和费用分类规则 |
| 集成冒烟 | Kimi 合成图片最小请求 | 验证 `kimi-k2.6` 模型、base URL、API Key 和 OpenAI-compatible 协议真实可用 |
| 业务冒烟 | 报销单 `testqianwen` 上传 `test-invoice.png` | 验证管理端配置路由到 Qwen/Kimi，并写回报销单字段 |
| 业务冒烟 | 报销单 `testqianwen` 上传 `test-invoice2.jpg` | 验证小图 JPG 走 Kimi 成功，暴露金额清洗问题 |

### 11.3 已执行验证

OCR 相关测试：

```powershell
.\mvnw.cmd -pl admin3-server "-Dtest=OcrServiceTest,OcrSecretCipherTest,OpenAiCompatibleOcrProviderTest" test
```

结果：13 个测试通过，0 失败。

构建与启动：

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

结果：构建成功；服务已启动；`http://127.0.0.1:9099/admin3` 返回 302。

## 12. 当前结论与待办

### 12.1 当前结论

- 本地 OCR 服务可用，且不需要启用 KIE/Table。
- `admin3` OCR 执行管理模块已具备管理员配置、菜单权限、Provider 路由、API Key 加密和提示词管理能力。
- Qwen `qwen3.5-ocr` 在当前样本上速度和写回效果较好，建议作为默认使用引擎。
- Kimi `kimi-k2.6` 已确认可用，但耗时较长，且金额字段需要清洗。
- 双层 PDF 不应走 Qwen/Kimi，当前策略正确。

### 12.2 后续待办

1. 对 `total_amount`、`tax_amount`、`amount_before_tax` 增加统一金额清洗。
2. 外部大模型调用前增加图片压缩/缩放策略。
3. 管理端增加“测试连接/测试识别”按钮，使用合成图片或脱敏样本验证配置。
4. 建立真实发票脱敏回归集，记录期望字段、Provider、耗时和可入库结论。
5. 若后续 OpenAI-compatible Provider 继续增加，再评估是否引入 Spring AI 统一客户端能力。
