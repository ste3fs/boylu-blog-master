set -e
TARGET=/var/www/boylu-blog/admin
SOURCE=/tmp/codex-admin-front-20260419/dist
mkdir -p "$SOURCE"
find "$TARGET" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
cp -a "$SOURCE"/. "$TARGET"/
chown -R www-data:www-data "$TARGET"
nginx -t
systemctl reload nginx
printf 'REMOTE_ADMIN_INDEX_SHA256\n'
sha256sum "$TARGET/index.html"
printf 'REMOTE_ADMIN_INDEX_MTIME\n'
stat -c '%y %n' "$TARGET/index.html"