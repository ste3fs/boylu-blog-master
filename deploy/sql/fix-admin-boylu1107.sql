START TRANSACTION;

SET @admin_role_id := (
    SELECT id
    FROM sys_role
    WHERE code = 'admin'
    LIMIT 1
);

SET @admin_user_id := COALESCE(
    (
        SELECT id
        FROM sys_user
        WHERE username = 'admin'
        LIMIT 1
    ),
    (
        SELECT id
        FROM sys_user
        WHERE username = 'boylu1107'
        LIMIT 1
    )
);

UPDATE sys_user
SET username = 'boylu1107',
    update_time = NOW()
WHERE id = @admin_user_id;

DELETE FROM sys_user_role
WHERE user_id = @admin_user_id;

INSERT INTO sys_user_role (role_id, user_id)
VALUES (@admin_role_id, @admin_user_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, id
FROM sys_menu;

SET @remove_article_id := (
    SELECT id
    FROM sys_article
    WHERE id = 292
       OR title = '个人开发的后台管理系统展示'
    LIMIT 1
);

DELETE FROM sys_article_like
WHERE article_id = @remove_article_id;

DELETE FROM sys_article_tag
WHERE article_id = @remove_article_id;

DELETE FROM sys_comment
WHERE article_id = @remove_article_id;

DELETE FROM sys_notifications
WHERE article_id = @remove_article_id;

DELETE FROM sys_article
WHERE id = @remove_article_id;

COMMIT;

SELECT u.id,
       u.username,
       u.nickname,
       r.code AS role_code
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
LEFT JOIN sys_role r ON r.id = ur.role_id
WHERE u.id = @admin_user_id;

SELECT id, title
FROM sys_article
WHERE id = @remove_article_id;
