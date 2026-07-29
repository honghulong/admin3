-- 报销费用类型字典：严格以 value 作为系统内费用类型编码。
-- 公里数是费用明细数量字段，不属于费用类型字典。

INSERT INTO sys_dict (dict_code, dict_name, description)
SELECT 'reimbursement_expense_type', '报销费用类型', '报销费用明细类型字典；template=公司OA模板列，extension=识别扩展类型'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict WHERE dict_code = 'reimbursement_expense_type'
);

SET @expense_type_dict_id := (
  SELECT id FROM sys_dict WHERE dict_code = 'reimbursement_expense_type' LIMIT 1
);

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '油费金额', 'fuel', 10, 'template;关键词:加油,汽油,柴油,油品,中石化,中石油'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'fuel');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '高速费', 'toll', 20, 'template;transport;关键词:高速,通行费,过路费,ETC,代收通行'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'toll');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '高铁/动车/火车费', 'rail', 30, 'template;transport;关键词:高铁,动车,火车,铁路,铁路电子客票'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'rail');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '地铁费', 'metro', 40, 'template;transport;关键词:地铁,轨道交通,城市轨道'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'metro');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '网约车费', 'ride_hailing', 50, 'template;transport;关键词:网约车,滴滴,出行,客运服务,用车,打车'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'ride_hailing');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '顺风车费', 'carpool', 60, 'template;transport;关键词:顺风车,拼车'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'carpool');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '货拉拉费', 'freight', 70, 'template;transport;关键词:货拉拉,货运,物流,货物运输,搬运,运输费'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'freight');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '停车费', 'parking', 80, 'template;transport;关键词:停车,停车场,泊车'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'parking');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '餐费', 'meal', 90, 'template;关键词:餐饮,餐费,饭店,餐厅,酒楼,美食,外卖,配送服务'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'meal');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '机票/航空费', 'air_travel', 110, 'extension;transport;关键词:机票,航空,航班,机场,航空运输,代订机票'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'air_travel');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '住宿费', 'hotel', 120, 'extension;关键词:住宿,酒店,宾馆,旅馆,房费'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'hotel');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '保险费', 'insurance', 130, 'extension;关键词:保险,意外险,航意险,保单,财产保险'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'insurance');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '出租车费', 'taxi', 140, 'extension;transport;关键词:出租车,出租汽车,的士'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'taxi');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '公交/客运费', 'bus', 150, 'extension;transport;关键词:公交,公共交通,汽车客运,客运车票,大巴'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'bus');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '租车费', 'rental_car', 160, 'extension;transport;关键词:租车,汽车租赁,包车'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'rental_car');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '车辆维修保养', 'vehicle_maintenance', 170, 'extension;关键词:维修,保养,洗车,轮胎,车辆检测'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'vehicle_maintenance');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '充电费', 'charging', 180, 'extension;关键词:充电,充电桩,新能源'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'charging');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '快递/配送费', 'delivery', 190, 'extension;关键词:快递,配送,邮寄,邮政,顺丰,京东物流'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'delivery');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '办公用品', 'office', 200, 'extension;关键词:办公用品,文具,耗材,打印,复印'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'office');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '通讯费', 'communication', 210, 'extension;关键词:电话费,话费,宽带,通信,移动,联通,电信'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'communication');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '会议/会务费', 'conference', 220, 'extension;关键词:会议,会务,展会,参会'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'conference');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '培训费', 'training', 230, 'extension;关键词:培训,课程,学习,考试'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'training');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '专业服务费', 'service', 240, 'extension;关键词:服务费,咨询,技术服务,代理服务,软件服务'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'service');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '招待费', 'entertainment', 250, 'extension;关键词:招待,宴请,商务接待'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'entertainment');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '物料/低值易耗品', 'supplies', 260, 'extension;关键词:物料,耗材,低值易耗,材料'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'supplies');

INSERT INTO sys_dict_value (dict_id, label, value, sort_order, description)
SELECT @expense_type_dict_id, '其他', 'other', 999, 'template;无法稳定归类'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_value WHERE dict_id = @expense_type_dict_id AND value = 'other');
