# OCR raw bbox region extraction test

This report parses `ocr_raw.bbox` from the local OCR service directly. It ignores OCR `invoice_fields` and uses approximate invoice regions: buyer block, seller block, item table, summary row, issuer block.

## Summary

| File | Region score | OCR fields score | PDF text+code score | What bbox improves |
|---|---:|---:|---:|---|
| 2026-07-22 广州-北京-204534158513-机票电子发票-1984.00.pdf | 10/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake; fills amount_before_tax; fills tax_amount; fills issuer |
| ESAE178467060404888838y1n94wADiy3v3高速票.pdf | 10/10 | 7/10 | 10/10 | fills amount_before_tax; fills tax_amount; fills issuer |
| dzfp_26112000003078497746_20260727105946王府井酒店2晚.pdf | 10/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake; fills amount_before_tax; fills tax_amount; fills issuer |
| 上海三快智送科技有限公司_发票金额5.00元.pdf | 10/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake; fills amount_before_tax; fills tax_amount; fills issuer |
| 北京京寻餐饮有限公司_发票金额174.50元.pdf | 10/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake; fills amount_before_tax; fills tax_amount; fills issuer |
| 携程用车行程单.pdf | 0/10 | 0/10 | 1/10 | no clear gain or not invoice layout |
| 滴滴电子发票 (19).pdf | 6/10 | 7/10 | 8/10 | no clear gain or not invoice layout |
| 滴滴电子发票 (20).pdf | 5/10 | 5/10 | 8/10 | no clear gain or not invoice layout |
| 滴滴顺风车行程单 (1).pdf | 0/10 | 0/10 | 1/10 | no clear gain or not invoice layout |
| 滴滴顺风车行程单.pdf | 0/10 | 0/10 | 1/10 | no clear gain or not invoice layout |
| 用车电子发票 (1).pdf | 8/10 | 6/10 | 10/10 | fills amount_before_tax; fills tax_amount |
| 用车电子发票.pdf | 8/10 | 6/10 | 10/10 | fills amount_before_tax; fills tax_amount |
| 用车行程单.pdf | 1/10 | 0/10 | 1/10 | fills tax_amount |
| 订单1128149704378709-电子普通发票(1).pdf | 10/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake; fills amount_before_tax; fills tax_amount; fills issuer |
| 订单1128149704378709-电子普通发票(2).pdf | 10/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake; fills amount_before_tax; fills tax_amount; fills issuer |
| 订单1128149704378709-电子普通发票-保险(1).pdf | 8/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake; fills issuer |
| 订单1128149704378709-电子普通发票-保险(2).pdf | 7/10 | 7/10 | 10/10 | buyer_name avoids table-header mistake |
| 货拉拉电子发票 (4).pdf | 8/10 | 6/10 | 10/10 | fills issuer |
| 货拉拉行程报销单 (3).pdf | 1/10 | 1/10 | 2/10 | no clear gain or not invoice layout |

## Aggregate observations

- buyer_name table-header mistakes avoided by region rule: 8.
- amount/tax complements from summary region: 19.
- issuer complements from issuer region: 9.
- Region method is meaningful for standard electronic invoice layouts. It is weak for trip sheets / reimbursement forms because they do not share the invoice template regions.

## 2026-07-22 广州-北京-204534158513-机票电子发票-1984.00.pdf

