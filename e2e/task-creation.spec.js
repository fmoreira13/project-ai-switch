import { expect, test } from "@playwright/test";

test("manager signs in, opens a project, and creates a task", async ({ page }) => {
  const taskTitle = `E2E task ${crypto.randomUUID()}`;

  await page.goto("/login.html");
  await page.getByLabel("Email").fill("manager@matriz.local");
  await page.getByLabel("Password").fill("e2e-manager-test-password");
  await page.getByRole("button", { name: "Sign in" }).click();

  const project = page
    .locator("#project-list button[data-project-id]")
    .filter({ hasText: "Matriz Demo - Alpha" });
  await expect(project).toBeVisible();
  await project.click();
  await expect(page.locator("#detail-name")).toHaveText("Matriz Demo - Alpha");

  await page.getByRole("button", { name: "New task" }).click();
  await page.locator("#create-task-title").fill(taskTitle);
  await page.getByRole("button", { name: "Create task", exact: true }).click();

  await expect(page.locator("#task-list")).toContainText(taskTitle);
  await expect(page.locator("#message")).toHaveText("Task created.");
});
