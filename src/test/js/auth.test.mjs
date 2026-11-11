import assert from "node:assert/strict";
import { test } from "node:test";

globalThis.document = {
    querySelector: () => ({ content: "" })
};

const location = {
    origin: "http://localhost:8080",
    pathname: "/login.html",
    search: "",
    hash: "",
    assigned: null,
    assign(value) {
        this.assigned = value;
    }
};
globalThis.window = { location };

const { apiFetch } = await import("../../main/resources/static/js/auth.js");

test("invalid login credentials are returned to the login form", async () => {
    globalThis.fetch = async input => {
        if (String(input).endsWith("/auth/csrf")) {
            return Response.json({ token: "test-csrf-token" });
        }
        return new Response(null, { status: 401 });
    };

    const response = await apiFetch("/auth/login", { method: "POST" });

    assert.equal(response.status, 401);
    assert.equal(location.assigned, null);
});

test("unauthorized protected API requests redirect to sign-in", async () => {
    globalThis.fetch = async () => new Response(null, { status: 401 });
    location.assigned = null;
    location.pathname = "/index.html";

    const response = await apiFetch("/projects");

    assert.equal(response, null);
    assert.match(location.assigned, /^\/login\.html\?reason=expired/);
});