- Region score: 10/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26127000000363374667 | 26127000000363374667 | 26127000000363374667 |
| `date` | 2026年07月22日 | 2026年07月22日 | 2026年07月22日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 天津航路无界国际旅行社有限公司 | 天津航路无界国际旅行社有限公司 | 天津航路无界国际旅行社有限公司 |
| `seller_tax_id` | 91120222MAETKCJ87K | 91120222MAETKCJ87K | 91120222MAETKCJ87K |
| `total_amount` | 1984.00 | 1984.00 | 1984.00 |
| `amount_before_tax` | 1871.70 |  | 1871.70 |
| `tax_amount` | 112.30 |  | 112.30 |
| `issuer` | 周晶 |  | 周晶 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号:91441900762909683X`
- seller: `名称：天津航路无界国际旅行社有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91120222MAETKCJ87K`
- table: `项目名称 | 单位 | 规格型号 | 数量 | 单价 | 税率/征收率 | 金额 | 税额 | 生产生活服务*代订机票 | 次 | 11781.132075471698 | 6% | 1781.13 | 106.8 | 套餐产品 | 次 | 生产生活服务*经纪代理 | 190.5660377358491 | 90.57 | 6% | 5.4 | 服务费`
- summary: `计 | 合 | ￥1871.70 | ￥112.30 | 价税合计（大写) | 壹仟玖佰捌拾肆圆整 | （小写）￥1984.00`
- issuer: `开票人：周晶`

## ESAE178467060404888838y1n94wADiy3v3高速票.pdf

- Region score: 10/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26447000001447993351 | 26447000001447993351 | 26447000001447993351 |
| `date` | 2026年07月22日 | 2026年07月22日 | 2026年07月22日 |
| `buyer_name` | 广东维信智联科技有限公司 | 广东维信智联科技有限公司 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 广东联合电子服务股份有限公司 | 广东联合电子服务股份有限公司 | 广东联合电子服务股份有限公司 |
| `seller_tax_id` | 914400007408467653 | 914400007408467653 | 914400007408467653 |
| `total_amount` | 40.00 | 40.00 | 40.00 |
| `amount_before_tax` | 40.00 |  | 40.00 |
| `tax_amount` | 0.00 |  | 0.00 |
| `issuer` | 邓泽辉 |  | 邓泽辉 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：广东联合电子服务股份有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：914400007408467653`
- table: `项目名称 | 单位 | 规格型号 | 数量 | 单价 | 金额 | 税率/征收率 | 税额 | 生产生活服务*代收通行*** | *** | 1 | 40.0 | 40.00 | 不征税 | 费`
- summary: `计 | A | ￥40.00 | ￥0.00 | 肆拾圆整 | 价税合计（大写） | （小写）￥40.00`
- issuer: `开票人：邓泽辉`

## dzfp_26112000003078497746_20260727105946王府井酒店2晚.pdf

- Region score: 10/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26112000003078497746 | 26112000003078497746 | 26112000003078497746 |
| `date` | 2026年07月24日 | 2026年07月24日 | 2026年07月24日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 北京王府井永光宾馆管理有限公司王府井分公司 | 北京王府井永光宾馆管理有限公司王府井分公司 | 北京王府井永光宾馆管理有限公司王府井分公司 |
| `seller_tax_id` | 91110101633604904T | 91110101633604904T | 91110101633604904T |
| `total_amount` | 804.00 | 804.00 | 804.00 |
| `amount_before_tax` | 758.49 |  | 758.49 |
| `tax_amount` | 45.5 |  | 45.51 |
| `issuer` | 李靖 |  | 李靖 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：北京王府井永光宾馆管理有限公司王府井分公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91110101633604904T`
- table: `单位 | 项目名称 | 规格型号 | 数量 | 单价 | 税率/征收率 | 金额 | 税额 | 生产生活服务*住宿费 | 天 | 2379.2452830188679 | 758.49 | 6% | 45.5`
- summary: `计 | 合 | ￥758.49 | ￥45.5 | )捌佰零肆圆整 | 价税合计（大写) | （小写）￥804.00`
- issuer: `开票人：李靖`

## 上海三快智送科技有限公司_发票金额5.00元.pdf

- Region score: 10/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26317000002597895258 | 26317000002597895258 | 26317000002597895258 |
| `date` | 2026年07月27日 | 2026年07月27日 | 2026年07月27日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 上海三快智送科技有限公司 | 上海三快智送科技有限公司 | 上海三快智送科技有限公司 |
| `seller_tax_id` | 91310000MA1FW9A80N | 91310000MA1FW9A80N | 91310000MA1FW9A80N |
| `total_amount` | 5.00 | 5.00 | 5.00 |
| `amount_before_tax` | 4.72 |  | 4.72 |
| `tax_amount` | 0.28 |  | 0.28 |
| `issuer` | 何娟 |  | 何娟 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：上海三快智送科技有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91310000MA1FW9A80N`
- table: `项目名称 | 单位 | 规格型号 | 数量 | 单价 | 税率/征收率 | 金额 | 税额 | 生产生活服务*配送服务 | 1 | 4.72 | 4.72 | 6% | 0.28 | 费`
- summary: `计 | 合 | ￥4.72 | ￥0.28 | 伍圆整 | 价税合计（大写） | ￥5.00 | （小写)`
- issuer: `开票人：何娟`

