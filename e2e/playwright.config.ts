import { defineConfig } from "@playwright/test";
import dotenv from "dotenv";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({ path: path.join(__dirname, ".env.local") });

const webBaseURL = process.env.WEB_BASE_URL || "http://127.0.0.1:3000";
const adminBaseURL = process.env.ADMIN_BASE_URL || "http://127.0.0.1:3001";

export default defineConfig({
  testDir: "./tests",
  timeout: 60_000,
  expect: {
    timeout: 8_000
  },
  fullyParallel: false,
  retries: 1,
  reporter: [["list"], ["html", { open: "never", outputFolder: "playwright-report" }]],
  use: {
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "retain-on-failure"
  },
  outputDir: "test-results",
  projects: [
    {
      name: "blog-web",
      testMatch: /blog-web\.spec\.ts/,
      use: { baseURL: webBaseURL }
    },
    {
      name: "blog-admin",
      testMatch: /blog-admin\.spec\.ts/,
      use: { baseURL: adminBaseURL }
    }
  ]
});
