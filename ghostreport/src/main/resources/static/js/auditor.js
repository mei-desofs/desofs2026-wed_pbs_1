const {
    element,
    setChildren,
    setText,
    message,
    metaLine,
    actionButton
} = window.GhostReportDom;

let auditorAuth = null;
let auditorUsername = null;
let auditorMfaChallengeId = null;
const AUDITOR_ALLOWED_ROLES = ["AUDITOR", "ADMIN"];

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

function getApiBase() {
    return typeof API_BASE !== "undefined" ? API_BASE : "";
}

async function auditorHandleJsonResponse(response) {
    if (typeof handleJsonResponse === "function") {
        return handleJsonResponse(response);
    }

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
        const fields = data?.fields ? " " + Object.entries(data.fields)
            .map(([field, fieldMessage]) => `${field}: ${fieldMessage}`)
            .join("; ") : "";
        throw new Error((data?.message || data?.error || text || "Erro no pedido.") + fields);
    }

    return data;
}

function auditorAuthHeaders(extra = {}) {
    auditorAuth = window.GhostReportAuth.getAuthHeader(AUDITOR_ALLOWED_ROLES);

    return {
        "Authorization": auditorAuth,
        ...extra
    };
}

async function auditorSafeFetch(url, options = {}) {
    const fetchOptions = typeof csrfFetchOptions === "function"
        ? csrfFetchOptions(options)
        : options;
    const response = await fetch(url, fetchOptions);
    const authFlowRequest = String(url).includes("/auth/login") || String(url).includes("/auth/mfa/verify");

    if (!authFlowRequest && response.status === 401) {
        auditorAuth = null;
        auditorUsername = null;
        window.GhostReportAuth.clearSession();
        throw new Error("Sessão expirada. Faz login novamente.");
    }

    if (!authFlowRequest && response.status === 403) {
        throw new Error("Sem permissões de auditoria para executar esta ação.");
    }

    return response;
}

async function validateAuditorSession() {
    const response = await auditorSafeFetch(`${getApiBase()}/audit/logs`, {
        headers: auditorAuthHeaders()
    });

    if (!response.ok) {
        throw new Error("Login inválido.");
    }
}

