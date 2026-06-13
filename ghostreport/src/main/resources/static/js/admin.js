const {
    element,
    setChildren,
    setText,
    message,
    metaLine,
    actionButton
} = window.GhostReportDom;

let adminSessionAuth = null;
let adminUsername = null;
let adminMfaChallengeId = null;
let adminUsersCache = [];

function getApiBase() {
    return typeof API_BASE !== "undefined" ? API_BASE : "";
}

async function adminHandleJsonResponse(response) {
    if (typeof handleJsonResponse === "function") {
        return handleJsonResponse(response);
    }

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
        throw new Error(data?.message || data?.error || text || "Erro no pedido.");
    }

    return data;
}

function clearAdminMessages() {
    [
        "loginError",
        "mfaError",
        "globalResult",
        "globalError",
        "userManagementResult",
        "userManagementError",
        "editUserResult",
        "editUserError",
        "createUserResult",
        "createUserError",
        "backupResult",
        "backupError"
    ].forEach(id => setText(id, ""));
}

function showElement(id, display = "block") {
    const node = document.getElementById(id);
    if (!node) return;
    node.classList.remove("hidden");
    node.style.display = display;
}

function hideElement(id) {
    const node = document.getElementById(id);
    if (!node) return;
    node.classList.add("hidden");
    node.style.display = "none";
}

function showAdminPage(pageId) {
    clearAdminMessages();

    document.querySelectorAll(".admin-page").forEach(page => {
        page.classList.add("hidden");
        page.style.display = "none";
    });

    const page = document.getElementById(pageId);
    page.classList.remove("hidden");
    page.style.display = "block";

    document.querySelectorAll("#adminNav button").forEach(button => {
        button.classList.remove("active");
    });

    const activeButton = Array.from(document.querySelectorAll("#adminNav button"))
        .find(button => button.dataset.page === pageId);

    if (activeButton) {
        activeButton.classList.add("active");
    }

    if (pageId === "usersPage") {
        clearEditUserRoute();
        loadUsers();
    }

    if (pageId === "auditLogsPage") {
        loadAuditLogs();
    }

    if (pageId === "securityAlertsPage") {
        loadSecurityAlerts();
    }

    if (pageId === "backupsPage") {
        loadBackups();
    }
}

function adminAuthHeaders(extra = {}) {
    if (!adminSessionAuth) {
        throw new Error("Sessão inválida. Faz login novamente.");
    }

    return {
        "Authorization": adminSessionAuth,
        ...extra
    };
}

async function adminSafeFetch(url, options = {}) {
    const fetchOptions = typeof csrfFetchOptions === "function"
        ? csrfFetchOptions(options)
        : options;
    const response = await fetch(url, fetchOptions);
    const authFlowRequest = String(url).includes("/auth/login") || String(url).includes("/auth/mfa/verify");

    if (!authFlowRequest && (response.status === 401 || response.status === 403)) {
        adminSessionAuth = null;
        adminUsername = null;
        adminMfaChallengeId = null;
        throw new Error("Sessão expirada ou sem permissões de administrador.");
    }

    return response;
}

async function adminValidateSession() {
    const response = await adminSafeFetch(`${getApiBase()}/admin/panel`, {
        headers: adminAuthHeaders()
    });

    if (!response.ok) {
        throw new Error("Login inválido.");
    }
}

function showAdminDashboard() {
    hideElement("adminLoginPanel");
    hideElement("loginSection");
    hideElement("mfaSection");
    showElement("adminDashboard");

    hideElement("publicNav");
    showElement("adminNav", "flex");

    showAdminPage("usersPage");
    loadAuditLogs();
    loadSecurityAlerts();
    loadBackups();
}

