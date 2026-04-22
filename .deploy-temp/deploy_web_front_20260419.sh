set -e
TARGET=/var/www/boylu-blog
SOURCE=/tmp/codex-web-front-20260419/dist
mkdir -p "$SOURCE"
find "$TARGET" -mindepth 1 -maxdepth 1 ! -name admin -exec rm -rf {} +
cp -a "$SOURCE"/. "$TARGET"/
chown -R www-data:www-data "$TARGET"
nginx -t
systemctl reload nginx
printf 'REMOTE_INDEX_SHA256\n'
sha256sum "$TARGET/index.html"
printf 'REMOTE_INDEX_MTIME\n'
stat -c '%y %n' "$TARGET/index.html"