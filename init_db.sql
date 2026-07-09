-- ============================================================
-- admin3 数据库完整初始化脚本
-- 导入命令：mysql -u root -p --default-character-set=utf8 < init_db.sql
-- 支持 MySQL 5.7+
-- ============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS admin3 CHARACTER SET utf8 COLLATE utf8_general_ci;
USE admin3;

-- 强制客户端连接使用 utf8，防止中文乱码
SET NAMES utf8;

SELECT '[OK] Database admin3 ready' as status;

-- 2. 建表结构
-- ============================================================
set foreign_key_checks = 0;

-- 2.1 组织架构表
DROP TABLE IF EXISTS `organization`;
CREATE TABLE `organization` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `parent_ids` varchar(255) DEFAULT NULL,
  `type` tinyint(4) NOT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc30yedjwp9qw1f3nn2ytda7tj` (`parent_id`),
  CONSTRAINT `FKc30yedjwp9qw1f3nn2ytda7tj` FOREIGN KEY (`parent_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.2 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `avatar` varchar(255) DEFAULT NULL,
  `created_time` datetime(6) DEFAULT NULL,
  `gender` tinyint(4) NOT NULL,
  `last_login_time` datetime(6) DEFAULT NULL,
  `state` tinyint(4) NOT NULL,
  `username` varchar(255) NOT NULL,
  `x_employee_id` varchar(255) DEFAULT NULL,
  `organization_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_sb8bbouer5wak8vyiiy4pf2bx` (`username`),
  UNIQUE KEY `UK_asrm1b0uhrbtg7b3aaoln3lle` (`x_employee_id`),
  KEY `FK9o02c5db97siwu48bqivpo4c0` (`organization_id`),
  CONSTRAINT `FK9o02c5db97siwu48bqivpo4c0` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.3 用户凭证表
DROP TABLE IF EXISTS `user_credential`;
CREATE TABLE `user_credential` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `credential` varchar(255) NOT NULL,
  `identifier` varchar(255) NOT NULL,
  `identity_type` tinyint(4) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6y499rs9ocqqa3kdpu2f32m86` (`user_id`),
  CONSTRAINT `FK6y499rs9ocqqa3kdpu2f32m86` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.4 会话表
DROP TABLE IF EXISTS `session`;
CREATE TABLE `session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data` longtext,
  `expire_time` datetime(6) NOT NULL,
  `last_login_time` datetime(6) NOT NULL,
  `last_modified_time` datetime(6) DEFAULT NULL,
  `token` varchar(255) NOT NULL,
  `credential_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKg65wpogd8ymr8lpth9m583sja` (`credential_id`),
  CONSTRAINT `FKg65wpogd8ymr8lpth9m583sja` FOREIGN KEY (`credential_id`) REFERENCES `user_credential` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.5 角色表
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `available` bit(1) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8sewwnpamngi6b1dwaa88askk` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.6 资源/权限表
DROP TABLE IF EXISTS `resource`;
CREATE TABLE `resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `icon` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `parent_ids` varchar(255) DEFAULT NULL,
  `permission` varchar(255) DEFAULT NULL,
  `type` tinyint(4) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKs2byvqo0b2enh3rltln5mmvyl` (`parent_id`),
  CONSTRAINT `FKs2byvqo0b2enh3rltln5mmvyl` FOREIGN KEY (`parent_id`) REFERENCES `resource` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.7 角色-资源关联表
