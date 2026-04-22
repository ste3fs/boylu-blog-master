set -e
TARGET=/var/www/boylu-blog/admin
SOURCE=/tmp/codex-admin-mobile-20260419/dist
if [ ! -d "$SOURCE" ]; then
  echo "SOURCE_MISSING: $SOURCE" >&2
  exit 1
fi
rm -rf "$TARGET"/*
cp -a "$SOURCE"/. "$TARGET"/
chown -R www-data:www-data "$TARGET"
nginx -t
systemctl reload nginx
stat -c '%y %n' "$TARGET/index.html"