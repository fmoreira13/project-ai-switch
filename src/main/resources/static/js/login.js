import { apiFetch, apiUrl, safeReturnPath } from "./auth.js";

const form = document.querySelector("#login-form");
const message = document.querySelector("#message");
const params = new URLSearchParams(window.location.search);
const returnTo = safeReturnPath(params.get("returnTo"));

if (params.get("reason") === "expired") {
    message.textContent = "Your session has expired. Sign in again to continue.";
} else if (params.get("reason") === "logout") {
    message.textContent = "You have signed out.";
} else if (params.get("reason") === "unavailable") {
    message.textContent = "Your session could not be verified. Please sign in again.";
} else {
    message.textContent = "Sign in to continue to Matriz.";
}

fetch(apiUrl("/auth/me"), { credentials: "include" })
    .then(response => {
        if (response.ok) {
            window.location.replace(returnTo);
        }
    })
    .catch(() => {
        message.textContent = "Unable to connect. Check your connection and try again.";
    });

form.addEventListener("submit", async event => {
    event.preventDefault();
    const submit = form.querySelector("button");
    submit.disabled = true;
    message.textContent = "Signing in...";

    try {
        const response = await apiFetch("/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: form.elements.email.value,
                password: form.elements.password.value
            })
        });

        if (response.status === 401) {
            message.textContent = "Email or password is incorrect.";
            return;
        }
        if (!response.ok) {
            message.textContent = "Sign in could not be completed. Please try again.";
            return;
        }
        window.location.replace(returnTo);
    } catch {
        message.textContent = "Unable to connect. Check your connection and try again.";
    } finally {
        submit.disabled = false;
    }
});
