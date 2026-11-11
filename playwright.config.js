import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  retries: 0,
  reporter: "list",
  use: {
    baseURL: "http://127.0.0.1:18080",
    trace: "retain-on-failure"
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] }
    }
  ],
  webServer: {
    command: "./gradlew bootRun",
    url: "http://127.0.0.1:18080/api/health",
    timeout: 120_000,
    reuseExistingServer: false,
    env: {
      SERVER_PORT: "18080",
      MATRIZ_DB_URL: "jdbc:h2:mem:matriz-e2e;DB_CLOSE_DELAY=-1",
      MATRIZ_DB_USERNAME: "sa",
      MATRIZ_DB_PASSWORD: "",
      MATRIZ_DEMO_MANAGER_PASSWORD: "e2e-manager-test-password",
      MATRIZ_DEMO_MEMBER_PASSWORD: "e2e-member-test-password",
      MATRIZ_SESSION_COOKIE_SECURE: "false"
    }
  }
});
