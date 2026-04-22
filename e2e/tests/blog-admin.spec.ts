import { expect, test, type Page } from "@playwright/test";

const ADMIN_TOKEN = process.env.ADMIN_TOKEN || "";
const ADMIN_TOKEN_COOKIE_NAME = process.env.ADMIN_TOKEN_COOKIE_NAME || "boylu-blog-admin-token";

async function injectAdminTokenCookie(page: Page) {
  const url = new URL(page.url());
  await page.context().addCookies([
    {
      name: ADMIN_TOKEN_COOKIE_NAME,
      value: ADMIN_TOKEN,
      domain: url.hostname,
      path: "/",
      httpOnly: false,
      secure: false,
      sameSite: "Lax"
    }
  ]);
}

test.describe("Blog Admin Regression", () => {
  test("后台登录页可正常打开", async ({ page }) => {
    await page.goto("/login");
    await expect(page.locator("input[type='password']").first()).toBeVisible();
    await expect(page.getByRole("button", { name: /登录|登 录|login/i }).first()).toBeVisible();
  });

  test("管理后台首页可访问（需要 ADMIN_TOKEN）", async ({ page }) => {
    test.skip(!ADMIN_TOKEN, "未设置 ADMIN_TOKEN，跳过后台鉴权回归。");
    await page.goto("/");
    await injectAdminTokenCookie(page);
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.locator("body")).toContainText(/仪表|dashboard|统计|概览/i);
  });

  test("相册管理页可访问（需要 ADMIN_TOKEN）", async ({ page }) => {
    test.skip(!ADMIN_TOKEN, "未设置 ADMIN_TOKEN，跳过后台鉴权回归。");
    await page.goto("/");
    await injectAdminTokenCookie(page);
    await page.goto("/site/album");
    await expect(page).toHaveURL(/\/site\/album/);
    await expect(page.locator("body")).toContainText(/相册|照片|album|photo/i);
  });
});