## 北京京寻餐饮有限公司_发票金额174.50元.pdf

- Region score: 10/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26112000003059680516 | 26112000003059680516 | 26112000003059680516 |
| `date` | 2026年07月23日 | 2026年07月23日 | 2026年07月23日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 北京京寻餐饮有限公司 | 北京京寻餐饮有限公司 | 北京京寻餐饮有限公司 |
| `seller_tax_id` | 91110101MA02A3E27L | 91110101MA02A3E27L | 91110101MA02A3E27L |
| `total_amount` | 174.50 | 174.50 | 174.50 |
| `amount_before_tax` | 172.77 |  | 172.77 |
| `tax_amount` | 1.7 |  | 1.73 |
| `issuer` | 李素华 |  | 李素华 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：北京京寻餐饮有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91110101MA02A3E27L`
- table: `项目名称 | 单位 | 规格型号 | 数量 | 单价 | 金额 | 税率/征收率 | 税额 | 生产生活服务*餐饮服务 | 1 | 172.77 | 172.77 | 1% | 1.7`
- summary: `计 | 合 | ￥172.77 | ￥1.7 | 壹佰柒拾肆圆伍角整 | 价税合计（大写） | （小写）￥174.50`
- issuer: `开票人：李素华`

## 携程用车行程单.pdf

- Region score: 0/10; OCR invoice_fields score: 0/10; PDF text+code score: 1/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` |  |  |  |
| `date` |  |  | 2026-07-25 |
| `buyer_name` |  |  |  |
| `buyer_tax_id` |  |  |  |
| `seller_name` |  |  |  |
| `seller_tax_id` |  |  |  |
| `total_amount` |  |  |  |
| `amount_before_tax` |  |  |  |
| `tax_amount` |  |  |  |
| `issuer` |  |  |  |

Region evidence:
- buyer: `预订信息 | 订单号：`
- seller: ``
- table: `下单日期： | 2026-07-24 | 行程信息`
- summary: `上车点： | 白云国际机场 T3 | 下车点： | 汇峰中心-D区`
- issuer: `用车时间： | 2026-07-25 00:01:00 | 车辆信息 | 车牌号码： | 粵ADZ2673 | 费用明细 | 订单费用： | CNY 232 | 优惠： | -CNY 50 | 实际费用： | CNY 182`

## 滴滴电子发票 (19).pdf

