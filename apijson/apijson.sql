/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50550
Source Host           : localhost:3306
Source Database       : apijson

Target Server Type    : MYSQL
Target Server Version : 50550
File Encoding         : 65001

Date: 2020-08-20 20:58:35
Update: 2021-07-13 10:58:35
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for access
-- ----------------------------
DROP TABLE IF EXISTS `access`;
CREATE TABLE `access` (
  `id` bigint(15) NOT NULL AUTO_INCREMENT,
  `debug` tinyint(2) NOT NULL DEFAULT '0' COMMENT '是否为调试表，只允许在开发环境使用，测试和线上环境禁用',
  `name` varchar(50) NOT NULL COMMENT '实际表名，例如 apijson_user',
  `alias` varchar(20) DEFAULT NULL COMMENT '外部调用的表别名，例如 User',
  `get` varchar(100) NOT NULL DEFAULT '["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]' COMMENT '允许 get 的角色列表，例如 ["LOGIN", "CONTACT", "CIRCLE", "OWNER"]\n用 JSON 类型不能设置默认值，反正权限对应的需求是明确的，也不需要自动转 JSONArray。\nTODO: 直接 LOGIN,CONTACT,CIRCLE,OWNER 更简单，反正是开发内部用，不需要复杂查询。',
  `head` varchar(100) NOT NULL DEFAULT '["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]' COMMENT '允许 head 的角色列表，例如 ["LOGIN", "CONTACT", "CIRCLE", "OWNER"]',
  `gets` varchar(100) NOT NULL DEFAULT '["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]' COMMENT '允许 gets 的角色列表，例如 ["LOGIN", "CONTACT", "CIRCLE", "OWNER"]',
  `heads` varchar(100) NOT NULL DEFAULT '["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]' COMMENT '允许 heads 的角色列表，例如 ["LOGIN", "CONTACT", "CIRCLE", "OWNER"]',
  `post` varchar(100) NOT NULL DEFAULT '["OWNER", "ADMIN"]' COMMENT '允许 post 的角色列表，例如 ["LOGIN", "CONTACT", "CIRCLE", "OWNER"]',
  `put` varchar(100) NOT NULL DEFAULT '["OWNER", "ADMIN"]' COMMENT '允许 put 的角色列表，例如 ["LOGIN", "CONTACT", "CIRCLE", "OWNER"]',
  `delete` varchar(100) NOT NULL DEFAULT '["OWNER", "ADMIN"]' COMMENT '允许 delete 的角色列表，例如 ["LOGIN", "CONTACT", "CIRCLE", "OWNER"]',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `detail` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`),
  UNIQUE KEY `alias_UNIQUE` (`alias`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8 COMMENT='权限配置(必须)';

-- ----------------------------
-- Records of access
-- ----------------------------
INSERT INTO `access` VALUES ('1','0','Access',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2019-07-21 12:21:36',null);
INSERT INTO `access` VALUES ('2','0','Table',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:14',null);
INSERT INTO `access` VALUES ('3','0','Column',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:14',null);
INSERT INTO `access` VALUES ('4','0','Function',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:15',null);
INSERT INTO `access` VALUES ('5','0','Request',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:14',null);
INSERT INTO `access` VALUES ('6','0','Response',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:15',null);
INSERT INTO `access` VALUES ('7','0','Document',null,'["LOGIN", "ADMIN"]','["LOGIN", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2018-11-28 16:38:15',null);
INSERT INTO `access` VALUES ('8','0','TestRecord',null,'["LOGIN", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2018-11-28 16:38:15',null);
INSERT INTO `access` VALUES ('9','0','Test',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:15',null);
INSERT INTO `access` VALUES ('10','1','PgAttribute',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:14',null);
INSERT INTO `access` VALUES ('11','1','PgClass',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:14',null);
INSERT INTO `access` VALUES ('12','0','Login',null,'[]','[]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[ "ADMIN"]','[ "ADMIN"]','["ADMIN"]','2018-11-28 16:29:48',null);
INSERT INTO `access` VALUES ('13','0','Verify',null,'[]','[]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[ "ADMIN"]','["ADMIN"]','2018-11-28 16:29:48',null);
INSERT INTO `access` VALUES ('14','0','apijson_user','User','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN","LOGIN","OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["ADMIN"]','2018-11-28 16:28:53',null);
INSERT INTO `access` VALUES ('15','0','apijson_privacy','Privacy','[]','[]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["UNKNOWN","LOGIN","OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["ADMIN"]','2018-11-28 16:29:48',null);
INSERT INTO `access` VALUES ('16','0','Moment',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2018-11-28 16:29:19',null);
INSERT INTO `access` VALUES ('17','0','Comment',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2018-11-28 16:29:19',null);
INSERT INTO `access` VALUES ('19','1','SysTable',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2019-10-04 01:01:20',null);
INSERT INTO `access` VALUES ('20','1','SysColumn',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2019-10-04 01:01:20',null);
INSERT INTO `access` VALUES ('21','1','ExtendedProperty',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2019-10-04 01:33:45',null);
INSERT INTO `access` VALUES ('22','1','Random',null,'["LOGIN", "ADMIN"]','["LOGIN", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["LOGIN", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2019-12-01 15:13:13',null);
INSERT INTO `access` VALUES ('23','1','Swagger',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','[]','[]','[]','2018-11-28 16:38:15',null);
INSERT INTO `access` VALUES ('24','0','Method',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["LOGIN", "ADMIN"]','["OWNER", "ADMIN"]','2018-11-28 16:38:15',null);
INSERT INTO `access` VALUES ('25','1','Input',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2020-01-10 07:12:49',null);
INSERT INTO `access` VALUES ('26','1','Device',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2020-01-10 07:12:49',null);
INSERT INTO `access` VALUES ('27','1','System',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2020-01-10 07:12:49',null);
INSERT INTO `access` VALUES ('28','1','Flow',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2020-01-10 07:12:49',null);
INSERT INTO `access` VALUES ('29','1','Output',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2020-01-10 07:12:49',null);
INSERT INTO `access` VALUES ('30','1','ViewTable',null,'["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','["OWNER", "ADMIN"]','2021-01-20 14:51:43','MySQL View 支持多张表合并为一张表，这个示例 View 的创建 SQL 为 CREATE VIEW sys.`ViewTable` AS SELECT C.id as `commentId`, C.toId, C.momentId, C.content,  U.* FROM sys.Comment AS C INNER JOIN sys.apijson_user AS U ON U.id = C.userId; 初测正则匹配等条件单表查询、与其它表关联查询、与其它表 JOIN 都和普通的表用起来没有大的区别，目前发现的问题两个是： 1.作为 ViewTable 的表中有同名字段的话要把部分字段取别名，例如 Comment 和 apijson_user 都有 id，这里就用 C.id as `commentId` 取了别名避免冲突；2.APIAuto 不能显示 ViewTable 的表注释和字段注释');
-- extend from 1001
INSERT INTO `access` VALUES ('1001', '0', 'District', NULL, '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[]', '[]', '[]', '2020-08-24 21:29:55');
INSERT INTO `access` VALUES ('1002', '0', 'Ecdict', NULL, '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[]', '[]', '[]', '2020-08-24 21:29:55');
INSERT INTO `access` VALUES ('1003', '0', 'Province', NULL, '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[]', '[]', '[]', '2020-08-24 21:29:55');
INSERT INTO `access` VALUES ('1004', '0', 'City', NULL, '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[]', '[]', '[]', '2020-08-24 21:29:55');
INSERT INTO `access` VALUES ('1005', '0', 'County', NULL, '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[]', '[]', '[]', '2020-08-24 21:29:55');
INSERT INTO `access` VALUES ('1006', '0', 'Town', NULL, '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[]', '[]', '[]', '2020-08-24 21:29:55');
INSERT INTO `access` VALUES ('1007', '0', 'Village', NULL, '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"UNKNOWN\", \"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[\"LOGIN\", \"CONTACT\", \"CIRCLE\", \"OWNER\", \"ADMIN\"]', '[]', '[]', '[]', '2020-08-24 21:29:55');

-- ----------------------------
-- Table structure for apijson_privacy
-- ----------------------------
DROP TABLE IF EXISTS `apijson_privacy`;
CREATE TABLE `apijson_privacy` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `certified` tinyint(2) NOT NULL DEFAULT '0' COMMENT '已认证',
  `phone` bigint(11) NOT NULL COMMENT '手机号，仅支持 11 位数的。不支持 +86 这种国家地区开头的。如果要支持就改为 VARCHAR(14)',
  `balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '余额',
  `_password` varchar(20) NOT NULL COMMENT '登录密码',
  `_payPassword` int(6) NOT NULL DEFAULT '123456' COMMENT '支付密码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone_UNIQUE` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户隐私信息表。\n对安全要求高，不想泄漏真实名称。对外名称为 Privacy';

-- ----------------------------
-- Records of apijson_privacy
-- ----------------------------
INSERT INTO `apijson_privacy` VALUES ('38710', '1', '13000038710', '33376.00', '666666', '123456');
INSERT INTO `apijson_privacy` VALUES ('70793', '0', '13000070793', '56000.00', 'apijson', '123456');
INSERT INTO `apijson_privacy` VALUES ('82001', '1', '13000082001', '99867.38', '123456', '123456');

-- ----------------------------
-- Table structure for apijson_user
-- ----------------------------
DROP TABLE IF EXISTS `apijson_user`;
CREATE TABLE `apijson_user` (
  `id` bigint(15) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `sex` tinyint(2) NOT NULL DEFAULT '0' COMMENT '性别：\n0-男\n1-女',
  `name` varchar(20) NOT NULL COMMENT '名称',
  `tag` varchar(45) DEFAULT NULL COMMENT '标签',
  `head` varchar(300) DEFAULT 'https://raw.githubusercontent.com/TommyLemon/StaticResources/master/APIJSON_Logo.png' COMMENT '头像url',
  `contactIdList` mediumtext COMMENT '联系人id列表',
  `pictureList` mediumtext COMMENT '照片列表',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1571629309521 DEFAULT CHARSET=utf8 COMMENT='用户公开信息表。\n对安全要求高，不想泄漏真实名称。对外名称为 User';

-- ----------------------------
-- Records of apijson_user
-- ----------------------------
INSERT INTO `apijson_user` VALUES ('38710', '0', 'TommyLemon', 'Android&Java', 'http://static.oschina.net/uploads/user/1218/2437072_100.jpg?t=1461076033000', '[70793]', '[\"http://static.oschina.net/uploads/user/1218/2437072_100.jpg?t=1461076033000\", \"http://common.cnblogs.com/images/icon_weibo_24.png\"]', '2017-02-01 19:21:50');
INSERT INTO `apijson_user` VALUES ('70793', '0', 'Strong', 'djdj', 'http://static.oschina.net/uploads/user/585/1170143_50.jpg?t=1390226446000', '[38710]', '[\"http://static.oschina.net/uploads/img/201604/22172508_eGDi.jpg\"]', '2017-02-01 19:21:50');
INSERT INTO `apijson_user` VALUES ('82001', '0', '测试账号', 'Dev', 'https://static.oschina.net/uploads/user/19/39085_50.jpg', '[93793]', '[\"http://common.cnblogs.com/images/icon_weibo_24.png\"]', '2017-02-01 19:21:50');

-- ----------------------------
-- Table structure for moment
-- ----------------------------
DROP TABLE IF EXISTS `moment`;
CREATE TABLE `moment` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `userId` bigint(15) NOT NULL COMMENT '用户id',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  `content` varchar(300) DEFAULT NULL COMMENT '内容',
  `praiseUserIdList` mediumtext NOT NULL COMMENT '点赞的用户id列表',
  `pictureList` mediumtext NOT NULL COMMENT '图片列表',
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`,`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='动态';

INSERT INTO `Moment` VALUES (12,38710,'2017-02-08 08:06:11','APIJSON,let interfaces and documents go to hell !','[38710]','[\"http://static.oschina.net/uploads/user/1218/2437072_100.jpg?t=1461076033000\"]');

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `toId` bigint(15) NOT NULL DEFAULT '0' COMMENT '被回复的id',
  `userId` bigint(15) NOT NULL COMMENT '评论人 User 的 id',
  `momentId` bigint(15) NOT NULL COMMENT '动态id',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  `content` varchar(1000) NOT NULL COMMENT '内容',
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `momentId` (`momentId`),
  KEY `toId` (`toId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='评论';

INSERT INTO `Comment` VALUES (4,0,38710,470,'2017-02-01 11:20:50','This is a Content...-4');
INSERT INTO `Comment` VALUES (175,0,38710,12,'2017-03-25 12:26:53','Java is the best program language!');

-- ----------------------------
-- Table structure for document
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `Document` (
  `id` bigint(15) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `userId` bigint(15) NOT NULL COMMENT '管理员用户id。\n需要先建Admin表，新增登录等相关接口。',
  `testAccountId` bigint(15) NOT NULL DEFAULT '0' COMMENT '测试账号id。0-不限',
  `version` tinyint(4) NOT NULL DEFAULT '3' COMMENT '接口版本号\n<=0 - 不限制版本，任意版本都可用这个接口；\n>0 - 在这个版本添加的接口。\n\n可在给新版文档前调高默认值，新增的测试用例就不用手动设置版本号了。',
  `name` varchar(100) NOT NULL COMMENT '接口名称',
  `type` varchar(5) NOT NULL DEFAULT 'JSON' COMMENT 'PARAM - GET  url parameters,\nFORM - POST  application/www-x-form-url-encoded,\nJSON - POST  application/json',
  `url` varchar(250) NOT NULL COMMENT '请求地址',
  `request` text NOT NULL COMMENT '请求\n用json格式会导致强制排序，而请求中引用赋值只能引用上面的字段，必须有序。',
  `standard` text,
  `header` text COMMENT '请求头 Request Header：\nkey: value  //注释',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1612031683616 DEFAULT CHARSET=utf8 COMMENT='测试用例文档\n后端开发者在测试好后，把选好的测试用例上传，这样就能共享给前端/客户端开发者';

-- ----------------------------
-- Table structure for function
-- ----------------------------
DROP TABLE IF EXISTS `function`;
CREATE TABLE `function` (
  `id` bigint(15) NOT NULL AUTO_INCREMENT,
  `userId` bigint(15) NOT NULL COMMENT '管理员用户Id',
  `name` varchar(50) NOT NULL COMMENT '方法名',
  `arguments` varchar(100) DEFAULT NULL COMMENT '参数列表，每个参数的类型都是 String。\n用 , 分割的字符串 比 [JSONArray] 更好，例如 array,item ，更直观，还方便拼接函数。',
  `demo` mediumtext NOT NULL COMMENT '可用的示例。\nTODO 改成 call，和返回值示例 back 对应。',
  `detail` varchar(1000) NOT NULL COMMENT '详细描述',
  `type` varchar(50) NOT NULL DEFAULT 'Object' COMMENT '返回值类型。TODO RemoteFunction 校验 type 和 back',
  `version` tinyint(4) NOT NULL DEFAULT '0' COMMENT '允许的最低版本号，只限于GET,HEAD外的操作方法。\nTODO 使用 requestIdList 替代 version,tag,methods',
  `tag` varchar(20) DEFAULT NULL COMMENT '允许的标签.\nnull - 允许全部\nTODO 使用 requestIdList 替代 version,tag,methods',
  `methods` varchar(50) DEFAULT NULL COMMENT '允许的操作方法。\nnull - 允许全部\nTODO 使用 requestIdList 替代 version,tag,methods',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `back` varchar(45) DEFAULT NULL COMMENT '返回值示例',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8 COMMENT='远程函数。强制在启动时校验所有demo是否能正常运行通过';

-- ----------------------------
-- Records of function
-- ----------------------------
INSERT INTO `function` VALUES ('3', '0', 'countArray', 'array', '{\"array\": [1, 2, 3]}', '获取数组长度。没写调用键值对，会自动补全 \"result()\": \"countArray(array)\"', 'Object', '0', null, null, '2018-10-13 16:23:23', null);
INSERT INTO `function` VALUES ('4', '0', 'countObject', 'object', '{\"object\": {\"key0\": 1, \"key1\": 2}}', '获取对象长度。', 'Object', '0', null, null, '2018-10-13 16:23:23', null);
INSERT INTO `function` VALUES ('5', '0', 'isContain', 'array,value', '{\"array\": [1, 2, 3], \"value\": 2}', '判断是否数组包含值。', 'Object', '0', null, null, '2018-10-13 16:23:23', null);
INSERT INTO `function` VALUES ('6', '0', 'isContainKey', 'object,key', '{\"key\": \"id\", \"object\": {\"id\": 1}}', '判断是否对象包含键。', 'Object', '0', null, null, '2018-10-13 16:30:31', null);
INSERT INTO `function` VALUES ('7', '0', 'isContainValue', 'object,value', '{\"value\": 1, \"object\": {\"id\": 1}}', '判断是否对象包含值。', 'Object', '0', null, null, '2018-10-13 16:30:31', null);
INSERT INTO `function` VALUES ('8', '0', 'getFromArray', 'array,position', '{\"array\": [1, 2, 3], \"result()\": \"getFromArray(array,1)\"}', '根据下标获取数组里的值。position 传数字时直接作为值，而不是从所在对象 request 中取值', 'Object', '0', null, null, '2018-10-13 16:30:31', null);
INSERT INTO `function` VALUES ('9', '0', 'getFromObject', 'object,key', '{\"key\": \"id\", \"object\": {\"id\": 1}}', '根据键获取对象里的值。', 'Object', '0', null, null, '2018-10-13 16:30:31', null);
INSERT INTO `function` VALUES ('10', '0', 'deleteCommentOfMoment', 'momentId', '{\"momentId\": 1}', '根据动态 id 删除它的所有评论', 'Object', '0', 'Moment', 'DELETE', '2019-08-18 02:46:56', null);
INSERT INTO `function` VALUES ('11', '0', 'verifyIdList', 'array', '{\"array\": [1, 2, 3], \"result()\": \"verifyIdList(array)\"}', '校验类型为 id 列表', 'Object', '0', null, null, '2019-08-18 03:58:33', null);
INSERT INTO `function` VALUES ('12', '0', 'verifyURLList', 'array', '{\"array\": [\"http://123.com/1.jpg\", \"http://123.com/a.png\", \"http://www.abc.com/test.gif\"], \"result()\": \"verifyURLList(array)\"}', '校验类型为 URL 列表', 'Object', '0', null, null, '2019-08-18 03:58:33', null);
INSERT INTO `function` VALUES ('13', '0', 'getWithDefault', 'value,defaultValue', '{\"value\": null, \"defaultValue\": 1}', '如果 value 为 null，则返回 defaultValue', 'Object', '0', null, null, '2019-08-20 23:26:36', null);
INSERT INTO `function` VALUES ('14', '0', 'removeKey', 'key', '{\"key\": \"s\", \"key2\": 2}', '从对象里移除 key', 'Object', '0', null, null, '2019-08-20 23:26:36', null);
INSERT INTO `function` VALUES ('15', '0', 'getFunctionDemo', null, '{}', '获取远程函数的 Demo', 'Object', '0', null, null, '2019-08-20 23:26:36', null);
INSERT INTO `function` VALUES ('16', '0', 'getFunctionDetail', null, '{}', '获取远程函数的详情', 'Object', '0', null, null, '2019-08-20 23:26:36', null);
INSERT INTO `function` VALUES ('17', '0', 'getMethodArguments', 'methodArgs', '{\"methodArgs\": \"methodArgs\"}', '获取方法参数', 'Object', '0', null, null, '2020-04-06 01:44:07', null);
INSERT INTO `function` VALUES ('18', '0', 'getMethodDefination', 'method,arguments,type,exceptions,language', '{\"method\": \"method\"}', '获取方法定义', 'Object', '0', null, null, '2020-04-06 01:44:07', null);
INSERT INTO `function` VALUES ('19', '0', 'getMethodRequest', null, '{}', '获取方法请求', 'Object', '0', null, null, '2020-04-06 01:44:07', null);

INSERT INTO `function` VALUES ('1001', '0', 'pinyinSort', 'object,key', '{\"object\":\"Province[]\",\"key\":\"name\"}', '根据name拼音排序', 'Object', '0', null, null, '2021-03-21 21:49:18', null);

-- ----------------------------
-- Table structure for login
-- ----------------------------
DROP TABLE IF EXISTS `login`;
CREATE TABLE `login` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `userId` bigint(15) NOT NULL COMMENT '用户id',
  `type` tinyint(2) NOT NULL COMMENT '类型\n0-密码登录\n1-验证码登录',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='@deprecated，登录信息存session';

-- ----------------------------
-- Table structure for method
-- ----------------------------
DROP TABLE IF EXISTS `method`;
CREATE TABLE `Method` (
  `id` bigint(15) NOT NULL AUTO_INCREMENT,
  `userId` bigint(15) NOT NULL COMMENT '管理员用户Id',
  `package` varchar(500) NOT NULL DEFAULT 'apijson/demo/server',
  `class` varchar(50) NOT NULL DEFAULT 'DemoFunction',
  `classArgs` text COMMENT '[\n        {   //User user\n            "type": "apijson/demo/server/model/User",\n            "value": {\n                "id": 1,\n                "name": "APIJSON"\n            }\n        },\n        {   //String id，不填 type 的话会根据 value 自动确定\n            "value": "id"\n        }\n]',
  `genericClassArgs` text,
  `method` varchar(50) NOT NULL COMMENT '方法名',
  `methodArgs` text COMMENT '参数列表，格式是\n[\n        {   //User user\n            "type": "apijson/demo/server/model/User",\n            "value": {\n                "id": 1,\n                "name": "APIJSON"\n            }\n        },\n        {   //String id，不填 type 的话会根据 value 自动确定\n            "value": "id"\n        }\n]',
  `genericMethodArgs` text,
  `exceptions` varchar(500) DEFAULT NULL COMMENT '异常类型列表',
  `genericExceptions` varchar(500) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL COMMENT '返回值类型。TODO RemoteMethod 校验 type 和 back',
  `genericType` varchar(50) DEFAULT NULL,
  `static` tinyint(2) NOT NULL DEFAULT '0' COMMENT '是否为 static 方法： 0-false; 1-true',
  `ui` tinyint(2) NOT NULL DEFAULT '0' COMMENT '0-非 UI 方法，1 - UI 方法',
  `request` text,
  `demo` json DEFAULT NULL COMMENT '可用的示例。\nTODO 改成 call，和返回值示例 back 对应。',
  `detail` varchar(1000) NOT NULL COMMENT '详细描述',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1604152910193 DEFAULT CHARSET=utf8 COMMENT='要做单元测试的方法(如果用到单元测试，则必须)';

insert into method(id,userId,package,class,classArgs,genericClassArgs,method,methodArgs,genericMethodArgs,exceptions,genericExceptions,type,genericType,static,ui,request,demo,detail,date) values('1592232543066','82001','unitauto.test','TestUtil',null,null,'pow','[{"type":"double","value":117},{"type":"double","value":16}]','[{"type":"double","value":-1.8},{"type":"double","value":-40.27}]',null,null,'double','double','1','0',null,null,'pow','2020-06-15 14:49:02');
insert into method(id,userId,package,class,classArgs,genericClassArgs,method,methodArgs,genericMethodArgs,exceptions,genericExceptions,type,genericType,static,ui,request,demo,detail,date) values('1592232544336','82001','unitauto.test','TestUtil',null,null,'plus','[{"type":"long","value":43},{"type":"long","value":178}]','[{"type":"long","value":32},{"type":"long","value":30}]',null,null,'long','long','1','0',null,null,'plus','2020-06-15 14:49:04');

-- ----------------------------
-- Table structure for praise
-- ----------------------------
DROP TABLE IF EXISTS `praise`;
CREATE TABLE `praise` (
  `id` bigint(15) NOT NULL AUTO_INCREMENT COMMENT '动态id',
  `momentId` bigint(15) NOT NULL COMMENT '唯一标识',
  `userId` bigint(15) NOT NULL COMMENT '用户id',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8 COMMENT='如果对Moment写安全要求高，可以将Moment内praiserUserIdList分离到Praise表中，作为userIdList。\n权限注解也改下：\n@MethodAccess(\n		PUT = {OWNER, ADMIN}\n		)\nclass Moment {\n       …\n}\n\n@MethodAccess(\n		PUT = {LOGIN, CONTACT, CIRCLE, OWNER, ADMIN}\n		)\n class Praise {\n       …\n }\n';

-- ----------------------------
-- Table structure for random
-- ----------------------------
DROP TABLE IF EXISTS `random`;
CREATE TABLE `random` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `toId` bigint(15) NOT NULL DEFAULT '0' COMMENT '父项 id',
  `userId` bigint(15) NOT NULL,
  `documentId` bigint(15) NOT NULL COMMENT '测试用例 Document 的 id',
  `count` int(3) NOT NULL DEFAULT '1' COMMENT '请求次数，默认 1',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `config` varchar(5000) NOT NULL COMMENT '配置',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='随机测试(必须)';

-- ----------------------------
-- Table structure for request
-- ----------------------------
DROP TABLE IF EXISTS `request`;
CREATE TABLE `request` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `version` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'GET,HEAD可用任意结构访问任意开放内容，不需要这个字段。\n其它的操作因为写入了结构和内容，所以都需要，按照不同的version选择对应的structure。\n\n自动化版本管理：\nRequest JSON最外层可以传  “version”:Integer 。\n1.未传或 <= 0，用最新版。 “@order”:”version-“\n2.已传且 > 0，用version以上的可用版本的最低版本。 “@order”:”version+”, “version{}”:”>={version}”',
  `method` varchar(10) DEFAULT 'GETS' COMMENT '只限于GET,HEAD外的操作方法。',
  `tag` varchar(20) NOT NULL COMMENT '标签',
  `structure` mediumtext NOT NULL COMMENT '结构。\nTODO 里面的 PUT 改为 UPDATE，避免和请求 PUT 搞混。',
  `detail` varchar(10000) DEFAULT NULL COMMENT '详细说明',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='请求参数校验配置(必须)。\n最好编辑完后删除主键，这样就是只读状态，不能随意更改。需要更改就重新加上主键。\n\n每次启动服务器时加载整个表到内存。\n这个表不可省略，model内注解的权限只是客户端能用的，其它可以保证即便服务端代码错误时也不会误删数据。';
-- ----------------------------
-- Records of request
-- ----------------------------
INSERT INTO `request` VALUES ('1','1','POST','register','{"User": {"MUST": "name", "REFUSE": "id", "UPDATE": {"id@": "Privacy/id"}}, "Privacy": {"MUST": "_password,phone", "REFUSE": "id", "UNIQUE": "phone", "VERIFY": {"phone~": "PHONE"}}}','UNIQUE校验phone是否已存在。VERIFY校验phone是否符合手机号的格式','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('2','1','POST','Moment','{"INSERT": {"@role": "OWNER", "pictureList": [], "praiseUserIdList": []}, "REFUSE": "id", "UPDATE": {"verifyIdList-()": "verifyIdList(praiseUserIdList)", "verifyURLList-()": "verifyURLList(pictureList)"}}','INSERT当没传pictureList和praiseUserIdList时用空数组[]补全，保证不会为null','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('3','1','POST','Comment','{"MUST": "momentId,content", "REFUSE": "id", "UPDATE": {"@role": "OWNER"}}','必须传userId,momentId,content，不允许传id','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('4','1','PUT','User','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "phone"}','必须传id，不允许传phone。INSERT当没传@role时用OWNER补全','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('5','1','DELETE','Moment','{"Moment": {"MUST": "id", "INSERT": {"@role": "OWNER"}, "UPDATE": {"commentCount()": "deleteCommentOfMoment(id)"}}}',null,'2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('6','1','DELETE','Comment','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "UPDATE": {"childCount()": "deleteChildComment(id)"}}','disallow没必要用于DELETE','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('8','1','PUT','User-phone','{"User": {"MUST": "id,phone,_password", "INSERT": {"@role": "OWNER"}, "REFUSE": "!", "UPDATE": {"@combine": "_password"}}}','! 表示其它所有，这里指necessary所有未包含的字段','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('14','1','POST','Verify','{"MUST": "phone,verify", "REFUSE": "!"}','必须传phone,verify，其它都不允许传','2017-02-18 14:20:43');
INSERT INTO `request` VALUES ('15','1','GETS','Verify','{"MUST": "phone"}','必须传phone','2017-02-18 14:20:43');
INSERT INTO `request` VALUES ('16','1','HEADS','Verify','{}','允许任意内容','2017-02-18 14:20:43');
INSERT INTO `request` VALUES ('17','1','PUT','Moment','{"MUST": "id", "REFUSE": "userId,date", "UPDATE": {"verifyIdList-()": "verifyIdList(praiseUserIdList)", "verifyURLList-()": "verifyURLList(pictureList)"}}',null,'2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('21','1','HEADS','Login','{"MUST": "userId,type", "REFUSE": "!"}',null,'2017-02-18 14:20:43');
INSERT INTO `request` VALUES ('22','1','GETS','User','{}','允许传任何内容，除了表对象','2017-02-18 14:20:43');
INSERT INTO `request` VALUES ('23','1','PUT','Privacy','{"MUST": "id", "INSERT": {"@role": "OWNER"}}','INSERT当没传@role时用OWNER补全','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('25','1','PUT','Praise','{"MUST": "id"}','必须传id','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('26','1','DELETE','Comment[]','{"Comment": {"MUST": "id{}", "INSERT": {"@role": "OWNER"}}}','DISALLOW没必要用于DELETE','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('27','1','PUT','Comment[]','{"Comment": {"MUST": "id{}", "INSERT": {"@role": "OWNER"}}}','DISALLOW没必要用于DELETE','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('28','1','PUT','Comment','{"MUST": "id", "INSERT": {"@role": "OWNER"}}','这里省略了Comment，因为tag就是Comment，Parser.getCorrectRequest会自动补全','2017-02-01 11:19:51');
INSERT INTO `request` VALUES ('29','1','GETS','login','{"Privacy": {"MUST": "phone,_password", "REFUSE": "id"}}',null,'2017-10-15 10:04:52');
INSERT INTO `request` VALUES ('30','1','PUT','balance+','{"Privacy": {"MUST": "id,balance+", "REFUSE": "!", "VERIFY": {"balance+&{}": ">=1,<=100000"}}}','验证balance+对应的值是否满足>=1且<=100000','2017-10-21 08:48:34');
INSERT INTO `request` VALUES ('31','1','PUT','balance-','{"Privacy": {"MUST": "id,balance-,_password", "REFUSE": "!", "UPDATE": {"@combine": "_password"}, "VERIFY": {"balance-&{}": ">=1,<=10000"}}}','UPDATE强制把_password作为WHERE条件','2017-10-21 08:48:34');
INSERT INTO `request` VALUES ('32','2','GETS','Privacy','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "_password,_payPassword"}',null,'2017-06-12 16:05:51');
INSERT INTO `request` VALUES ('33','2','GETS','Privacy-CIRCLE','{"Privacy": {"MUST": "id", "REFUSE": "!", "UPDATE": {"@role": "CIRCLE", "@column": "phone"}}}',null,'2017-06-12 16:05:51');
INSERT INTO `request` VALUES ('35','2','POST','Document','{"Document": {"MUST": "name,url,request", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}, "TestRecord": {"MUST": "response", "INSERT": {"@role": "OWNER"}, "REFUSE": "id,documentId", "UPDATE": {"documentId@": "Document/id"}}}',null,'2017-11-26 08:34:41');
INSERT INTO `request` VALUES ('36','2','PUT','Document','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "userId"}',null,'2017-11-26 08:35:15');
INSERT INTO `request` VALUES ('37','2','DELETE','Document','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "!", "UPDATE": {"Random": {"@role": "OWNER", "documentId@": "Method/id"}, "TestRecord": {"@role": "OWNER", "documentId@": "Document/id"}}}',null,'2017-11-26 00:36:20');
INSERT INTO `request` VALUES ('38','2','POST','TestRecord','{"MUST": "documentId,response", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}',null,'2018-06-16 23:44:36');
INSERT INTO `request` VALUES ('39','2','POST','Method','{"Method": {"MUST": "method,class,package", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}, "TestRecord": {"MUST": "response", "INSERT": {"@role": "OWNER"}, "REFUSE": "id,documentId", "UPDATE": {"documentId@": "Method/id"}}}',null,'2017-11-26 00:34:41');
INSERT INTO `request` VALUES ('40','2','PUT','Method','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "userId"}',null,'2017-11-26 00:35:15');
INSERT INTO `request` VALUES ('41','2','DELETE','Method','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "!", "UPDATE": {"Random": {"@role": "OWNER", "documentId@": "Method/id"}, "TestRecord": {"@role": "OWNER", "documentId@": "Method/id"}}}',null,'2017-11-25 16:36:20');
INSERT INTO `request` VALUES ('42','2','POST','Random','{"INSERT": {"@role": "OWNER"}, "Random": {"MUST": "documentId,name,config"}, "TestRecord": {"UPDATE": {"randomId@": "/Random/id", "documentId@": "/Random/documentId"}}}',null,'2017-11-26 00:34:41');
INSERT INTO `request` VALUES ('43','2','PUT','Random','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "userId"}',null,'2017-11-26 00:35:15');
INSERT INTO `request` VALUES ('44','2','DELETE','Random','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "UPDATE": {"TestRecord": {"@role": "OWNER", "randomId@": "/id"}}}',null,'2017-11-25 16:36:20');
INSERT INTO `request` VALUES ('45','2','POST','Comment:[]','{"TYPE": {"Comment[]": "OBJECT[]"}, "INSERT": {"@role": "OWNER"}, "Comment[]": []}',null,'2020-03-01 05:40:04');
INSERT INTO `request` VALUES ('46','2','POST','Moment:[]','{"INSERT": {"@role": "OWNER"}, "Moment[]": []}',null,'2020-03-01 05:41:42');
INSERT INTO `request` VALUES ('47','2','PUT','Comment:[]','{"INSERT": {"@role": "OWNER"}, "Comment[]": []}',null,'2020-03-01 05:40:04');
INSERT INTO `request` VALUES ('48','2','DELETE','TestRecord','{"MUST": "id", "INSERT": {"@role": "OWNER"}}',null,'2017-11-25 16:36:20');
INSERT INTO `request` VALUES ('49','2','POST','Input','{"MUST": "deviceId,x,y", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}',null,'2017-11-26 00:34:41');
INSERT INTO `request` VALUES ('50','2','POST','Device','{"MUST": "brand,model", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}',null,'2017-11-26 00:34:41');
INSERT INTO `request` VALUES ('51','2','POST','System','{"MUST": "type,versionCode,versionName", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}',null,'2017-11-26 00:34:41');
INSERT INTO `request` VALUES ('52','2','POST','Flow','{"MUST": "deviceId,systemId,name", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}',null,'2017-11-26 00:34:41');
INSERT INTO `request` VALUES ('53','4','GETS','Privacy','{"MUST": "id", "INSERT": {"@role": "OWNER"}, "REFUSE": "!"}',null,'2017-06-12 16:05:51');
INSERT INTO `request` VALUES ('54','2','POST','Output','{"MUST": "inputId", "INSERT": {"@role": "OWNER"}, "REFUSE": "id"}',null,'2018-06-16 23:44:36');
INSERT INTO `request` VALUES ('55','2','DELETE','Output','{"MUST": "id", "INSERT": {"@role": "OWNER"}}',null,'2017-11-25 16:36:20');

-- ----------------------------
-- Table structure for response
-- ----------------------------
DROP TABLE IF EXISTS `response`;
CREATE TABLE `response` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `method` varchar(10) DEFAULT 'GET' COMMENT '方法',
  `model` varchar(20) NOT NULL COMMENT '表名，table是SQL关键词不能用',
  `structure` mediumtext NOT NULL COMMENT '结构',
  `detail` varchar(10000) DEFAULT NULL COMMENT '详细说明',
  `date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='返回结果处理配置(目前未用上)\n每次启动服务器时加载整个表到内存。';

-- ----------------------------
-- Records of response
-- ----------------------------
INSERT INTO `response` VALUES ('1', 'GET', 'User', '{\"put\": {\"extra\": \"Response works! Test:He(She) is lazy and wrote nothing here\"}, \"remove\": \"phone\"}', null, '2017-05-22 20:36:47');
INSERT INTO `response` VALUES ('2', 'DELETE', 'Comment', '{\"remove\": \"Comment:child\"}', null, '2017-05-04 01:51:26');
INSERT INTO `response` VALUES ('3', 'DELETE', 'Moment', '{\"remove\": \"Comment\"}', null, '2017-05-04 01:51:26');

-- ----------------------------
-- Table structure for test
-- ----------------------------
DROP TABLE IF EXISTS `test`;
CREATE TABLE `test` (
  `id` tinyint(2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='测试及验证用的表，可以用 SELECT condition替代 SELECT * FROM Test WHERE condition，这样就不需要这张表了';

-- ----------------------------
-- Records of test
-- ----------------------------
INSERT INTO `test` VALUES ('1');

-- ----------------------------
-- Table structure for testrecord
-- ----------------------------
DROP TABLE IF EXISTS `testrecord`;
CREATE TABLE `TestRecord` (
  `id` bigint(15) NOT NULL COMMENT '唯一标识',
  `userId` bigint(15) NOT NULL COMMENT '用户id',
  `testAccountId` bigint(15) NOT NULL DEFAULT '0',
  `documentId` bigint(15) NOT NULL COMMENT '测试用例文档id',
  `randomId` bigint(15) NOT NULL DEFAULT '0' COMMENT '随机配置 id',
  `host` varchar(1000) DEFAULT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  `duration` bigint(15) DEFAULT NULL COMMENT '服务处理耗时，单位为毫秒(ms)',
  `minDuration` bigint(15) DEFAULT NULL COMMENT '统计过的最短服务处理耗时，单位为毫秒(ms)',
  `maxDuration` bigint(15) DEFAULT NULL COMMENT '统计过的最长服务处理耗时，单位为毫秒(ms)',
  `response` text NOT NULL COMMENT '接口返回结果JSON',
  `compare` text COMMENT '对比结果',
  `standard` text COMMENT 'response 的校验标准，是一个 JSON 格式的 AST ，描述了正确 Response 的结构、里面的字段名称、类型、长度、取值范围 等属性。',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='测试记录(必须)\n主要用于保存自动化接口回归测试。5.0.0 可能改为 Test(先废弃 Test 表) ';

-- ----------------------------
-- Table structure for verify
-- ----------------------------
DROP TABLE IF EXISTS `verify`;
CREATE TABLE `verify` (
  `id` bigint(15) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `type` int(2) NOT NULL DEFAULT '0' COMMENT '类型：\n0-登录\n1-注册\n2-修改登录密码\n3-修改支付密码',
  `phone` bigint(11) NOT NULL COMMENT '手机号',
  `verify` int(6) NOT NULL COMMENT '验证码',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1590930499668 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for _visit
-- ----------------------------
DROP TABLE IF EXISTS `_visit`;
CREATE TABLE `_visit` (
  `model` varchar(15) NOT NULL,
  `id` bigint(15) NOT NULL,
  `operate` tinyint(4) NOT NULL COMMENT '1-增\n2-删\n3-改\n4-查',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- user table for beetlsql test
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `name` varchar(64) DEFAULT NULL,
      `age` int(4) DEFAULT NULL,
      `createDate` datetime DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO `user` (`id`, `name`, `age`, `createDate`) VALUES ('1', 'admin', '18', '2020-08-21 23:44:48');

