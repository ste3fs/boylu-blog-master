ALTER TABLE `sys_resource`
    ADD COLUMN `cover` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资源封面' AFTER `category`,
    ADD COLUMN `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资源描述' AFTER `cover`;