- Region score: 6/10; OCR invoice_fields score: 7/10; PDF text+code score: 8/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26117000000531132508 | 26117000000531132508 | 26117000000531132508 |
| `date` | 2026年04月08日 | 2026年04月08日 | 2026年04月08日 |
| `buyer_name` | 广东维信智联科技有限公司 | 广东维信智联科技有限公司 | 名称：广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 北京滴滴出行科技有限公司 | 北京滴滴出行科技有限公司 | 名称：北京滴滴出行科技有限公司 |
| `seller_tax_id` | 91110108MA01G0FB09 | 91110108MA01G0FB09 | 91110108MA01G0FB09 |
| `total_amount` |  | 17.90 | 17.90 |
| `amount_before_tax` |  |  |  |
| `tax_amount` |  |  |  |
| `issuer` |  |  | 于秋红 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：北京滴滴出行科技有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91110108MA01G0FB09`
- table: `项目名称 | 单价 | 数量 | 税率/征收率 | 税额 | 金额 | *运输服务*客运服务费 | 18.54 | 1 | 18.54 | 3% | 0.56 | *运输服务*客运服务费 | -1.17 | 3% | -0.03 | 计 | 合`
- summary: `价税合计（大写） | 壹拾柒圆玖角整 | （小写）`
- issuer: `于秋红 | 开票人：`

## 滴滴电子发票 (20).pdf

- Region score: 5/10; OCR invoice_fields score: 5/10; PDF text+code score: 8/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26117000000530671207 | 26117000000530671207 | 26117000000530671207 |
| `date` |  |  | 2026年04月08日 |
| `buyer_name` | 广东维信智联科技有限公司 |  | 名称：广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 北京滴滴出行科技有限公司 | 广东维信智联科技有限公司 | 名称：北京滴滴出行科技有限公司 |
| `seller_tax_id` | 91110108MA01G0FB09 | 91110108MA01G0FB09 | 91110108MA01G0FB09 |
| `total_amount` |  | 15.70 | 15.70 |
| `amount_before_tax` |  |  |  |
| `tax_amount` |  |  |  |
| `issuer` |  |  | 于秋红 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：北京滴滴出行科技有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91110108MA01G0FB09`
- table: `项目名称 | 单价 | 数量 | 税率/征收率 | 税额 | 金额 | *运输服务*客运服务费 | 17.09 | 1 | 17.09 | 3% | 0.51 | *运输服务*客运服务费 | 3% | -1.84 | -0.06 | 计 | 合`
- summary: `价税合计（大写） | 壹拾伍圆柒角整 | （小写）`
- issuer: ` | 于秋红 | 开票人：`

## 滴滴顺风车行程单 (1).pdf

- Region score: 0/10; OCR invoice_fields score: 0/10; PDF text+code score: 1/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` |  |  |  |
| `date` |  |  | 2026-05-07 |
| `buyer_name` |  |  |  |
| `buyer_tax_id` |  |  |  |
| `seller_name` |  |  |  |
| `seller_tax_id` |  |  |  |
| `total_amount` |  |  |  |
| `amount_before_tax` |  |  |  |
| `tax_amount` |  |  |  |
| `issuer` |  |  |  |

Region evidence:
- buyer: `·申请日期：2026-05-07 | ·行程人手机号：13669813770`
- seller: `滴滴顺风车行程单 | DIDI HITCH - TRIP TABLE | ·行程起止日期：2026-04-29至2026-04-29 | ·共1笔行程，合计57.00元`
- table: `序号车型 | 城市 | 出发时间 | 到达时间 | 起点 | 终点 | 里程[公里] | 备注 | 金额[元] | 1 | 独享 | 04-2914:32周三 | 东莞市 | 04-2915:38周三 | 湘味居(花园大道店) | 汇峰中心D区 | 48.71 | 57.00`
- summary: ``
- issuer: `页码：1/1`

## 滴滴顺风车行程单.pdf

- Region score: 0/10; OCR invoice_fields score: 0/10; PDF text+code score: 1/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` |  |  |  |
| `date` |  |  | 2026-05-07 |
| `buyer_name` |  |  |  |
| `buyer_tax_id` |  |  |  |
| `seller_name` |  |  |  |
| `seller_tax_id` |  |  |  |
| `total_amount` |  |  |  |
| `amount_before_tax` |  |  |  |
| `tax_amount` |  |  |  |
| `issuer` |  |  |  |

Region evidence:
- buyer: `·申请日期：2026-05-07 | ·行程人手机号：13669813770`
- seller: `滴滴顺风车行程单 | DIDI HITCH - TRIP TABLE | ·行程起止日期：2026-04-29至2026-04-29 | ·共1笔行程，合计57.00元`
- table: `序号车型 | 城市 | 出发时间 | 到达时间 | 起点 | 终点 | 里程[公里] | 备注 | 金额[元] | 1 | 独享 | 04-2914:32周三 | 东莞市 | 04-2915:38周三 | 湘味居(花园大道店) | 汇峰中心D区 | 48.71 | 57.00`
- summary: ``
- issuer: `页码：1/1`

