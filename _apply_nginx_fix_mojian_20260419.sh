set -e
cp /tmp/_boylu-blog.conf.fix-mojian-20260419 /etc/nginx/sites-available/boylu-blog.conf
nginx -t
systemctl reload nginx
curl -I -s http://127.0.0.1/mojian/api/webConfig | head -n 5
curl -I -s http://127.0.0.1/mojian/chat/list | head -n 5