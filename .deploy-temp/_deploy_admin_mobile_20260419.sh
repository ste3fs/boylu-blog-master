set -e
TMP_DIR=/tmp/codex-admin-mobile-20260419
ZIP_PATH="$TMP_DIR/blog-admin-mobile-20260419.zip"
EXTRACT_DIR="$TMP_DIR/extract"
TARGET_DIR=/var/www/boylu-blog/admin
mkdir -p "$EXTRACT_DIR"
unzip -oq "$ZIP_PATH" -d "$EXTRACT_DIR"
rm -rf "$TARGET_DIR"/*
cp -a "$EXTRACT_DIR"/. "$TARGET_DIR"/
chown -R www-data:www-data "$TARGET_DIR"
nginx -t
systemctl reload nginx
stat -c '%y %n' "$TARGET_DIR/index.html"