## 用车电子发票 (1).pdf

- Region score: 8/10; OCR invoice_fields score: 6/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26112000003031372846 | 26112000003031372846 | 26112000003031372846 |
| `date` | 2026年07月22日 | 2026年07月22日 | 2026年07月22日 |
| `buyer_name` | 广东维信智联科技有限公司 |  | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 北京爱特博旅运服务有限责任公司 | 广东维信智联科技有限公司 | 北京爱特博旅运服务有限责任公司 |
| `seller_tax_id` | 9111010263378065XE | 9111010263378065XE | 9111010263378065XE |
| `total_amount` |  | 53.00 | 53.00 |
| `amount_before_tax` | 51.46 |  | 51.46 |
| `tax_amount` | 1.54 |  | 1.54 |
| `issuer` |  |  | 黄俊华 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：北京爱特博旅运服务有限责任公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：9111010263378065XE`
- table: `单价 | 数量 | 金额 | 项目名称 | 税率/征收率 | 税额 | 51.4563106796117 | *交通运输服务*客运服务费 | 1 | 51.46 | 3% | 1.54`
- summary: `￥51.46 | ￥1.54 | 计 | 合 | 有效身份证件号 | 出行日期 | 出行人 | 出发地 | 到达地 | 等级 | 交通工具类型`
- issuer: `伍拾叁圆整 | 价税合计（大写） | （小写）￥53.00 | 备注`

## 用车电子发票.pdf

- Region score: 8/10; OCR invoice_fields score: 6/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26112000003031372846 | 26112000003031372846 | 26112000003031372846 |
| `date` | 2026年07月22日 | 2026年07月22日 | 2026年07月22日 |
| `buyer_name` | 广东维信智联科技有限公司 |  | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 北京爱特博旅运服务有限责任公司 | 广东维信智联科技有限公司 | 北京爱特博旅运服务有限责任公司 |
| `seller_tax_id` | 9111010263378065XE | 9111010263378065XE | 9111010263378065XE |
| `total_amount` |  | 53.00 | 53.00 |
| `amount_before_tax` | 51.46 |  | 51.46 |
| `tax_amount` | 1.54 |  | 1.54 |
| `issuer` |  |  | 黄俊华 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：北京爱特博旅运服务有限责任公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：9111010263378065XE`
- table: `单价 | 数量 | 金额 | 项目名称 | 税率/征收率 | 税额 | 51.4563106796117 | *交通运输服务*客运服务费 | 1 | 51.46 | 3% | 1.54`
- summary: `￥51.46 | ￥1.54 | 计 | 合 | 有效身份证件号 | 出行日期 | 出行人 | 出发地 | 到达地 | 等级 | 交通工具类型`
- issuer: `伍拾叁圆整 | 价税合计（大写） | （小写）￥53.00 | 备注`

## 用车行程单.pdf

