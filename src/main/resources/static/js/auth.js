const loginPath = "/login.html";

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
    const response = await fetch("/auth/csrf", { credentials: "same-origin" });
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

    const response = await fetch(url, {
        ...options,
        method,
        headers,
        credentials: "same-origin"
    });
    if (response.status === 401) {
        const returnTo = window.location.pathname + window.location.search + window.location.hash;
        window.location.assign(`${loginPath}?reason=expired&returnTo=${encodeURIComponent(returnTo)}`);
        return null;
    }
    return response;
}

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
