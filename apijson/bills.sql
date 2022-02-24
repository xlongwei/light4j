CREATE TABLE `bills` (
  `_id` char(24) NOT NULL,
  `apiCode` smallint(6) DEFAULT NULL,
  `apiName` varchar(11) DEFAULT NULL,
  `slaveId` char(24) DEFAULT NULL,
  `slaveName` varchar(14) DEFAULT NULL,
  `payTime` datetime DEFAULT NULL,
  `realPayMoney` smallint(6) DEFAULT NULL,
  PRIMARY KEY (`_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='bills';
SELECT  apiCode
       ,apiName
       ,COUNT(*)                 AS num
       ,SUM(realPayMoney)/1000.0 AS sum
FROM bills
GROUP BY  apiCode ORDER BY num desc;