- Region score: 1/10; OCR invoice_fields score: 0/10; PDF text+code score: 1/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` |  |  |  |
| `date` |  |  | 2026-07-22 |
| `buyer_name` |  |  |  |
| `buyer_tax_id` |  |  |  |
| `seller_name` |  |  |  |
| `seller_tax_id` |  |  |  |
| `total_amount` |  |  |  |
| `amount_before_tax` |  |  |  |
| `tax_amount` | 53.00 |  |  |
| `issuer` |  |  |  |

Region evidence:
- buyer: ``
- seller: ``
- table: `TRIP TABLE | 申请日期：2026-07-22 | 行程日期：2026-07-22至2026-07-22 | 行程人手机号：13669813770 | 行程总计：共1个行程，合计53.00元`
- summary: `金额 | 起点 | 用车时间 | 终点 | 1 | 接机/站 | 占北京 | 53.00 | 2026-07-2211:30:00 | 首都国际机场T2 | 中华人民共和国最高人民检察院-东门`
- issuer: ``

## 订单1128149704378709-电子普通发票(1).pdf

- Region score: 10/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26317000002664059212 | 26317000002664059212 | 26317000002664059212 |
| `date` | 2026年07月25日 | 2026年07月25日 | 2026年07月25日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 上海华程西南国际旅行社有限公司 | 上海华程西南国际旅行社有限公司 | 上海华程西南国际旅行社有限公司 |
| `seller_tax_id` | 91310105134638405A | 91310105134638405A | 91310105134638405A |
| `total_amount` | 1980.00 | 1980.00 | 1980.00 |
| `amount_before_tax` | 1867.92 |  | 1867.92 |
| `tax_amount` | 112.08 |  | 112.08 |
| `issuer` | 韩贇斐 |  | 韩贇斐 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：上海华程西南国际旅行社有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91310105134638405A`
- table: `项目名称 | 单位 | 规格型号 | 数量 | 单价 | 税率/征收率 | 金额 | 税额 | *生产生活服务*代订机票 | 11867.924528301887 | 1867.92 | 6% | 112.08 | 产品`
- summary: `计 | 合 | ￥1867.92 | ￥112.08 | 价税合计（大写） | 壹仟玖佰捌拾圆整 | (小写)￥1980.00`
- issuer: `开票人：韩贇斐`

## 订单1128149704378709-电子普通发票(2).pdf

- Region score: 10/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26317000002664059211 | 26317000002664059211 | 26317000002664059211 |
| `date` | 2026年07月25日 | 2026年07月25日 | 2026年07月25日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 上海华程西南国际旅行社有限公司 | 上海华程西南国际旅行社有限公司 | 上海华程西南国际旅行社有限公司 |
| `seller_tax_id` | 91310105134638405A | 91310105134638405A | 91310105134638405A |
| `total_amount` | 278.00 | 278.00 | 278.00 |
| `amount_before_tax` | 262.26 |  | 262.26 |
| `tax_amount` | 15.74 |  | 15.74 |
| `issuer` | 韩贇斐 |  | 韩贇斐 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：上海华程西南国际旅行社有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91310105134638405A`
- table: `项目名称 | 单位 | 规格型号 | 数量 | 单价 | 税率/征收率 | 金额 | 税额 | *生产生活服务*代订附加 | 1262.2641509433962 | 262.26 | 6% | 15.7 | 产品`
- summary: `计 | 合 | ￥262.26 | ￥15.74 | 价税合计（大写） | 贰佰柒拾捌圆整 | (小写)￥278.00`
- issuer: `开票人：韩贇斐`

## 订单1128149704378709-电子普通发票-保险(1).pdf

- Region score: 8/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26317000002633379048 | 26317000002633379048 | 26317000002633379048 |
| `date` | 2026年07月25日 | 2026年07月25日 | 2026年07月25日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 众安在线财产保险股份有限公司 | 众安在线财产保险股份有限公司 | 众安在线财产保险股份有限公司 |
| `seller_tax_id` | 91310000080013687R | 91310000080013687R | 91310000080013687R |
| `total_amount` | 27.36 | 29.00 | 29.00 |
| `amount_before_tax` |  |  | 27.36 |
| `tax_amount` |  |  | 1.64 |
| `issuer` | ， |  | 彭天灵 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：众安在线财产保险股份有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91310000080013687R`
- table: `项目名称 | 规格型号 | 单位 | 数量 | 单价 | 税率/征税率 | 税额 | 金额 | *金融服务*国内航空意外险29 | ** | 份 | 1 | 27.358491 | 6% | 27.36 | 1.64 | 元计划`
- summary: `计 | A | ￥27.36 | ￥1.64`
- issuer: `备注 | 开票人， | 彭无灵`

## 订单1128149704378709-电子普通发票-保险(2).pdf

