CREATE TABLE `district` (
  `provinceName` varchar(24) NOT NULL COMMENT '省份名称',
  `province` char(6) NOT NULL COMMENT '省份代码',
  `cityName` varchar(33) NOT NULL COMMENT '城市名称',
  `city` char(6) NOT NULL COMMENT '城市代码',
  `countyName` varchar(45) NOT NULL COMMENT '区县名称',
  `county` char(6) NOT NULL COMMENT '区县代码',
  PRIMARY KEY (`county`),
  KEY `idx_province_city_name` (`province`,`city`,`cityName`),
  KEY `idx_city_county_name` (`city`,`county`,`countyName`),
  KEY `idx_province_name` (`province`,`provinceName`)
) ENGINE=MyISAM DEFAULT CHARSET=gbk COMMENT='行政区划';

CREATE TABLE `ecdict` (
  `word` varchar(64) NOT NULL COMMENT '单词',
  `phonetic` varchar(64) NOT NULL COMMENT '音标',
  PRIMARY KEY (`word`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='英语单词和音标';

CREATE TABLE `province` (
  `code` char(2) NOT NULL COMMENT '代码',
  `name` varchar(255) NOT NULL COMMENT '名称',
  PRIMARY KEY (`code`)
) ENGINE=MyISAM DEFAULT CHARSET=gbk COMMENT='省';
CREATE TABLE `city` (
  `code` char(4) NOT NULL COMMENT '代码',
  `name` varchar(255) NOT NULL COMMENT '名称',
  PRIMARY KEY (`code`)
) ENGINE=MyISAM DEFAULT CHARSET=gbk COMMENT='市';
CREATE TABLE `county` (
  `code` char(6) NOT NULL COMMENT '代码',
  `name` varchar(255) NOT NULL COMMENT '名称',
  PRIMARY KEY (`code`)
) ENGINE=MyISAM DEFAULT CHARSET=gbk COMMENT='县';
CREATE TABLE `town` (
  `code` char(9) NOT NULL COMMENT '代码',
  `name` varchar(255) NOT NULL COMMENT '名称',
  PRIMARY KEY (`code`)
) ENGINE=MyISAM DEFAULT CHARSET=gbk COMMENT='乡';
CREATE TABLE `village` (
  `code` char(12) NOT NULL COMMENT '代码',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `type` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '111主城区，112城乡结合区，121镇中心区，122镇乡结合区，123特殊区域；210乡中心区，220村庄',
  PRIMARY KEY (`code`)
) ENGINE=MyISAM DEFAULT CHARSET=gbk COMMENT='村';

CREATE TABLE `sequence` (
  `name` varchar(64) NOT NULL COMMENT '序列名',
  `value` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '当前值',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='序列';

delimiter //
create function sequence(name varchar(64), step bigint(20))
returns bigint
begin
update sequence t1 ,(select @current_val := `value` from sequence t2 where t2.name=name) t3 
set t1.value = t1.value+step where t1.name=name and t1.value=@current_val;
return @current_val+1;
end //
delimiter ;

CREATE TABLE `bank_card` (
  `cardBin` varchar(12) NOT NULL COMMENT '卡bin码',
  `issuerCode` char(8) NOT NULL DEFAULT '无' COMMENT '发卡行代码',
  `issuerName` varchar(30) NOT NULL DEFAULT '无' COMMENT '发卡行名称',
  `cardName` varchar(30) NOT NULL DEFAULT '无' COMMENT '卡片名称',
  `cardDigits` tinyint(3) unsigned NOT NULL DEFAULT '16' COMMENT '卡号位数',
  `cardType` varchar(10) NOT NULL DEFAULT '无' COMMENT '卡片类型',
  `bankCode` varchar(7) NOT NULL DEFAULT '无' COMMENT '银行代码',
  `bankName` varchar(20) NOT NULL DEFAULT '无' COMMENT '银行名称',
  PRIMARY KEY (`cardBin`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='银行卡信息';
-- BankUtilTest.testMysql()
UPDATE bank_card bc set bc.bankCode='CMBC',bc.bankName='中国民生银行' where bc.issuerName like '%民生%';
UPDATE bank_card bc set bc.bankCode='CEB',bc.bankName='中国光大银行' where bc.issuerName like '%光大%' and bc.bankCode='';
UPDATE bank_card bc set bc.bankCode='SPABANK',bc.bankName='平安银行' where bc.issuerName like '%平安%' and bc.bankCode='';
UPDATE bank_card bc set bc.bankCode='CITIC',bc.bankName='中信银行' where bc.issuerName like '%中信%' and bc.bankCode='';
UPDATE bank_card bc set bc.bankCode='BOC',bc.bankName='中国银行' where bc.issuerName like '%中国银行%' and bc.bankCode='';
UPDATE bank_card bc set bc.bankCode='BOC',bc.bankName='中国银行' where bc.issuerName like '%中银信用卡%' and bc.bankCode='';
UPDATE bank_card bc set bc.bankCode='ZYB',bc.bankName='中原银行' where bc.issuerName like '%中原%' and bc.bankCode='';
UPDATE bank_card bc set bc.bankCode='CSCB',bc.bankName='长沙银行' where bc.issuerName like '%长沙%' and bc.bankCode='';
UPDATE bank_card bc set bc.bankCode='SPDB',bc.bankName='上海浦东发展银行' where bc.issuerName like '%浦发%' and bc.bankCode='';
