import { apiFetch, requireSession } from "./auth.js";

const profile = document.querySelector("#profile");
const message = document.querySelector("#message");
const logoutButton = document.querySelector("#logout");

const user = await requireSession();
if (user) {
    profile.textContent = `${user.displayName} (${user.email})`;
}

logoutButton.addEventListener("click", async () => {
    logoutButton.disabled = true;
    message.textContent = "";
    try {
        const response = await apiFetch("/auth/logout", {
            method: "POST"
        });
        if (response?.status === 204) {
            window.location.replace("/login.html?reason=logout");
        } else if (response) {
            message.textContent = "Sign out could not be completed. Please try again.";
        }
    } catch {
        message.textContent = "Unable to connect. Check your connection and try again.";
    } finally {
        logoutButton.disabled = false;
    }
});
