const API_BASE = window.location.origin && window.location.origin !== "null"
    ? window.location.origin
    : "http://localhost:8081";
const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

let adminAuth = null;

function readCookie(name) {
    return document.cookie
        .split("; ")
        .map(cookie => cookie.split("="))
        .find(([key]) => key === name)
        ?.slice(1)
        .join("=") || "";
}

function csrfFetchOptions(options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const unsafeMethod = !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
    const headers = {
        ...(options.headers || {})
    };

    if (unsafeMethod && !headers[CSRF_HEADER_NAME]) {
        const token = readCookie(CSRF_COOKIE_NAME);
        if (token) {
            headers[CSRF_HEADER_NAME] = decodeURIComponent(token);
        }
    }

    return {
        ...options,
        credentials: options.credentials || "same-origin",
        headers
    };
}

function isUnsafeRequest(options = {}) {
    const method = (options.method || "GET").toUpperCase();
    return !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
}

async function ensureCsrfCookie(options = {}) {
    if (!isUnsafeRequest(options)) {
        return;
    }

    const headers = options.headers || {};
    if (headers[CSRF_HEADER_NAME] || readCookie(CSRF_COOKIE_NAME)) {
        return;
    }

    try {
        await fetch(`${API_BASE}/`, {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store"
        });
    } catch {
        // The unsafe request below will still fail closed if the CSRF cookie cannot be obtained.
    }
}

async function csrfFetch(url, options = {}) {
    await ensureCsrfCookie(options);
    return fetch(url, csrfFetchOptions(options));
}

async function handleJsonResponse(response) {
    const contentType = response.headers.get("content-type");

    let data;
    if (contentType && contentType.includes("application/json")) {
        data = await response.json();
    } else {
        data = await response.text();
    }

    if (!response.ok) {
        const fieldMessage = data?.fields
            ? " " + Object.entries(data.fields).map(([field, message]) => `${field}: ${message}`).join("; ")
            : "";
        const errorMessage = (data?.error || data?.message || JSON.stringify(data)) + fieldMessage;
        throw new Error(errorMessage);
    }

    return data;
}

async function revokeCurrentToken(authHeader) {
    if (!authHeader) {
        return;
    }

    try {
        await fetch(`${API_BASE}/auth/logout`, csrfFetchOptions({
            method: "POST",
            headers: {
                "Authorization": authHeader
            }
        }));
    } catch {
        // Local logout still clears the in-memory token if the network is unavailable.
    }
}

async function login() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const errorDiv = document.getElementById("loginError");

    try {
        const loginResponse = await fetch(`${API_BASE}/auth/login`, csrfFetchOptions({
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ username, password })
        }));

        const loginData = await handleJsonResponse(loginResponse);
        adminAuth = `${loginData.tokenType} ${loginData.token}`;

        document.getElementById("loginSection").style.display = "none";
        document.getElementById("adminPanel").style.display = "block";

        loadUsers();
    } catch (err) {
        errorDiv.textContent = err.message;
    }
}

async function logout() {
    await revokeCurrentToken(adminAuth);
    adminAuth = null;

    document.getElementById("adminPanel").style.display = "none";
    document.getElementById("loginSection").style.display = "block";
}

async function loadUsers() {
    if (!adminAuth) return;

    const { element, setChildren, message } = window.GhostReportDom;
    const usersContainer = document.getElementById("users");

    try {
        const response = await fetch(`${API_BASE}/admin/users`, {
            headers: {
                "Authorization": adminAuth
            }
        });

        const data = await handleJsonResponse(response);
        const users = Array.isArray(data) ? data : [];

        if (!users.length) {
            setChildren(usersContainer, message("result", "Sem utilizadores para apresentar."));
            return;
        }

        setChildren(usersContainer, users.map(user => element("div", { className: "card" },
            element("strong", { text: user.username || "Utilizador" }),
            element("br"),
            `Email: ${user.email || "-"}`,
            element("br"),
            `Role: ${user.role || "-"}`
        )));
    } catch (err) {
        setChildren(usersContainer, message("error", err.message));
    }
}

async function createUser() {
    const resultDiv = document.getElementById("createUserResult");

    const payload = {
        username: document.getElementById("newUsername").value,
        email: document.getElementById("newEmail").value,
        password: document.getElementById("newPassword").value,
        role: document.getElementById("newRole").value
    };

    try {
        const response = await fetch(`${API_BASE}/admin/users`, csrfFetchOptions({
            method: "POST",
            headers: {
                "Authorization": adminAuth,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        }));

        await handleJsonResponse(response);

        resultDiv.textContent = "Utilizador criado com sucesso!";
        loadUsers();
    } catch (err) {
        resultDiv.textContent = err.message;
    }
}
