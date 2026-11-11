const loginPath = "/login.html";
const configuredApiBaseUrl = document.querySelector('meta[name="matriz-api-base-url"]')?.content.trim();
const apiBaseUrl = configuredApiBaseUrl || window.location.origin;

function apiUrl(path) {
    let base;
    try {
        base = new URL(apiBaseUrl);
    } catch {
        throw new Error("The configured Backend address is invalid.");
    }
    if (!["http:", "https:"].includes(base.protocol)) {
        throw new Error("The configured Backend address must use HTTP or HTTPS.");
    }
    return new URL(path.replace(/^\/+/, ""), `${base.origin}/`);
}

export function safeReturnPath(value) {
    if (typeof value !== "string" || !value.startsWith("/") || value.startsWith("//")) {
        return "/index.html";
    }
    let target;
    try {
        target = new URL(value, window.location.origin);
    } catch {
        return "/index.html";
    }
    if (target.origin !== window.location.origin || target.pathname === loginPath) {
        return "/index.html";
    }
    return `${target.pathname}${target.search}${target.hash}`;
}

export async function getCsrfToken() {
    const response = await fetch(apiUrl("/auth/csrf"), { credentials: "include" });
    if (!response.ok) {
        throw new Error("Could not prepare a secure request.");
    }
    const result = await response.json();
    return result.token;
}

export async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    const method = (options.method || "GET").toUpperCase();
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
        headers.set("X-CSRF-TOKEN", await getCsrfToken());
    }

    const response = await fetch(apiUrl(url), {
        ...options,
        method,
        headers,
        credentials: "include"
    });
    if (response.status === 401) {
        const returnTo = window.location.pathname + window.location.search + window.location.hash;
        window.location.assign(`${loginPath}?reason=expired&returnTo=${encodeURIComponent(returnTo)}`);
        return null;
    }
    return response;
}

export { apiUrl };

export async function requireSession() {
    const response = await apiFetch("/auth/me");
    if (!response || !response.ok) {
        if (response) {
            window.location.assign(`${loginPath}?reason=unavailable`);
        } else {
            window.location.assign(
                `${loginPath}?reason=expired&returnTo=${encodeURIComponent(window.location.pathname)}`
            );
        }
        return null;
    }
    return response.json();
}
