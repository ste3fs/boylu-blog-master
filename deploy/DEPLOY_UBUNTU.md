# Ubuntu Deployment

This deployment layout serves:

- frontend at `http://101.43.1.187/`
- admin at `http://101.43.1.187/console-9x7k/login`
- backend API behind `/mojian`
- uploaded files behind `/localFile`

## 1. Server packages

```bash
sudo apt update
sudo apt install -y nginx redis-server mysql-server openjdk-17-jre unzip
```

## 2. Open ports

Open these ports in the Tencent Cloud firewall:

- `22`
- `80`
- `443` if you add HTTPS later

Do not expose `3306`, `6379`, or `8800` publicly.

## 3. Database

```bash
sudo mysql
CREATE DATABASE blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'boylu'@'127.0.0.1' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON blog.* TO 'boylu'@'127.0.0.1';
FLUSH PRIVILEGES;
EXIT;
```

Import the initial data:

```bash
mysql -uboylu -p blog < /opt/boylu-blog/sql/mj-blog.sql
mysql -uboylu -p blog < /opt/boylu-blog/sql/ubuntu-init.sql
```

## 4. Directory layout

```bash
sudo mkdir -p /opt/boylu-blog/server
sudo mkdir -p /opt/boylu-blog/sql
sudo mkdir -p /opt/boylu-blog/storage
sudo mkdir -p /var/www/boylu-blog/admin
sudo mkdir -p /etc/boylu-blog
sudo chown -R www-data:www-data /opt/boylu-blog /var/www/boylu-blog
```

## 5. Upload files

Upload these local outputs to the server:

- `blog-web/dist/*` -> `/var/www/boylu-blog/`
- `blog-admin/dist/*` -> `/var/www/boylu-blog/admin/`
- `blog/mojian-server/target/mojian-blog.jar` -> `/opt/boylu-blog/server/mojian-blog.jar`
- `mj-blog.sql` -> `/opt/boylu-blog/sql/mj-blog.sql`
- `deploy/sql/ubuntu-init.sql` -> `/opt/boylu-blog/sql/ubuntu-init.sql`
- `deploy/systemd/boylu-blog.env.example` -> `/etc/boylu-blog/boylu-blog.env`

Then edit the env file:

```bash
sudo nano /etc/boylu-blog/boylu-blog.env
```

Set at least:

- `PUBLIC_BASE_URL=http://101.43.1.187`
- `DB_USERNAME=boylu`
- `DB_PASSWORD=your_real_password`

## 6. Backend service

```bash
sudo cp /path/to/deploy/systemd/boylu-blog.service /etc/systemd/system/boylu-blog.service
sudo systemctl daemon-reload
sudo systemctl enable boylu-blog
sudo systemctl start boylu-blog
sudo systemctl status boylu-blog
```

View logs:

```bash
sudo journalctl -u boylu-blog -f
```

## 7. Nginx

```bash
sudo cp /path/to/deploy/nginx/boylu-blog.conf /etc/nginx/sites-available/boylu-blog.conf
sudo ln -sf /etc/nginx/sites-available/boylu-blog.conf /etc/nginx/sites-enabled/boylu-blog.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

## 8. Verify

Check:

- `http://101.43.1.187/`
- `http://101.43.1.187/console-9x7k/login`
- `http://101.43.1.187/mojian/api/webConfig`

If uploads do not display, verify:

- `sys_file_oss.domain` is `http://101.43.1.187/localFile/`
- `sys_file_oss.storage_path` is `/opt/boylu-blog/storage/`
- `www-data` can read and write `/opt/boylu-blog/storage/`
