START TRANSACTION;

SET @site_menu_id := (
    SELECT id
    FROM sys_menu
    WHERE title = '站点管理'
       OR path = '/site'
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_menu (
    parent_id, path, component, title, sort, icon, type,
    redirect, name, hidden, perm, is_external, create_time, update_time
)
SELECT
    0, '/site', 'Layout', '站点管理', 2, 'DeleteLocation', 'CATALOG',
    '/site/resource', 'Site', 0, '', 0, NOW(), NOW()
WHERE @site_menu_id IS NULL;

SET @site_menu_id := COALESCE(@site_menu_id, LAST_INSERT_ID());

UPDATE sys_menu
SET redirect = CASE
        WHEN redirect IS NULL OR redirect = '' THEN '/site/resource'
        ELSE redirect
    END,
    hidden = 0,
    is_external = 0,
    update_time = NOW()
WHERE id = @site_menu_id;

INSERT INTO sys_menu (
    parent_id, path, component, title, sort, icon, type,
    redirect, name, hidden, perm, is_external, create_time, update_time
)
SELECT
    @site_menu_id, 'resource', '/site/resource/index', '资源审核', 6, 'FolderChecked', 'MENU',
    NULL, 'SiteResource', 0, '', 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_menu
    WHERE component = '/site/resource/index'
       OR (parent_id = @site_menu_id AND path = 'resource')
);

SET @resource_menu_id := (
    SELECT id
    FROM sys_menu
    WHERE component = '/site/resource/index'
       OR (parent_id = @site_menu_id AND path = 'resource')
    ORDER BY id
    LIMIT 1
);

UPDATE sys_menu
SET parent_id = @site_menu_id,
    path = 'resource',
    component = '/site/resource/index',
    title = '资源审核',
    sort = 6,
    icon = 'FolderChecked',
    type = 'MENU',
    name = 'SiteResource',
    hidden = 0,
    is_external = 0,
    update_time = NOW()
WHERE id = @resource_menu_id;

INSERT INTO sys_menu (
    parent_id, path, component, title, sort, icon, type,
    redirect, name, hidden, perm, is_external, create_time, update_time
)
SELECT @resource_menu_id, '', '', '资源新增', 1, '', 'BUTTON', NULL, 'ResourceAdd', 0, 'sys:resource:add', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'sys:resource:add');

INSERT INTO sys_menu (
    parent_id, path, component, title, sort, icon, type,
    redirect, name, hidden, perm, is_external, create_time, update_time
)
SELECT @resource_menu_id, '', '', '资源修改/审核', 2, '', 'BUTTON', NULL, 'ResourceUpdate', 0, 'sys:resource:update', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'sys:resource:update');

INSERT INTO sys_menu (
    parent_id, path, component, title, sort, icon, type,
    redirect, name, hidden, perm, is_external, create_time, update_time
)
SELECT @resource_menu_id, '', '', '资源删除', 3, '', 'BUTTON', NULL, 'ResourceDelete', 0, 'sys:resource:delete', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'sys:resource:delete');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m
WHERE r.code = 'admin'
  AND (
      m.id = @resource_menu_id
      OR m.perm IN ('sys:resource:add', 'sys:resource:update', 'sys:resource:delete')
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
  );

COMMIT;

SELECT id, parent_id, path, component, title, sort, type, perm
FROM sys_menu
WHERE id = @resource_menu_id
   OR parent_id = @resource_menu_id
ORDER BY sort, id;
