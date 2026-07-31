# OCR Evaluation: test_page2 (1)_server_v1

- input: `test_page2 (1).pdf`
- model: `PP-OCRv5_server_det` + `PP-OCRv5_server_rec`
- dpi: `220`
- total_duration_ms: `3759`

| Page | Grade | Lines | Rows | Avg Conf | Low Conf | Tall Boxes | Warnings | Box Image |
|---:|---|---:|---:|---:|---:|---:|---|---|
| 1 | A | 49 | 16 | 0.9825 | 1 | 3 | dense_row_maybe_table_or_bad_line_merge | page_001_boxes.png |

## Sorted Text Preview

### Page 1
```text
电子发 统 发普 通发票） 发票号码：26117000000530671207
税 旅客运输服务
国家税务总局
开票日期：2026年04月08日
北京市税务局
购买方信息 销售方信息
名称：广东维信智联科技有限公司 名称：北京滴滴出行科技有限公司
统一社会信用代码/纳税人识别号：91441900762909683X 统一社会信用代码/纳税人识别号：91110108MA01G0FB09
项目名称 单价 数量 金额 税率/征收率 税额
*运输服务*客运服务费 17.09 1 17.09 3% 0.51
*运输服务*客运服务费 -1.84 3% -0.06
合 计 ￥15.25 ￥0.45
出行人 有效身份证件号 出行日期 出发地 到达地 等级 交通工具类型
价税合计 （大写） 壹拾伍圆柒角整 （小写）￥15.70
备注
开票人： 于秋红
```
