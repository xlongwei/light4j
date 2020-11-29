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
) ENGINE=InnoDB DEFAULT CHARSET=gbk COMMENT='行政区划';