async function auditorLogin() {
    clearAuditorMessages();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
        setText("loginError", "Preenche utilizador e password.");
        return;
    }

    try {
        const loginResponse = await auditorSafeFetch(`${getApiBase()}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ username, password })
        });
        const loginData = await auditorHandleJsonResponse(loginResponse);

        if (loginData.mfaRequired) {
            auditorAuth = null;
            window.GhostReportAuth.clearSession();
            auditorUsername = loginData.username;
            auditorMfaChallengeId = loginData.mfaChallengeId;
            hideElement("loginSection");
            showElement("mfaSection");
            return;
        }

        window.GhostReportAuth.setSession(loginData);
        auditorAuth = window.GhostReportAuth.getAuthHeader(AUDITOR_ALLOWED_ROLES);

        await validateAuditorSession();
        auditorUsername = loginData.username;

        showAuditorDashboard();
    } catch (error) {
        auditorAuth = null;
        auditorUsername = null;
        window.GhostReportAuth.clearSession();
        setText("loginError", error.message || "Login inválido.");
    }
}

async function verifyAuditorMfa() {
    clearAuditorMessages();

    const code = document.getElementById("mfaCode").value.trim();
    if (!auditorMfaChallengeId || !code) {
        setText("mfaError", "Introduz o codigo de verificacao.");
        return;
    }

    try {
        const response = await auditorSafeFetch(`${getApiBase()}/auth/mfa/verify`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                challengeId: auditorMfaChallengeId,
                code
            })
        });
        const data = await auditorHandleJsonResponse(response);
        window.GhostReportAuth.setSession(data);
        auditorAuth = window.GhostReportAuth.getAuthHeader(AUDITOR_ALLOWED_ROLES);
        auditorUsername = data.username;
        auditorMfaChallengeId = null;
        document.getElementById("mfaCode").value = "";

        await validateAuditorSession();
        showAuditorDashboard();
    } catch (error) {
        auditorAuth = null;
        window.GhostReportAuth.clearSession();
        setText("mfaError", error.message || "Codigo invalido ou expirado.");
    }
}

function cancelAuditorMfa() {
    auditorMfaChallengeId = null;
    auditorUsername = null;
    window.GhostReportAuth.clearSession();
    document.getElementById("mfaCode").value = "";
    hideElement("mfaSection");
    showElement("loginSection");
}

function showAuditorDashboard() {
    hideElement("auditorLoginPanel");
    hideElement("loginSection");
    hideElement("mfaSection");
    showElement("auditorDashboard");

    hideElement("publicNav");
    showElement("auditorNav", "flex");

    showAuditorPage("auditLogsPage");
    loadSecurityAlerts();
}

async function auditorLogout(reload = true) {
    await window.GhostReportAuth.logout(auditorAuth);

    auditorAuth = null;
    auditorUsername = null;
    auditorMfaChallengeId = null;

    if (reload) {
        location.reload();
    } else {
        showElement("publicNav", "flex");
        hideElement("auditorNav");
        hideElement("auditorDashboard");
        hideElement("mfaSection");
        showElement("auditorLoginPanel");
        showElement("loginSection", "flex");
    }
}

async function restoreAuditorSession() {
    const session = window.GhostReportAuth.getSession(AUDITOR_ALLOWED_ROLES);
    if (!session) {
        return;
    }

    try {
        auditorAuth = window.GhostReportAuth.getAuthHeader(AUDITOR_ALLOWED_ROLES);
        auditorUsername = session.username;
        await validateAuditorSession();
        showAuditorDashboard();
    } catch {
        auditorAuth = null;
        auditorUsername = null;
        window.GhostReportAuth.clearSession();
    }
}

function showAuditorPage(pageId) {
    clearAuditorMessages();

    document.querySelectorAll(".admin-page").forEach(page => {
        page.classList.add("hidden");
        page.style.display = "none";
    });

    const page = document.getElementById(pageId);
    page.classList.remove("hidden");
    page.style.display = "block";

    document.querySelectorAll("#auditorNav button").forEach(button => {
        button.classList.remove("active");
    });

    const activeButton = Array.from(document.querySelectorAll("#auditorNav button"))
        .find(button => button.dataset.page === pageId);

    if (activeButton) {
        activeButton.classList.add("active");
    }

    if (pageId === "auditLogsPage") {
        loadAuditLogs();
    }

    if (pageId === "securityAlertsPage") {
        loadSecurityAlerts();
    }

    if (pageId === "closedCasesPage") {
        loadClosedCases();
    }

    if (pageId === "backupsPage") {
        loadBackups();
    }
}

function clearAuditorMessages() {
    ["loginError", "mfaError"].forEach(id => setText(id, ""));
}

function safeClassToken(value) {
    const token = String(value || "unknown").toLowerCase();
    return /^[a-z0-9_-]+$/.test(token) ? token : "unknown";
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

function renderClosedCase(item) {
    return element("div", { className: "audit-card" },
        element("h3", { text: `Caso #${item.reportId}` }),
        metaLine("Estado", item.status),
        metaLine("Categoria", item.category),
        metaLine("Prioridade", item.priority),
        metaLine("Analista", item.assignedAnalyst),
        metaLine("Anexos", item.attachmentCount),
        metaLine("Criado", item.reportCreatedAt),
        metaLine("Atualizado", item.caseUpdatedAt)
    );
}

function renderBackup(backup) {
    return element("div", { className: "audit-card" },
        element("h3", { text: backup.filename }),
        metaLine("Tamanho", backup.size),
        metaLine("SHA-256", backup.sha256),
        metaLine("Criado", backup.createdAt),
        actionButton("Verificar", "verifyBackup", { filename: backup.filename }),
        actionButton("Manifesto", "loadBackupManifest", { filename: backup.filename })
    );
}

