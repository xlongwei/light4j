CREATE TABLE `district` (
  `provinceName` varchar(24) NOT NULL COMMENT '省份名称',
  `province` char(6) NOT NULL COMMENT '省份代码',
  `cityName` varchar(33) NOT NULL COMMENT '城市名称',
  `city` char(6) NOT NULL COMMENT '城市代码',
  `countyName` varchar(45) NOT NULL COMMENT '区县名称',
  `county` char(6) NOT NULL COMMENT '区县代码',
  PRIMARY KEY (`county`),
  KEY `idx_city` (`city`) USING BTREE COMMENT '城市索引',
  KEY `idx_province_city` (`province`,`city`) USING BTREE COMMENT '省市联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='行政区划';
