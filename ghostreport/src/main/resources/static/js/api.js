const API_BASE = "http://localhost:8081";

let adminAuth = localStorage.getItem("adminAuth");

async function handleJsonResponse(response) {
    const contentType = response.headers.get("content-type");

    let data;
    if (contentType && contentType.includes("application/json")) {
        data = await response.json();
    } else {
        data = await response.text();
    }

    if (!response.ok) {
        const errorMessage = data?.error || data?.message || JSON.stringify(data);
        throw new Error(errorMessage);
    }

    return data;
}

async function login() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const errorDiv = document.getElementById("loginError");

    try {
        const loginResponse = await fetch(`${API_BASE}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ username, password })
        });

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

function logout() {
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
        const response = await fetch(`${API_BASE}/admin/users`, {
            method: "POST",
            headers: {
                "Authorization": adminAuth,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

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
