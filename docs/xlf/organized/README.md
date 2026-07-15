# xlf 发票整理结果

原始文件未移动或改名；本目录为规整副本。文件名采用 `日期_类别_发票号码.扩展名`，完全重复件放入 `99_duplicates`。

## 分类统计

- `01_vat_general_pdf`：通用数电/增值税发票 PDF，非重复 6 份
- `02_didi_invoice_pdf`：滴滴电子发票 PDF，非重复 14 份
- `03_didi_trip_pdf`：滴滴顺风车行程单 PDF，非重复 3 份
- `04_huolala_invoice_pdf`：货拉拉电子发票 PDF，非重复 2 份
- `05_toll_ofd_xml`：通行费 OFD/XML，非重复 6 份
- `99_duplicates`：完全重复 4 份

## 识别方式

- PDF：优先使用内嵌文本提取，速度快、无需调用付费模型。
- XML/OFD：XML 结构化读取；同名前缀 OFD 使用对应 XML 信息归类。
- 未做全量图片 OCR；如需校验抬头、税号、明细，建议只对少量问题件调用本地 OCR。

## 索引文件

- `index.csv`：Excel/WPS 友好索引。
- `index.json`：程序读取索引。
