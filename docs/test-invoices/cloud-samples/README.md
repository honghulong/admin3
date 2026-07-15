# 云服务/公开发票测试素材

这些文件来自公开文档或公开税务资料，仅用于开发测试。不要把真实个人或公司发票直接提交到仓库。

## 文件说明

- `tencent-cloud-invoice-preview.png`：腾讯云公开文档中的发票预览截图，适合测试发票 OCR、发票列表展示、删除、关联关系。
- `tencent-cloud-invoice-list.png`：腾讯云公开文档中的发票列表截图，更适合测试附件上传/非标准票据识别失败或弱识别场景。
- `aliyun-invoice-records.png`：阿里云公开文档中的发票记录截图，更适合测试非票面截图处理。
- `china-tax-electronic-invoice-template.pdf`：公开电子发票版式资料，适合测试 PDF 上传/预览，不建议作为结构化 OCR 的唯一基准。

## 推荐回归组合

1. 用 `../01-meal-20260115-a.png`、`../02-meal-20260115-b.png`、`../03-fuel-20260115.png`、`../04-toll-20260115.png`、`../05-meal-20260116.png` 测同日同类合并、不同类型/日期拆分。
2. 用 `tencent-cloud-invoice-preview.png` 测真实云服务发票截图识别。
3. 用 `tencent-cloud-invoice-list.png`、`aliyun-invoice-records.png` 测非标准截图不会误生成多张正式发票。
4. 用 `china-tax-electronic-invoice-template.pdf` 测 PDF 上传、预览和失败/弱识别路径。

## 增删查改与导出检查

- 增：新建报销并上传上述素材。
- 查：刷新详情，检查发票数量、发票合计、费用明细金额。
- 改：编辑发票金额/费用明细关联，检查上级金额实时变化。
- 删：删除发票或费用明细，检查有效发票数、发票合计、报销金额实时变化，保存后刷新不恢复。
- 导出：下载电子发票登记表、报销费用明细表、批量 OA zip。