async function adminLogin() {
    clearAdminMessages();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
        setText("loginError", "Preenche utilizador e password.");
        return;
    }

    try {
        const loginResponse = await adminSafeFetch(`${getApiBase()}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ username, password })
        });
        const loginData = await adminHandleJsonResponse(loginResponse);
        if (loginData.mfaRequired) {
            adminSessionAuth = null;
            adminUsername = loginData.username;
            adminMfaChallengeId = loginData.mfaChallengeId;
            hideElement("loginSection");
            showElement("mfaSection");
            hideElement("adminNav");
            showElement("publicNav", "flex");
            return;
        }

        adminSessionAuth = `${loginData.tokenType} ${loginData.token}`;

        await adminValidateSession();
        adminUsername = loginData.username;

        showAdminDashboard();
    } catch (error) {
        adminSessionAuth = null;
        adminUsername = null;
        setText("loginError", error.message || "Login inválido.");
    }
}

async function verifyAdminMfa() {
    clearAdminMessages();

    const code = document.getElementById("mfaCode").value.trim();
    if (!adminMfaChallengeId || !code) {
        setText("mfaError", "Introduz o codigo de verificacao.");
        return;
    }

    try {
        const response = await adminSafeFetch(`${getApiBase()}/auth/mfa/verify`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                challengeId: adminMfaChallengeId,
                code
            })
        });
        const data = await adminHandleJsonResponse(response);
        adminSessionAuth = `${data.tokenType} ${data.token}`;
        adminUsername = data.username;
        adminMfaChallengeId = null;
        document.getElementById("mfaCode").value = "";

        await adminValidateSession();
        showAdminDashboard();
    } catch (error) {
        adminSessionAuth = null;
        setText("mfaError", error.message || "Codigo invalido ou expirado.");
    }
}

function cancelAdminMfa() {
    adminSessionAuth = null;
    adminUsername = null;
    adminMfaChallengeId = null;
    document.getElementById("mfaCode").value = "";
    hideElement("mfaSection");
    showElement("loginSection", "flex");
    showElement("publicNav", "flex");
    hideElement("adminNav");
}

async function adminLogout(reload = true) {
    if (typeof revokeCurrentToken === "function") {
        await revokeCurrentToken(adminSessionAuth);
    }

    adminSessionAuth = null;
    adminUsername = null;
    adminMfaChallengeId = null;

    if (reload) {
        location.reload();
    } else {
        showElement("publicNav", "flex");
        hideElement("adminNav");
        hideElement("adminDashboard");
        hideElement("mfaSection");
        showElement("adminLoginPanel");
        showElement("loginSection", "flex");
    }
}

function roleLabel(role) {
    return String(role ?? "-").toUpperCase();
}

function formatBytes(size) {
    const value = Number(size || 0);
    if (value < 1024) return `${value} B`;
    if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
    return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

function shortHash(hash) {
    const value = String(hash || "");
    return value.length > 16 ? `${value.slice(0, 16)}...` : value || "-";
}

function safeClassToken(value) {
    const token = String(value || "unknown").toLowerCase();
    return /^[a-z0-9_-]+$/.test(token) ? token : "unknown";
}

function renderUser(user) {
    return element("div", {
        className: "user-card",
        dataset: {
            userId: user.id,
            active: user.active === false ? "false" : "true"
        }
    },
        element("h3", { text: user.username || "Utilizador" }),
        element("div", { className: "user-summary" },
            metaLine("ID", user.id),
            metaLine("Email", user.email || "-"),
            metaLine("Role", roleLabel(user.role)),
            metaLine("Estado", user.active === false ? "Inativo" : "Ativo")
        ),
        element("div", { className: "user-actions" },
            actionButton("Editar", "editUser", { userId: user.id }),
            user.active === false
                ? actionButton("Ativar", "activateUser", { userId: user.id })
                : actionButton("Desativar", "deactivateUser", { userId: user.id }, "danger-btn")
        )
    );
}

function renderAuditLog(log) {
    return element("div", { className: "audit-card" },
        element("h3", { text: log.action || "Ação registada" }),
        metaLine("ID", log.id),
        metaLine("Data", log.timestamp),
        metaLine("Ator", log.actor),
        metaLine("Alvo", `${log.targetType || "-"} ${log.targetId || ""}`.trim()),
        metaLine("Detalhes", log.details)
    );
}

function renderSecurityAlert(alert) {
    const severity = safeClassToken(alert.severity);

    return element("div", { className: "alert-card" },
        element("h3", { text: alert.alertType || "Alerta de segurança" }),
        element("div", { className: "meta-line" },
            element("span", { className: "meta-label", text: "Severidade" }),
            element("span", { className: `severity ${severity}`, text: alert.severity || "-" })
        ),
        metaLine("ID", alert.id),
        metaLine("Data", alert.timestamp),
        metaLine("Ator", alert.actor),
        metaLine("Alvo", `${alert.targetType || "-"} ${alert.targetId || ""}`.trim()),
        metaLine("Descrição", alert.description)
    );
}

function renderBackup(backup) {
    return element("div", { className: "backup-card" },
        element("h3", { text: backup.filename }),
        metaLine("Criado em", backup.createdAt),
        metaLine("Tamanho", formatBytes(backup.size)),
        metaLine("SHA-256", shortHash(backup.sha256)),
        element("div", { className: "backup-actions" },
            actionButton("Validar", "verifyBackup", { filename: backup.filename }),
            actionButton("Descarregar", "downloadBackup", { filename: backup.filename }),
            actionButton("Restore", "restoreBackup", { filename: backup.filename })
        )
    );
}

async function loadUsers() {
    const usersDiv = document.getElementById("users");
    const count = document.getElementById("usersCount");

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/users`, {
            method: "GET",
            headers: adminAuthHeaders()
        });

        const data = await adminHandleJsonResponse(response);
        const users = Array.isArray(data) ? data : [];
        adminUsersCache = users;

        count.textContent = users.length;

        if (!users.length) {
            setChildren(usersDiv, message("result", "Sem utilizadores para apresentar."));
            return;
        }

        setChildren(usersDiv, users.map(renderUser));
    } catch (error) {
        setChildren(usersDiv, message("error", error.message));
        if (count) count.textContent = "0";
    }
}

