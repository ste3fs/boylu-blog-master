UPDATE sys_web_config
SET author_avatar = '/boylu-avatar.jpg'
WHERE id = 1;

UPDATE sys_user
SET avatar = '/boylu-avatar.jpg'
WHERE username IN ('admin', 'boylu1107');
