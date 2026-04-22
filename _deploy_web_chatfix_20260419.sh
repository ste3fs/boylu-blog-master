set -e
TARGET=/var/www/boylu-blog
SOURCE=/tmp/codex-web-chatfix-20260419/dist
find "$TARGET" -mindepth 1 -maxdepth 1 ! -name admin -exec rm -rf {} +
cp -a "$SOURCE"/. "$TARGET"/
chown -R www-data:www-data "$TARGET"
nginx -t
systemctl reload nginx
stat -c '%y %n' "$TARGET/index.html"