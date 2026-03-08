-- 直播间表
CREATE TABLE `live_room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `classId` bigint NOT NULL COMMENT '班级ID',
  `teacherId` bigint NOT NULL COMMENT '教师(主播)ID',
  `title` varchar(200) DEFAULT '班级直播' COMMENT '直播标题',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未开始 1-直播中 2-已结束',
  `streamId` varchar(100) DEFAULT NULL COMMENT 'SRS 流ID',
  `startTime` datetime DEFAULT NULL COMMENT '开始时间',
  `endTime` datetime DEFAULT NULL COMMENT '结束时间',
  `viewerCount` int NOT NULL DEFAULT 0 COMMENT '观看人数',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_classId` (`classId`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间表';