- Region score: 7/10; OCR invoice_fields score: 7/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26317000002662000660 | 26317000002662000660 | 26317000002662000660 |
| `date` | 2026年07月25日 | 2026年07月25日 | 2026年07月25日 |
| `buyer_name` | 广东维信智联科技有限公司 | 规格型号 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 众安在线财产保险股份有限公司 | 众安在线财产保险股份有限公司 | 众安在线财产保险股份有限公司 |
| `seller_tax_id` | 91310000080013687R | 91310000080013687R | 91310000080013687R |
| `total_amount` | 27.36 | 29.00 | 29.00 |
| `amount_before_tax` |  |  | 27.36 |
| `tax_amount` |  |  | 1.64 |
| `issuer` |  |  | 彭天灵 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：众安在线财产保险股份有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91310000080013687R`
- table: `项目名称 | 规格型号 | 单位 | 数量 | 单价 | 税率/征稅率 | 金额 | 税额 | *金融服务*国内航空意外险29 | ** | 份 | 1 | 27.358491 | 6% | 27.36 | 1.64 | 元计划`
- summary: `计 | A | ￥27.36 | ￥1.64`
- issuer: `备注 | 彭无灵 | 开票人`

## 货拉拉电子发票 (4).pdf

- Region score: 8/10; OCR invoice_fields score: 6/10; PDF text+code score: 10/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26127000000365266621 |  | 26127000000365266621 |
| `date` | 2026年07月24日 | 2026年07月24日 | 2026年07月24日 |
| `buyer_name` | 广东维信智联科技有限公司 | 广东维信智联科技有限公司 | 广东维信智联科技有限公司 |
| `buyer_tax_id` | 91441900762909683X | 91441900762909683X | 91441900762909683X |
| `seller_name` | 啦啦（天津）汽车科技有限公司 | 啦啦（天津）汽车科技有限公司 | 啦啦（天津）汽车科技有限公司 |
| `seller_tax_id` | 91120118MA06URTX41 | 91120118MA06URTX41 | 91120118MA06URTX41 |
| `total_amount` | 307.06 | 307.06 | 307.06 |
| `amount_before_tax` |  |  | 281.71 |
| `tax_amount` |  |  | 25.35 |
| `issuer` | 谢一宽 |  | 谢一宽 |

Region evidence:
- buyer: `名称：广东维信智联科技有限公司 | 购买方信息 | 统一社会信用代码/纳税人识别号：91441900762909683X`
- seller: `名称：啦啦（天津）汽车科技有限公司 | 销售方信息 | 统一社会信用代码/纳税人识别号：91120118MA06URTX41`
- table: `项目名称 | 单位 | 数量 | 单价 | 金额 | 税额 | 税率/征收率 | 交通运输服务*货物运输费 | 次 | 1 | 281.71 | 281.71 | 9% | 25.35 | 计 | A | ￥281.71 | ¥25.3 | 运输工具种类 | 运输工具牌号 | 起运地`
- summary: `叁佰零柒圆零陆分 | 价税合计（大写） | （小写）¥307.06`
- issuer: `开票人：谢一宽`

## 货拉拉行程报销单 (3).pdf

- Region score: 1/10; OCR invoice_fields score: 1/10; PDF text+code score: 2/10.

| Field | bbox region | OCR invoice_fields | PDF text+code |
|---|---|---|---|
| `invoice_no` | 26127000000365266621 | 26127000000365266621 | 26127000000365266621 |
| `date` |  |  | 2026-07-24 |
| `buyer_name` |  |  |  |
| `buyer_tax_id` |  |  |  |
| `seller_name` |  |  |  |
| `seller_tax_id` |  |  |  |
| `total_amount` |  |  |  |
| `amount_before_tax` |  |  |  |
| `tax_amount` |  |  |  |
| `issuer` |  |  |  |

Region evidence:
- buyer: `部门： | 公司：广东维信智联科技有限公司 | 申请日期：2026-07-2415:44:39`
- seller: `共1条用车记录，总计307.06元 | 2026-07-24至2026-07-24`
- table: `订单备注 | 支付方式 | 里程 | 金额[元] | 订单编号 | 地址 | 序 | 下单人 | 货物类 | 车牌号 | 车型 | 用车时间 | [公里] | 号 | 型`
- summary: `门 | ●泰合鲜·精品鲁菜 | (陶然亭店)`
- issuer: `抗华就找 | `