async function createUser() {
    clearAdminMessages();

    const username = document.getElementById("newUsername").value.trim();
    const email = document.getElementById("newEmail").value.trim();
    const password = document.getElementById("newPassword").value;
    const role = document.getElementById("newRole").value;

    if (!username || !email || !password || !role) {
        setText("createUserError", "Preenche todos os campos.");
        return;
    }

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/users`, {
            method: "POST",
            headers: adminAuthHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify({ username, email, password, role })
        });

        await adminHandleJsonResponse(response);

        setText("createUserResult", `Utilizador ${username} criado com sucesso.`);
        clearCreateUserForm();

        await loadUsers();
        showAdminPage("usersPage");
    } catch (error) {
        setText("createUserError", error.message);
    }
}

function setEditUserRoute(userId) {
    if (window.history?.replaceState) {
        window.history.replaceState(null, document.title, `#/admin/users/${encodeURIComponent(userId)}/edit`);
    }
}

function clearEditUserRoute() {
    if (window.history?.replaceState && window.location.hash.startsWith("#/admin/users/")) {
        window.history.replaceState(null, document.title, window.location.pathname);
    }
}

function editUser(userId) {
    clearAdminMessages();

    const user = adminUsersCache.find(candidate => Number(candidate.id) === Number(userId));
    if (!user) {
        setText("userManagementError", "Utilizador nao encontrado. Atualiza a lista e tenta novamente.");
        return;
    }

    document.getElementById("editUserId").value = user.id;
    document.getElementById("editUsername").value = user.username || "";
    document.getElementById("editEmail").value = user.email || "";
    document.getElementById("editRole").value = roleLabel(user.role);
    document.getElementById("editActive").value = user.active === false ? "false" : "true";

    setEditUserRoute(user.id);
    showAdminPage("editUserPage");
}

function cancelEditUser() {
    document.getElementById("editUserId").value = "";
    clearEditUserRoute();
    showAdminPage("usersPage");
}

function editUserPayload() {
    return {
        username: document.getElementById("editUsername").value.trim(),
        email: document.getElementById("editEmail").value.trim(),
        role: document.getElementById("editRole").value,
        active: document.getElementById("editActive").value === "true"
    };
}

async function saveEditedUser() {
    clearAdminMessages();

    try {
        const userId = document.getElementById("editUserId").value;
        const payload = editUserPayload();

        const response = await adminSafeFetch(`${getApiBase()}/admin/users/${encodeURIComponent(userId)}`, {
            method: "PUT",
            headers: adminAuthHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify(payload)
        });

        await adminHandleJsonResponse(response);
        setText("editUserResult", "Utilizador atualizado com sucesso.");
        await loadUsers();
        setTimeout(() => {
            clearEditUserRoute();
            showAdminPage("usersPage");
            setText("userManagementResult", "Utilizador atualizado com sucesso.");
        }, 250);
    } catch (error) {
        setText("editUserError", error.message);
    }
}

async function setUserActive(userId, active) {
    clearAdminMessages();

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/users/${encodeURIComponent(userId)}/${active ? "activate" : "deactivate"}`, {
            method: "PATCH",
            headers: adminAuthHeaders()
        });

        await adminHandleJsonResponse(response);
        setText("userManagementResult", active ? "Utilizador ativado." : "Utilizador desativado.");
        await loadUsers();
    } catch (error) {
        setText("userManagementError", error.message);
    }
}

function clearCreateUserForm() {
    document.getElementById("newUsername").value = "";
    document.getElementById("newEmail").value = "";
    document.getElementById("newPassword").value = "";
    document.getElementById("newRole").value = "ANALYST";
}

async function loadAuditLogs() {
    const logsDiv = document.getElementById("auditLogs");
    const count = document.getElementById("auditLogsCount");

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/audit-logs`, {
            method: "GET",
            headers: adminAuthHeaders()
        });

        const data = await adminHandleJsonResponse(response);
        const logs = Array.isArray(data) ? data : [];

        count.textContent = logs.length;

        if (!logs.length) {
            setChildren(logsDiv, message("result", "Sem audit logs."));
            return;
        }

        setChildren(logsDiv, logs.map(renderAuditLog));
    } catch (error) {
        setChildren(logsDiv, message("error", error.message));
        if (count) count.textContent = "0";
    }
}

