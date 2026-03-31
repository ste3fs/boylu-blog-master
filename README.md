# boylu-blog-master

A customized personal blog project for `boylu`.

## Contact

- QQ: `3453619783`
- Email: `3453619783@qq.com`
- WeChat: `a3453619783`

## Project Structure

```text
blog/          Spring Boot backend
blog-web/      Frontend site
blog-admin/    Admin panel
uniapp-blog/   UniApp client
mj-blog.sql    Database initialization script
```

## Local Run

1. Import `mj-blog.sql`.
2. Update backend config in `blog/mojian-server/src/main/resources/application-dev.yml`.
3. Start the backend service on port `8800`.
4. Start `blog-web`.
5. Start `blog-admin`.

## Default Local URLs

- Frontend: `http://localhost:3000`
- Admin: `http://localhost:3001`
- API: `http://localhost:8800`
