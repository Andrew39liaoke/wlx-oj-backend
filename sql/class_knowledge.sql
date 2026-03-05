-- 班级知识库表
CREATE TABLE IF NOT EXISTS `class_knowledge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `classId` BIGINT NOT NULL COMMENT '班级ID',
    `fileInfoId` BIGINT NOT NULL COMMENT '文件信息ID，关联file_info表',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0-未删除, 1-已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_classId` (`classId`),
    INDEX `idx_fileInfoId` (`fileInfoId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级知识库表';