DROP TABLE IF EXISTS `role_resource`;
CREATE TABLE `role_resource` (
  `role_id` bigint(20) NOT NULL,
  `resource_id` bigint(20) NOT NULL,
  PRIMARY KEY (`role_id`,`resource_id`),
  KEY `FKr2orp5em3dob6f299ra9oyexr` (`resource_id`),
  CONSTRAINT `FKh8lunkrwoyio367ec8y12bis1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `FKr2orp5em3dob6f299ra9oyexr` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.8 用户-角色关联表
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKa68196081fvovjhkek5m97n3y` (`role_id`),
  CONSTRAINT `FKa68196081fvovjhkek5m97n3y` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `FKfgsgxvihks805qcq8sq26ab7c` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.9 存储配置表
DROP TABLE IF EXISTS `storage_config`;
CREATE TABLE `storage_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `access_key` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `bucket_name` varchar(255) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `create_user` varchar(255) DEFAULT NULL,
  `endpoint` varchar(255) DEFAULT NULL,
  `is_default` bit(1) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `secret_key` varchar(255) DEFAULT NULL,
  `storage_id` varchar(255) DEFAULT NULL,
  `storage_path` varchar(255) DEFAULT NULL,
  `type` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.10 存储文件表
DROP TABLE IF EXISTS `storage_file`;
CREATE TABLE `storage_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `create_user` varchar(255) DEFAULT NULL,
  `key` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `storage_id` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK575ag81fcckiyb7jxbu657s10` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.11 操作事件日志表
DROP TABLE IF EXISTS `stored_event`;
CREATE TABLE `stored_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `event_body` longtext,
  `occurred_on` datetime(6) DEFAULT NULL,
  `type_name` varchar(255) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4y0r4thyym073n8jo5a5pci88` (`user_id`),
  CONSTRAINT `FK4y0r4thyym073n8jo5a5pci88` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.12 系统字典表
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `dict_code` varchar(255) NOT NULL,
  `dict_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_kywjqkex25xa7tkw126tdr1vr` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.13 系统字典值表
DROP TABLE IF EXISTS `sys_dict_value`;
CREATE TABLE `sys_dict_value` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `label` varchar(255) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `value` varchar(255) NOT NULL,
  `dict_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmpq3t9abtype7smm87pgfg959` (`dict_id`),
  CONSTRAINT `FKmpq3t9abtype7smm87pgfg959` FOREIGN KEY (`dict_id`) REFERENCES `sys_dict` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.14 请假表
DROP TABLE IF EXISTS `leave`;
CREATE TABLE `leave` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cancel_time` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `end_time` datetime(6) NOT NULL,
  `leave_reason` varchar(200) DEFAULT NULL,
  `leave_status` varchar(255) NOT NULL,
  `leave_type` varchar(255) NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhwuruv390bf2c901utyc54yv0` (`user_id`),
  CONSTRAINT `FKhwuruv390bf2c901utyc54yv0` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.15 报销管理表
DROP TABLE IF EXISTS `reimbursement`;
CREATE TABLE `reimbursement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` decimal(10,2) NOT NULL,
  `applicant_id` bigint(20) NOT NULL,
  `applicant_name` varchar(50) NOT NULL,
  `approve_comment` text,
  `approve_time` datetime(6) DEFAULT NULL,
  `approver_id` bigint(20) DEFAULT NULL,
  `approver_name` varchar(50) DEFAULT NULL,
  `buyer_name` varchar(200) DEFAULT NULL,
  `buyer_tax_id` varchar(50) DEFAULT NULL,
  `category` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `description` text,
  `invoice_code` varchar(50) DEFAULT NULL,
  `invoice_date` datetime(6) DEFAULT NULL,
  `invoice_no` varchar(50) DEFAULT NULL,
  `invoice_status` varchar(50) DEFAULT NULL,
  `invoice_type` varchar(50) DEFAULT NULL,
  `ocr_raw_json` mediumtext,
  `seller_name` varchar(200) DEFAULT NULL,
  `seller_tax_id` varchar(50) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `title` varchar(100) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.16 审批日志表
DROP TABLE IF EXISTS `approval_log`;
CREATE TABLE `approval_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `action` varchar(20) NOT NULL,
  `comment` text,
  `created_at` datetime(6) NOT NULL,
  `operator_id` bigint(20) NOT NULL,
  `operator_name` varchar(50) NOT NULL,
  `reimbursement_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.17 附件表
DROP TABLE IF EXISTS `attachment`;
CREATE TABLE `attachment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint(20) NOT NULL,
  `file_type` varchar(50) NOT NULL,
  `file_url` varchar(500) NOT NULL,
  `ocr_result` mediumtext,
  `ocr_status` varchar(20) DEFAULT NULL,
  `reimbursement_id` bigint(20) DEFAULT NULL,
  `uploaded_by` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.18 发票临时表
DROP TABLE IF EXISTS `invoice_temp`;
CREATE TABLE `invoice_temp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `buyer_name` varchar(200) DEFAULT NULL,
  `buyer_tax_id` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `invoice_code` varchar(50) DEFAULT NULL,
  `invoice_date` datetime(6) DEFAULT NULL,
  `invoice_no` varchar(50) DEFAULT NULL,
  `invoice_status` varchar(50) DEFAULT NULL,
  `invoice_type` varchar(50) DEFAULT NULL,
  `ocr_raw_json` mediumtext,
  `reimbursement_id` bigint(20) DEFAULT NULL,
  `seller_name` varchar(200) DEFAULT NULL,
  `seller_tax_id` varchar(50) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `total_amount` decimal(10,2) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2.19 小艺 MCP 用户会话绑定表
DROP TABLE IF EXISTS `xiao_yi_user_session`;
CREATE TABLE `xiao_yi_user_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `agent_login_session_id` varchar(255) DEFAULT NULL,
  `created_time` datetime(6) NOT NULL,
  `expire_time` datetime(6) NOT NULL,
  `huawei_open_id` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  `sid` varchar(255) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ouhjlegi3vi8rjl2kra1ksgc3` (`sid`),
  KEY `FKcjoea6ucy4bcaolxwfn0omqb8` (`user_id`),
  CONSTRAINT `FKcjoea6ucy4bcaolxwfn0omqb8` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SELECT '[OK] All tables created' as status;

set foreign_key_checks = 1;

-- 3. 导入基础数据
set foreign_key_checks = 0;

-- 3.1 组织架构
INSERT INTO organization (id, name, parent_ids, type, parent_id) VALUES (1, '根节点', '/', 0, null);
INSERT INTO organization (id, name, parent_ids, type, parent_id) VALUES (2, '水浒传', '/1/', 0, 1);
INSERT INTO organization (id, name, parent_ids, type, parent_id) VALUES (3, '测试账号', '/1/', 0, 1);

-- 3.2 用户
INSERT INTO user (id, username, x_employee_id, avatar, created_time, gender, state, organization_id) VALUES (1, 'admin', 'EMP001', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 1);
INSERT INTO user (id, username, x_employee_id, avatar, created_time, gender, state, organization_id) VALUES (2, 'employee', 'EMP002', 'avatar.jpg', '2023-01-05 17:16:11.000000', 1, 0, 3);
INSERT INTO user (id, username, x_employee_id, avatar, created_time, gender, state, organization_id) VALUES (3, 'guest', 'EMP003', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 3);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (202, '呼保义宋江 ', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (203, '玉麒麟卢俊义', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (204, '智多星吴用', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (205, '入云龙公孙胜', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (206, '大刀关胜', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (207, '豹子头林冲', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (208, '霹雳火秦明', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (209, '双鞭呼延灼', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 1, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (210, '小李广花荣', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 1, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (211, '小旋风柴进', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 1, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (212, '扑天雕李应', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 1, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (213, '美髯公朱仝', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (214, '花和尚鲁智深', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (215, '行者武松', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (216, '双抢将董平', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (217, '没羽箭张清', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (218, '青面兽杨志', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (219, '金抢手徐宁', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (220, '急先锋索超', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (221, '神行太保戴宗', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (222, '赤发鬼刘唐', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (223, '黑旋风李逵', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (224, '九纹龙史进', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (225, '没遮拦穆弘', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (226, '插翅虎雷横', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (227, '混江龙李俊', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (228, '立地太岁阮小二', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (229, '船火儿张横', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (230, '短命二郎阮小五', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (231, '浪里白条张顺', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (232, '活阎罗阮小七', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (233, '病关索扬雄', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (234, '拼命三郎石秀', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (235, '两头蛇解珍', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (236, '双尾蝎解宝', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (237, '浪子燕青', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (238, '神机军师朱武', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (239, '镇三山黄信', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (240, '病尉迟孙立', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (241, '丑郡马宣赞', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (242, '井木犴郝思文', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (243, '百胜将韩滔', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (244, '天目将彭玘', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (245, '圣水将单廷圭', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (246, '神火将魏定国', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (247, '圣手书生萧让', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (248, '铁面孔目裴宣', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (249, '摩云金翅欧鹏', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (250, '火眼狻猊邓飞', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (251, '锦毛虎燕顺', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (252, '锦豹子杨林', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (253, '轰天雷凌振', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (254, '神算子蒋敬', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (255, '小温侯吕方', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (256, '赛仁贵郭盛', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (257, '神医安道全', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (258, '紫髯伯皇甫端', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (259, '矮脚虎王英', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (260, '一丈青扈三娘', 'avatar.jpg', '2023-01-05 17:16:11.000000', 1, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (261, '丧门神鲍旭', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (262, '混世魔王樊瑞', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (263, '毛头星孔明', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (264, '独火星孔亮', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (265, '八臂哪吒项充', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (266, '飞天大圣李衮', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (267, '玉臂匠金大坚', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (268, '铁笛仙马麟', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (269, '出洞蛟童威', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (270, '翻江蜃童猛', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (271, '玉幡竿孟康', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (272, '通臂猿侯健', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (273, '跳涧虎陈达', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (274, '白花蛇杨春', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (275, '白面郎君郑天寿', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (276, '九尾龟陶宗旺', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (277, '铁扇子宋清', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (278, '铁叫子乐和', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (279, '花项虎龚旺', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (280, '中箭虎丁得孙', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (281, '小遮拦穆春', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (282, '操刀鬼曹正', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (283, '云里金刚宋万', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (284, '摸着天杜迁', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (285, '病大虫薛永', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (286, '金眼彪施恩', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (287, '打虎将李忠', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (288, '小霸王周通', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (289, '金钱豹子汤隆', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (290, '鬼脸儿杜兴', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (291, '出林龙邹渊', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (292, '独角龙邹润', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (293, '旱地忽律朱贵', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (294, '笑面虎朱富', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (295, '铁臂膊蔡福', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (296, '一枝花蔡庆', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (297, '催命判官李立', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (298, '青眼虎李云', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (299, '没面目焦挺', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (300, '石将军石勇', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (301, '小尉迟孙新', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (302, '母大虫顾大嫂', 'avatar.jpg', '2023-01-05 17:16:11.000000', 1, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (303, '菜园子张青', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (304, '母夜叉孙二娘', 'avatar.jpg', '2023-01-05 17:16:11.000000', 1, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (305, '活闪婆王定六', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (306, '险道神郁保四', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (307, '白日鼠白 胜', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (308, '鼓上蚤时迁', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);
INSERT INTO user (id, username, avatar, created_time, gender, state, organization_id) VALUES (309, '金毛犬段景住', 'avatar.jpg', '2023-01-05 17:16:11.000000', 0, 0, 2);

-- 3.3 用户凭证（密码：123456）
INSERT INTO user_credential (id, credential, identifier, identity_type, user_id) VALUES (1, 'a66abb5684c45962d887564f08346e8d', 'admin', 0, 1);
INSERT INTO user_credential (id, credential, identifier, identity_type, user_id) VALUES (2, '9c0967753a201ecde21ef29efa514761', 'employee', 0, 2);
INSERT INTO user_credential (id, credential, identifier, identity_type, user_id) VALUES (3, 'fe4ceeb01d43a6c29d8f4fe93313c6c1', 'guest', 0, 3);

-- 3.4 角色
INSERT INTO role (id, available, description, name) VALUES (1, true, '超级管理员可以对企业内的所有用户进行管理，请谨慎修改超管权限', '超级管理员');
INSERT INTO role (id, available, description, name) VALUES (2, true, '项目开发人员', '开发者');
INSERT INTO role (id, available, description, name) VALUES (3, true, '普通的用户', '普通用户');
INSERT INTO role (id, available, description, name) VALUES (4, false, '只能查看的角色', '游客');

-- 3.5 用户-角色关联
INSERT INTO user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO user_role (user_id, role_id) VALUES (2, 2);
INSERT INTO user_role (user_id, role_id) VALUES (3, 4);
INSERT INTO user_role (user_id, role_id) VALUES (202, 2);
INSERT INTO user_role (user_id, role_id) VALUES (203, 1);
INSERT INTO user_role (user_id, role_id) VALUES (203, 3);
INSERT INTO user_role (user_id, role_id) VALUES (204, 3);
INSERT INTO user_role (user_id, role_id) VALUES (205, 3);
INSERT INTO user_role (user_id, role_id) VALUES (206, 3);
INSERT INTO user_role (user_id, role_id) VALUES (207, 3);
INSERT INTO user_role (user_id, role_id) VALUES (208, 3);
INSERT INTO user_role (user_id, role_id) VALUES (209, 3);
INSERT INTO user_role (user_id, role_id) VALUES (210, 3);
INSERT INTO user_role (user_id, role_id) VALUES (211, 3);
INSERT INTO user_role (user_id, role_id) VALUES (212, 3);
INSERT INTO user_role (user_id, role_id) VALUES (302, 2);

-- 3.6 资源/权限
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (1, null, '根节点', null, '*', null, null, null);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (2, 'Odometer', '仪表盘', null, 'dashboard', 0, '/dashboard', 1);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (3, 'SetUp', '系统管理', null, 'sys', 0, '/sys', 1);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (4, 'User', '用户管理', null, 'user:view', 0, '/users', 3);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (5, 'Tickets', '角色管理', null, 'role:view', 0, '/roles', 3);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (6, 'Collection', '权限资源', null, 'resource:view', 0, '/resources', 3);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (7, null, '查看用户', null, 'user:view', 1, null, 4);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (8, null, '新增用户', null, 'user:create', 1, null, 4);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (9, null, '修改用户', null, 'user:update', 1, null, 4);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (10, null, '删除用户', null, 'user:delete', 1, null, 4);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (11, null, '查看角色', null, 'role:view', 1, null, 5);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (12, null, '新增角色', null, 'role:create', 1, null, 5);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (13, null, '修改角色', null, 'role:update', 1, null, 5);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (14, null, '删除角色', null, 'role:delete', 1, null, 5);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (15, null, '查看资源', null, 'resource:view', 1, null, 6);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (16, null, '新增资源', null, 'resource:create', 1, null, 6);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (17, null, '修改资源', null, 'resource:update', 1, null, 6);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (18, null, '删除资源', null, 'resource:delete', 1, null, 6);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (19, null, '新增组织架构', null, 'organization:create', 1, null, 4);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (20, null, '修改组织架构', null, 'organization:update', 1, null, 4);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (21, null, '删除组织架构', null, 'organization:delete', 1, null, 4);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (22, 'Timer', '操作日志', null, 'log:view', 0, '/logs', 3);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (23, null, '清空日志', null, 'log:clean', 1, null, 22);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (24, null, '查看日志', null, 'log:view', 1, null, 22);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (25, 'Files', '对象存储', null, 'storage:view', 0, '/storage', 3);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (26, null, '查看对象存储', null, 'storage:view', 1, null, 25);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (27, null, '新增对象存储', null, 'storage:create', 1, null, 25);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (28, null, '更新对象存储', null, 'storage:update', 1, null, 25);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (29, null, '删除对象存储', null, 'storage:delete', 1, null, 25);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (30, null, '设置默认存储', null, 'storage:markAsDefault', 1, null, 25);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (31, 'Calendar', '请假管理', null, 'leave:view', 0, '/leaves', 1);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (32, null, '查看请假', null, 'leave:view', 1, null, 31);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (33, null, '新增请假', null, 'leave:create', 1, null, 31);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (34, null, '修改请假', null, 'leave:update', 1, null, 31);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (35, null, '删除请假', null, 'leave:delete', 1, null, 31);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (36, null, '审批请假', null, 'leave:approve', 1, null, 31);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (37, null, '销假', null, 'leave:cancel', 1, null, 31);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (38, 'Reading', '字典管理', null, 'dict:view', 0, '/dict', 3);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (39, null, '查看字典', null, 'dict:view', 1, null, 38);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (40, null, '新增字典', null, 'dict:create', 1, null, 38);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (41, null, '修改字典', null, 'dict:update', 1, null, 38);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (42, null, '删除字典', null, 'dict:delete', 1, null, 38);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (43, 'Money', '报销管理', null, 'reimbursement:view', 0, '/reimbursements', 1);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (44, null, '查看报销', null, 'reimbursement:view', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (45, null, '新增报销', null, 'reimbursement:create', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (46, null, '修改报销', null, 'reimbursement:update', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (47, null, '删除报销', null, 'reimbursement:delete', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (48, null, '提交报销', null, 'reimbursement:submit', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (49, null, '审批报销', null, 'reimbursement:approve', 1, null, 43);
INSERT INTO resource (id, icon, name, parent_ids, permission, type, url, parent_id) VALUES (50, null, '撤回报销', null, 'reimbursement:recall', 1, null, 43);

-- 3.7 角色-资源关联
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 7), (1, 8), (1, 9), (1, 10);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 11), (1, 12), (1, 13), (1, 14);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 15), (1, 16), (1, 17), (1, 18);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 19), (1, 20), (1, 21);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 22), (1, 23), (1, 24);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 25), (1, 26), (1, 27), (1, 28), (1, 29), (1, 30);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 31), (1, 32), (1, 33), (1, 34), (1, 35), (1, 36), (1, 37);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 38), (1, 39), (1, 40), (1, 41), (1, 42);
INSERT INTO role_resource (role_id, resource_id) VALUES (1, 43), (1, 44), (1, 45), (1, 46), (1, 47), (1, 48), (1, 49), (1, 50);

INSERT INTO role_resource (role_id, resource_id) VALUES (2, 2), (2, 3), (2, 6), (2, 15), (2, 16), (2, 17), (2, 18);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 22), (2, 23), (2, 24);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 25), (2, 26), (2, 27), (2, 28), (2, 29), (2, 30);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 31), (2, 32), (2, 33), (2, 34), (2, 35), (2, 36), (2, 37);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 38), (2, 39), (2, 40), (2, 41), (2, 42);
INSERT INTO role_resource (role_id, resource_id) VALUES (2, 43), (2, 44), (2, 45), (2, 46), (2, 47), (2, 48), (2, 49), (2, 50);

INSERT INTO role_resource (role_id, resource_id) VALUES (3, 2);
INSERT INTO role_resource (role_id, resource_id) VALUES (4, 2), (4, 3), (4, 4), (4, 5), (4, 6);
INSERT INTO role_resource (role_id, resource_id) VALUES (4, 7), (4, 11), (4, 15);

-- 3.8 存储配置
INSERT INTO storage_config (type, id, is_default, name, address, storage_path, access_key, bucket_name, endpoint, secret_key, create_time, create_user, storage_id) VALUES (0, 1, true, '本地', 'storage/fetch/', 'files', null, null, null, null, '2023-07-10 17:00:48.000000', 'admin', 'SsIPzgpd9rFgxJhe3yUxk');

-- 3.9 请假数据
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (100, 'sick', '2026-05-20 09:00:00.000000', '2026-05-21 18:00:00.000000', '感冒发烧，需要休息', '0', null, 2);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (101, 'personal', '2026-05-25 09:00:00.000000', '2026-05-25 18:00:00.000000', '家里有急事需要处理', '0', null, 3);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (102, 'maternity', '2026-06-01 09:00:00.000000', '2026-06-30 18:00:00.000000', '产假', '1', null, 260);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (103, 'offshift', '2026-05-28 09:00:00.000000', '2026-05-28 18:00:00.000000', '周末加班调休', '2', null, 207);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (104, 'sick', '2026-05-15 09:00:00.000000', '2026-05-16 18:00:00.000000', '身体不适，去医院检查', '3', '2026-05-17 10:00:00.000000', 214);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (105, 'personal', '2026-06-05 09:00:00.000000', '2026-06-05 18:00:00.000000', '参加朋友婚礼', '0', null, 223);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (106, 'sick', '2026-06-10 09:00:00.000000', '2026-06-12 18:00:00.000000', '腰椎不适，需卧床休息', '0', null, 237);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (107, 'offshift', '2026-05-30 09:00:00.000000', '2026-05-30 18:00:00.000000', '五一值班调休', '1', null, 202);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (108, 'personal', '2026-06-15 09:00:00.000000', '2026-06-16 18:00:00.000000', '回老家探亲', '0', null, 215);
INSERT INTO `leave` (id, leave_type, start_time, end_time, leave_reason, leave_status, cancel_time, user_id) VALUES (109, 'sick', '2026-05-18 09:00:00.000000', '2026-05-18 18:00:00.000000', '牙疼需要治疗', '2', null, 208);

-- 3.10 系统字典
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (1, 'leave_type', '请假类型', '请假申请中的请假类型');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (1, 1, '病假', 'sick', 1, '因病请假');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (2, 1, '事假', 'personal', 2, '因私事请假');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (3, 1, '年假', 'annual', 3, '年休假');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (4, 1, '婚假', 'marriage', 4, '结婚休假');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (5, 1, '丧假', 'bereavement', 5, '丧事休假');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (6, 1, '产假', 'maternity', 6, '产假');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (7, 1, '调休', 'offshift', 7, '调休');

INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (2, 'leave_status', '请假状态', '请假申请的状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (8, 2, '刚提交', '0', 1, '刚提交的请假申请');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (9, 2, '审核通过', '1', 2, '审核通过的请假申请');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (10, 2, '被退回', '2', 3, '被退回的请假申请');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (11, 2, '已销假', '3', 4, '已销假的请假申请');

INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (3, 'user_gender', '用户性别', '用户性别');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (12, 3, '男', '0', 1, '男性');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (13, 3, '女', '1', 2, '女性');

INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (4, 'user_state', '用户状态', '用户账户状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (14, 4, '正常', '0', 1, '账户正常');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (15, 4, '锁定', '1', 2, '账户被锁定');

-- 3.11 报销数据
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (1, '6月出差上海差旅费', 'travel', 1280.50, '6月1日-3日上海出差差旅费', 'pending', 2, '管理员', '2026-06-01 10:00:00.000000', '2026-06-01 10:00:00.000000', 2, 2);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (2, '办公用品采购', 'office', 320.00, '采购打印纸、笔、文件夹等', 'approved', 3, '张三', '2026-06-01 14:00:00.000000', '2026-06-02 09:00:00.000000', 3, 3);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (3, '打车费用报销', 'transport', 85.00, '5月28日客户拜访打车', 'draft', 2, '管理员', '2026-06-02 11:00:00.000000', '2026-06-02 11:00:00.000000', 2, 2);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (4, '客户招待餐饮费', 'catering', 560.00, '招待重要客户午餐', 'rejected', 3, '张三', '2026-05-30 16:00:00.000000', '2026-05-31 10:00:00.000000', 3, 3);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (5, '端午节福利采购', 'other', 2000.00, '端午节员工福利采购', 'pending', 2, '管理员', '2026-05-25 09:00:00.000000', '2026-05-25 09:00:00.000000', 2, 2);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (6, '6月出差深圳差旅费', 'travel', 3500.00, '6月5日-7日深圳出差差旅费', 'pending', 202, '呼保义宋江', '2026-06-03 08:00:00.000000', '2026-06-03 08:00:00.000000', 202, 202);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (7, '项目团建餐饮费', 'catering', 1800.00, '项目第一阶段完成团建聚餐', 'draft', 203, '玉麒麟卢俊义', '2026-06-02 16:00:00.000000', '2026-06-02 16:00:00.000000', 203, 203);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (8, '服务器采购', 'office', 15000.00, '采购开发服务器一台', 'pending', 207, '豹子头林冲', '2026-06-01 09:30:00.000000', '2026-06-01 09:30:00.000000', 207, 207);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (9, '客户拜访交通费', 'transport', 230.00, '6月2日拜访客户往返打车', 'approved', 214, '花和尚鲁智深', '2026-06-02 10:00:00.000000', '2026-06-03 09:00:00.000000', 214, 214);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (10, '技术书籍采购', 'other', 450.00, '采购Spring Cloud相关技术书籍', 'recalled', 215, '行者武松', '2026-05-28 14:00:00.000000', '2026-05-29 11:00:00.000000', 215, 215);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (11, '成都出差差旅费', 'travel', 2200.00, '5月20日-22日成都出差', 'approved', 218, '青面兽杨志', '2026-05-19 09:00:00.000000', '2026-05-23 10:00:00.000000', 218, 218);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (12, '团队下午茶', 'catering', 320.00, '周五团队下午茶', 'rejected', 223, '黑旋风李逵', '2026-06-02 15:00:00.000000', '2026-06-03 08:30:00.000000', 223, 223);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (13, '出差北京高铁票', 'transport', 580.00, '5月15日北京出差高铁票', 'approved', 237, '浪子燕青', '2026-05-14 10:00:00.000000', '2026-05-16 14:00:00.000000', 237, 237);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (14, '显示器采购', 'office', 1299.00, '采购27寸4K显示器一台', 'pending', 240, '病尉迟孙立', '2026-06-03 11:00:00.000000', '2026-06-03 11:00:00.000000', 240, 240);
INSERT INTO reimbursement (id, title, category, amount, description, status, applicant_id, applicant_name, created_at, updated_at, created_by, updated_by) VALUES (15, '云服务器费用', 'other', 299.00, '6月阿里云服务器费用', 'draft', 202, '呼保义宋江', '2026-06-01 00:00:00.000000', '2026-06-01 00:00:00.000000', 202, 202);

-- 3.12 审批日志
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (1, 2, 'submit', 3, '张三', null, '2026-06-01 14:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (2, 2, 'approve', 2, '管理员', '同意报销', '2026-06-02 09:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (3, 4, 'submit', 3, '张三', null, '2026-05-30 16:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (4, 4, 'reject', 2, '管理员', '发票金额与实际申请不一致，请重新核实', '2026-05-31 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (5, 9, 'submit', 214, '花和尚鲁智深', null, '2026-06-02 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (6, 9, 'approve', 2, '管理员', '核实无误，同意报销', '2026-06-03 09:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (7, 10, 'submit', 215, '行者武松', null, '2026-05-28 14:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (8, 10, 'recall', 215, '行者武松', '重复提交了，先撤回', '2026-05-29 11:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (9, 11, 'submit', 218, '青面兽杨志', null, '2026-05-19 09:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (10, 11, 'approve', 2, '管理员', '同意报销', '2026-05-23 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (11, 12, 'submit', 223, '黑旋风李逵', null, '2026-06-02 15:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (12, 12, 'reject', 2, '管理员', '下午茶费用请从团建经费中支出', '2026-06-03 08:30:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (13, 13, 'submit', 237, '浪子燕青', null, '2026-05-14 10:00:00.000000');
INSERT INTO approval_log (id, reimbursement_id, action, operator_id, operator_name, comment, created_at) VALUES (14, 13, 'approve', 2, '管理员', '同意', '2026-05-16 14:00:00.000000');

-- 3.13 报销字典数据
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (5, 'reimbursement_category', '报销类别', '报销申请的类别');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (16, 5, '差旅', 'travel', 1, '出差差旅费');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (17, 5, '办公', 'office', 2, '办公用品采购');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (18, 5, '交通', 'transport', 3, '交通费用');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (19, 5, '餐饮', 'catering', 4, '餐饮费用');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (20, 5, '其他', 'other', 5, '其他费用');
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (6, 'reimbursement_status', '报销状态', '报销申请的状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (21, 6, '草稿', 'draft', 1, '草稿状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (22, 6, '待审批', 'pending', 2, '待审批状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (23, 6, '已通过', 'approved', 3, '已审批通过');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (24, 6, '已退回', 'rejected', 4, '已审批退回');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (25, 6, '已撤回', 'recalled', 5, '已撤回');
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (7, 'reimbursement_invoice_type', '发票类型', '发票的类型分类');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (26, 7, '增值税专用发票', 'special', 1, '增值税专用发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (27, 7, '增值税普通发票', 'normal', 2, '增值税普通发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (28, 7, '电子发票', 'electronic', 3, '电子发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (29, 7, '定额发票', 'fixed', 4, '定额发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (30, 7, '机动车销售发票', 'vehicle', 5, '机动车销售统一发票');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (31, 7, '其他', 'other', 6, '其他类型发票');
INSERT INTO sys_dict (id, dict_code, dict_name, description) VALUES (8, 'reimbursement_invoice_status', '发票状态', '发票的状态');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (32, 8, '正常', 'normal', 1, '发票正常');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (33, 8, '已作废', 'voided', 2, '发票已作废');
INSERT INTO sys_dict_value (id, dict_id, label, value, sort_order, description) VALUES (34, 8, '已红冲', 'red_charged', 3, '发票已红冲');

-- 3.14 XiaoYi MCP 测试数据
INSERT INTO xiao_yi_user_session (sid, session_id, agent_login_session_id, user_id, created_time, expire_time)
VALUES
    ('test_sid_bound_001',   'test_session_bound_001',   'test_bound_001',   1, NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR)),
    ('test_sid_unbound_001', 'test_session_unbound_001', 'test_unbound_001', NULL, NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR)),
    ('test_sid_expired_001', 'test_session_expired_001', 'test_expired_001', 1, NOW(), DATE_ADD(NOW(), INTERVAL -1 HOUR));

SELECT '[OK] All data imported' as status;

set foreign_key_checks = 1;