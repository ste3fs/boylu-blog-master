import { expect, test } from "@playwright/test";

test.describe("Blog Web Regression", () => {
  test("登录页可正常打开", async ({ page }) => {
    await page.goto("/login");
    await expect(page.locator("input[type='password']").first()).toBeVisible();
    await expect(page.getByRole("button", { name: /登录|登 录|login/i }).first()).toBeVisible();
  });

  test("文章页可从首页进入并打开详情", async ({ page }) => {
    await page.goto("/");
    const firstPostLink = page.locator("a[href*='/post/']").first();
    await expect(firstPostLink).toBeVisible();
    await firstPostLink.click();
    await expect(page).toHaveURL(/\/post\/\d+/);
    await expect(page.locator("article, .article, .post-content, .markdown-body").first()).toBeVisible();
  });

  test("相册页可正常加载", async ({ page }) => {
    await page.goto("/photos");
    await expect(page).toHaveURL(/\/photos/);
    await expect(page.locator("body")).toContainText(/相册|照片|photo|gallery/i);
  });
});
