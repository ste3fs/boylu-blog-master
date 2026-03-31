UPDATE sys_web_config
SET web_url = 'http://101.43.1.187',
    logo = '/boylu-logo.png',
    author = 'boylu',
    author_avatar = '/boylu-avatar.jpg'
WHERE id = 1;

UPDATE sys_friend
SET url = 'http://101.43.1.187',
    avatar = '/boylu-logo.png'
WHERE id = 4;

UPDATE sys_user
SET avatar = '/boylu-avatar.jpg',
    nickname = 'boylu'
WHERE username IN ('admin', 'boylu1107');

UPDATE sys_file_oss
SET domain = 'http://101.43.1.187/localFile/',
    storage_path = '/opt/boylu-blog/storage/',
    base_path = 'local-plus/',
    path_patterns = 'localFile/**',
    enable_access = 1,
    is_enable = 1
WHERE id = 1;

UPDATE sys_article
SET content = REPLACE(content, 'http://127.0.0.1:8800/localFile/', 'http://101.43.1.187/localFile/'),
    content_md = REPLACE(content_md, 'http://127.0.0.1:8800/localFile/', 'http://101.43.1.187/localFile/');