async function loadSecurityAlerts() {
    const alertsDiv = document.getElementById("securityAlerts");
    const count = document.getElementById("securityAlertsCount");

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/security-alerts`, {
            method: "GET",
            headers: adminAuthHeaders()
        });

        const data = await adminHandleJsonResponse(response);
        const alerts = Array.isArray(data) ? data : [];

        count.textContent = alerts.length;

        if (!alerts.length) {
            setChildren(alertsDiv, message("result", "Sem security alerts."));
            return;
        }

        setChildren(alertsDiv, alerts.map(renderSecurityAlert));
    } catch (error) {
        setChildren(alertsDiv, message("error", error.message));
        if (count) count.textContent = "0";
    }
}

async function loadBackups() {
    const backupsDiv = document.getElementById("backups");
    const count = document.getElementById("backupsCount");

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/backups`, {
            method: "GET",
            headers: adminAuthHeaders()
        });

        const data = await adminHandleJsonResponse(response);
        const backups = Array.isArray(data) ? data : [];

        count.textContent = backups.length;

        if (!backups.length) {
            setChildren(backupsDiv, message("result", "Sem backups criados."));
            return;
        }

        setChildren(backupsDiv, backups.map(renderBackup));
    } catch (error) {
        setChildren(backupsDiv, message("error", error.message));
        if (count) count.textContent = "0";
    }
}

async function createBackup() {
    clearAdminMessages();

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/backups`, {
            method: "POST",
            headers: adminAuthHeaders()
        });

        const data = await adminHandleJsonResponse(response);
        setText("backupResult", `Backup criado: ${data.filename} (${formatBytes(data.size)}).`);
        await loadBackups();
    } catch (error) {
        setText("backupError", error.message);
    }
}

async function verifyBackup(filename) {
    clearAdminMessages();

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/backups/${encodeURIComponent(filename)}/verify`, {
            method: "POST",
            headers: adminAuthHeaders()
        });

        const data = await adminHandleJsonResponse(response);
        setText("backupResult", `${data.filename} validado com sucesso. Ficheiros verificados: ${data.checkedFiles}.`);
        await loadBackups();
    } catch (error) {
        setText("backupError", error.message);
    }
}

async function downloadBackup(filename) {
    clearAdminMessages();

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/backups/${encodeURIComponent(filename)}/download`, {
            method: "GET",
            headers: adminAuthHeaders()
        });

        if (!response.ok) {
            await adminHandleJsonResponse(response);
            return;
        }

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);

        setText("backupResult", `Download iniciado: ${filename}.`);
    } catch (error) {
        setText("backupError", error.message);
    }
}

async function restoreBackup(filename) {
    clearAdminMessages();

    try {
        const response = await adminSafeFetch(`${getApiBase()}/admin/backups/${encodeURIComponent(filename)}/restore`, {
            method: "POST",
            headers: adminAuthHeaders()
        });

        const data = await adminHandleJsonResponse(response);
        setText("backupResult", `${data.filename} validado e extraído para staging.`);
    } catch (error) {
        setText("backupError", error.message);
    }
}

document.addEventListener("click", event => {
    const button = event.target.closest("[data-action]");
    if (!button) return;

    const actions = {
        showAdminPage: () => showAdminPage(button.dataset.page),
        adminLogout: () => adminLogout(),
        adminLogin,
        verifyAdminMfa,
        cancelAdminMfa,
        loadUsers,
        createUser,
        editUser: () => editUser(Number(button.dataset.userId)),
        saveEditedUser,
        cancelEditUser,
        activateUser: () => setUserActive(Number(button.dataset.userId), true),
        deactivateUser: () => setUserActive(Number(button.dataset.userId), false),
        clearCreateUserForm,
        loadAuditLogs,
        loadSecurityAlerts,
        createBackup,
        loadBackups,
        verifyBackup: () => verifyBackup(button.dataset.filename),
        downloadBackup: () => downloadBackup(button.dataset.filename),
        restoreBackup: () => restoreBackup(button.dataset.filename)
    };

    const action = actions[button.dataset.action];
    if (action) action();
});
