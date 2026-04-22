UPDATE sys_user
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL
  AND TRIM(email) <> '';

UPDATE sys_user
SET username = CONCAT('user_', id)
WHERE username IS NULL
  OR TRIM(username) = '';

SET @email_duplicate_count = (
  SELECT COUNT(*)
  FROM (
    SELECT LOWER(TRIM(email)) AS normalized_email
    FROM sys_user
    WHERE email IS NOT NULL
      AND TRIM(email) <> ''
    GROUP BY LOWER(TRIM(email))
    HAVING COUNT(*) > 1
  ) duplicate_emails
);

SET @has_email_unique_index = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'uk_sys_user_email'
);

SET @create_email_unique_index_sql = IF(
  @email_duplicate_count = 0 AND @has_email_unique_index = 0,
  'ALTER TABLE sys_user ADD UNIQUE INDEX uk_sys_user_email (email)',
  'SELECT 1'
);

PREPARE stmt FROM @create_email_unique_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
