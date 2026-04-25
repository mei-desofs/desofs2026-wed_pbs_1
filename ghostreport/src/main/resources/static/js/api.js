const API_BASE = "http://localhost:8081";

let adminAuth = null;

function basicAuthHeader(username, password) {
    return "Basic " + btoa(`${username}:${password}`);
}

function login() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const errorDiv = document.getElementById("loginError");

    adminAuth = basicAuthHeader(username, password);

    fetch(`${API_BASE}/admin/users`, {
        headers: {
            "Authorization": adminAuth
        }
    })
        .then(res => {
            if (!res.ok) throw new Error("Login inválido");
            return res.json();
        })
        .then(() => {
            document.getElementById("loginSection").style.display = "none";
            document.getElementById("adminPanel").style.display = "block";

            loadUsers();
        })
        .catch(err => {
            errorDiv.innerText = err.message;
        });
}

function loadUsers() {
    fetch(`${API_BASE}/admin/users`, {
        headers: {
            "Authorization": adminAuth
        }
    })
        .then(res => res.json())
        .then(data => {
            document.getElementById("users").innerHTML =
                data.map(u => `
                <div class="card">
                    <strong>${u.username}</strong> (${u.role})
                </div>
            `).join("");
        });
}

function createUser() {
    const resultDiv = document.getElementById("createUserResult");

    const payload = {
        username: document.getElementById("newUsername").value,
        email: document.getElementById("newEmail").value,
        password: document.getElementById("newPassword").value,
        role: document.getElementById("newRole").value
    };

    fetch(`${API_BASE}/admin/users`, {
        method: "POST",
        headers: {
            "Authorization": adminAuth,
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) throw new Error("Erro ao criar utilizador");
            return res.json();
        })
        .then(() => {
            resultDiv.innerText = "Utilizador criado!";
            loadUsers();
        })
        .catch(err => {
            resultDiv.innerText = err.message;
        });
}