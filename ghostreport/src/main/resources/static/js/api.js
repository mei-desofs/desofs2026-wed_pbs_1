const API_BASE = "http://localhost:8081";
const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

let adminAuth = localStorage.getItem("adminAuth");

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
        // Local logout still removes the browser-side token if the network is unavailable.
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

        localStorage.setItem("adminAuth", adminAuth);

        document.getElementById("loginSection").style.display = "none";
        document.getElementById("adminPanel").style.display = "block";

        loadUsers();

    } catch (err) {
        errorDiv.innerText = err.message;
    }
}

async function logout() {
    await revokeCurrentToken(adminAuth);

    localStorage.removeItem("adminAuth");
    adminAuth = null;

    document.getElementById("adminPanel").style.display = "none";
    document.getElementById("loginSection").style.display = "block";
}

async function loadUsers() {
    if (!adminAuth) return;

    try {
        const response = await fetch(`${API_BASE}/admin/users`, {
            headers: {
                "Authorization": adminAuth
            }
        });

        const data = await handleJsonResponse(response);

        document.getElementById("users").innerHTML =
            data.map(u => `
                <div class="card">
                    <strong>${u.username}</strong><br>
                    Email: ${u.email}<br>
                    Role: ${u.role}
                </div>
            `).join("");

    } catch (err) {
        document.getElementById("users").innerText = err.message;
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

        resultDiv.innerText = "Utilizador criado com sucesso!";
        loadUsers();

    } catch (err) {
        resultDiv.innerText = err.message;
    }
}

window.onload = () => {
    if (adminAuth) {
        document.getElementById("loginSection").style.display = "none";
        document.getElementById("adminPanel").style.display = "block";
        loadUsers();
    }
};