async function loadAuditLogs() {
    const logsDiv = document.getElementById("auditLogs");
    const count = document.getElementById("auditLogsCount");

    try {
        const response = await auditorSafeFetch(`${getApiBase()}/audit/logs`, {
            method: "GET",
            headers: auditorAuthHeaders()
        });

        const data = await auditorHandleJsonResponse(response);
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
        const response = await auditorSafeFetch(`${getApiBase()}/audit/security-alerts`, {
            method: "GET",
            headers: auditorAuthHeaders()
        });

        const data = await auditorHandleJsonResponse(response);
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

async function loadClosedCases() {
    const casesDiv = document.getElementById("closedCases");
    const count = document.getElementById("closedCasesCount");

    try {
        const response = await auditorSafeFetch(`${getApiBase()}/audit/cases/closed`, {
            method: "GET",
            headers: auditorAuthHeaders()
        });

        const data = await auditorHandleJsonResponse(response);
        const cases = Array.isArray(data) ? data : [];
        count.textContent = cases.length;

        if (!cases.length) {
            setChildren(casesDiv, message("result", "Sem casos fechados."));
            return;
        }

        setChildren(casesDiv, cases.map(renderClosedCase));
    } catch (error) {
        setChildren(casesDiv, message("error", error.message));
        if (count) count.textContent = "0";
    }
}

async function verifyEvidencePackage() {
    const resultDiv = document.getElementById("evidenceResult");
    const reportId = document.getElementById("evidenceReportId").value;

    if (!reportId) {
        setChildren(resultDiv, message("error", "Indica o ID da denúncia."));
        return;
    }

    try {
        const response = await auditorSafeFetch(`${getApiBase()}/audit/cases/${encodeURIComponent(reportId)}/evidence-package/verify`, {
            method: "GET",
            headers: auditorAuthHeaders()
        });

        const data = await auditorHandleJsonResponse(response);
        const files = Array.isArray(data.files) ? data.files : [];
        const summary = element("div", { className: "audit-card" },
            element("h3", { text: `Pacote #${data.reportId}` }),
            metaLine("Estado do caso", data.status),
            metaLine("Integridade", data.valid ? "Válida" : "Inválida"),
            metaLine("Ficheiros verificados", data.checkedFiles),
            metaLine("Mensagem", data.message)
        );
        const fileCards = files.map(file => element("div", { className: "audit-card" },
            element("h3", { text: `Ficheiro ${Number(file.index || 0) + 1}` }),
            metaLine("Tamanho", file.size),
            metaLine("SHA-256", file.sha256),
            metaLine("Válido", file.valid ? "Sim" : "Não")
        ));

        setChildren(resultDiv, summary, fileCards);
    } catch (error) {
        setChildren(resultDiv, message("error", error.message));
    }
}

async function loadBackups() {
    const backupsDiv = document.getElementById("backups");
    const resultDiv = document.getElementById("backupVerification");
    const count = document.getElementById("backupsCount");

    setChildren(resultDiv);

    try {
        const response = await auditorSafeFetch(`${getApiBase()}/audit/backups`, {
            method: "GET",
            headers: auditorAuthHeaders()
        });

        const data = await auditorHandleJsonResponse(response);
        const backups = Array.isArray(data) ? data : [];
        count.textContent = backups.length;

        if (!backups.length) {
            setChildren(backupsDiv, message("result", "Sem backups."));
            return;
        }

        setChildren(backupsDiv, backups.map(renderBackup));
    } catch (error) {
        setChildren(backupsDiv, message("error", error.message));
        if (count) count.textContent = "0";
    }
}

async function verifyBackup(filename) {
    const resultDiv = document.getElementById("backupVerification");

    try {
        const response = await auditorSafeFetch(`${getApiBase()}/audit/backups/${encodeURIComponent(filename)}/verify`, {
            method: "GET",
            headers: auditorAuthHeaders()
        });

        const data = await auditorHandleJsonResponse(response);
        setChildren(resultDiv,
            element("div", { className: "audit-card" },
                element("h3", { text: `Verificação ${data.filename}` }),
                metaLine("Válido", data.valid ? "Sim" : "Não"),
                metaLine("SHA-256", data.sha256),
                metaLine("Ficheiros verificados", data.checkedFiles),
                metaLine("Mensagem", data.message)
            )
        );
    } catch (error) {
        setChildren(resultDiv, message("error", error.message));
    }
}

async function loadBackupManifest(filename) {
    const resultDiv = document.getElementById("backupVerification");

    try {
        const response = await auditorSafeFetch(`${getApiBase()}/audit/backups/${encodeURIComponent(filename)}/manifest`, {
            method: "GET",
            headers: auditorAuthHeaders()
        });

        const data = await auditorHandleJsonResponse(response);
        const exports = data.databaseExports || {};
        const exportLines = Object.entries(exports).map(([key, value]) => metaLine(key, value));

        setChildren(resultDiv,
            element("div", { className: "audit-card" },
                element("h3", { text: `Manifesto ${data.filename}` }),
                metaLine("Formato", data.formatVersion),
                metaLine("Criado", data.createdAt),
                metaLine("Total de ficheiros", data.totalFiles),
                exportLines
            )
        );
    } catch (error) {
        setChildren(resultDiv, message("error", error.message));
    }
}

document.addEventListener("click", event => {
    const button = event.target.closest("[data-action]");
    if (!button) return;

    const actions = {
        showAuditorPage: () => showAuditorPage(button.dataset.page),
        auditorLogout: () => auditorLogout(),
        auditorLogin,
        verifyAuditorMfa,
        cancelAuditorMfa,
        loadAuditLogs,
        loadSecurityAlerts,
        loadClosedCases,
        verifyEvidencePackage,
        loadBackups,
        verifyBackup: () => verifyBackup(button.dataset.filename),
        loadBackupManifest: () => loadBackupManifest(button.dataset.filename)
    };

    const action = actions[button.dataset.action];
    if (action) action();
});

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", restoreAuditorSession);
} else {
    restoreAuditorSession();
}
