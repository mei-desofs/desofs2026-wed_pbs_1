let auditorAuth = sessionStorage.getItem("auditorAuth");
    let auditorUsername = sessionStorage.getItem("auditorUsername");

    window.addEventListener("load", async () => {
        if (auditorAuth) {
            try {
                await validateAuditorSession();
                showAuditorDashboard();
            } catch {
                auditorLogout(false);
            }
        }
    });

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
                .map(([field, message]) => `${field}: ${message}`)
                .join("; ") : "";
            throw new Error((data?.message || data?.error || text || "Erro no pedido.") + fields);
        }

        return data;
    }

    function auditorAuthHeaders(extra = {}) {
        if (!auditorAuth) {
            throw new Error("Sessão inválida. Faz login novamente.");
        }

        return {
            "Authorization": auditorAuth,
            ...extra
        };
    }

    async function auditorSafeFetch(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            credentials: "omit",
            headers: {
                ...(options.headers || {})
            }
        });

        if (response.status === 401 || response.status === 403) {
            sessionStorage.removeItem("auditorAuth");
            sessionStorage.removeItem("auditorUsername");
            auditorAuth = null;
            throw new Error("Sessão expirada ou sem permissões de auditoria.");
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
            document.getElementById("loginError").innerText = "Preenche utilizador e password.";
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
            auditorAuth = `${loginData.tokenType} ${loginData.token}`;

            await validateAuditorSession();

            sessionStorage.setItem("auditorAuth", auditorAuth);
            sessionStorage.setItem("auditorUsername", loginData.username);
            auditorUsername = loginData.username;

            showAuditorDashboard();
        } catch (error) {
            sessionStorage.removeItem("auditorAuth");
            sessionStorage.removeItem("auditorUsername");
            auditorAuth = null;
            document.getElementById("loginError").innerText = error.message || "Login inválido.";
        }
    }

    function showAuditorDashboard() {
        document.getElementById("auditorLoginPanel").style.display = "none";
        document.getElementById("loginSection").style.display = "none";
        document.getElementById("auditorDashboard").style.display = "block";

        document.getElementById("publicNav").style.display = "none";
        document.getElementById("auditorNav").style.display = "flex";

        showAuditorPage("auditLogsPage");
        loadSecurityAlerts();
    }

    function auditorLogout(reload = true) {
        sessionStorage.removeItem("auditorAuth");
        sessionStorage.removeItem("auditorUsername");

        auditorAuth = null;
        auditorUsername = null;

        if (reload) {
            location.reload();
        } else {
            document.getElementById("publicNav").style.display = "flex";
            document.getElementById("auditorNav").style.display = "none";
            document.getElementById("auditorDashboard").style.display = "none";
            document.getElementById("auditorLoginPanel").style.display = "block";
            document.getElementById("loginSection").style.display = "block";
        }
    }

    function showAuditorPage(pageId) {
        clearAuditorMessages();

        document.querySelectorAll(".admin-page").forEach(page => {
            page.style.display = "none";
        });

        document.getElementById(pageId).style.display = "block";

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
        ["loginError"].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.innerText = "";
        });
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function renderMeta(label, value) {
        return `
            <div class="meta-line">
                <span class="meta-label">${escapeHtml(label)}</span>
                <span>${escapeHtml(value || "-")}</span>
            </div>
        `;
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

            count.innerText = logs.length;

            if (!logs.length) {
                logsDiv.innerHTML = `<div class="result">Sem audit logs.</div>`;
                return;
            }

            logsDiv.innerHTML = logs.map(log => `
                <div class="audit-card">
                    <h3>${escapeHtml(log.action || "Ação registada")}</h3>
                    ${renderMeta("ID", log.id)}
                    ${renderMeta("Data", log.timestamp)}
                    ${renderMeta("Ator", log.actor)}
                    ${renderMeta("Alvo", `${log.targetType || "-"} ${log.targetId || ""}`.trim())}
                    ${renderMeta("Detalhes", log.details)}
                </div>
            `).join("");
        } catch (error) {
            logsDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
            if (count) count.innerText = "0";
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

            count.innerText = alerts.length;

            if (!alerts.length) {
                alertsDiv.innerHTML = `<div class="result">Sem security alerts.</div>`;
                return;
            }

            alertsDiv.innerHTML = alerts.map(alert => {
                const severity = String(alert.severity || "-").toLowerCase();

                return `
                    <div class="alert-card">
                        <h3>${escapeHtml(alert.alertType || "Alerta de segurança")}</h3>
                        <div class="meta-line">
                            <span class="meta-label">Severidade</span>
                            <span class="severity ${escapeHtml(severity)}">${escapeHtml(alert.severity || "-")}</span>
                        </div>
                        ${renderMeta("ID", alert.id)}
                        ${renderMeta("Data", alert.timestamp)}
                        ${renderMeta("Ator", alert.actor)}
                        ${renderMeta("Alvo", `${alert.targetType || "-"} ${alert.targetId || ""}`.trim())}
                        ${renderMeta("Descrição", alert.description)}
                    </div>
                `;
            }).join("");
        } catch (error) {
            alertsDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
            if (count) count.innerText = "0";
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
            count.innerText = cases.length;

            if (!cases.length) {
                casesDiv.innerHTML = `<div class="result">Sem casos fechados.</div>`;
                return;
            }

            casesDiv.innerHTML = cases.map(item => `
                <div class="audit-card">
                    <h3>Caso #${escapeHtml(item.reportId)}</h3>
                    ${renderMeta("Estado", item.status)}
                    ${renderMeta("Categoria", item.category)}
                    ${renderMeta("Prioridade", item.priority)}
                    ${renderMeta("Analista", item.assignedAnalyst)}
                    ${renderMeta("Anexos", item.attachmentCount)}
                    ${renderMeta("Criado", item.reportCreatedAt)}
                    ${renderMeta("Atualizado", item.caseUpdatedAt)}
                </div>
            `).join("");
        } catch (error) {
            casesDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
            if (count) count.innerText = "0";
        }
    }

    async function verifyEvidencePackage() {
        const resultDiv = document.getElementById("evidenceResult");
        const reportId = document.getElementById("evidenceReportId").value;

        if (!reportId) {
            resultDiv.innerHTML = `<div class="error">Indica o ID da denúncia.</div>`;
            return;
        }

        try {
            const response = await auditorSafeFetch(`${getApiBase()}/audit/cases/${encodeURIComponent(reportId)}/evidence-package/verify`, {
                method: "GET",
                headers: auditorAuthHeaders()
            });

            const data = await auditorHandleJsonResponse(response);
            const files = Array.isArray(data.files) ? data.files : [];

            resultDiv.innerHTML = `
                <div class="audit-card">
                    <h3>Pacote #${escapeHtml(data.reportId)}</h3>
                    ${renderMeta("Estado do caso", data.status)}
                    ${renderMeta("Integridade", data.valid ? "Válida" : "Inválida")}
                    ${renderMeta("Ficheiros verificados", data.checkedFiles)}
                    ${renderMeta("Mensagem", data.message)}
                </div>
                ${files.map(file => `
                    <div class="audit-card">
                        <h3>Ficheiro ${escapeHtml(file.index + 1)}</h3>
                        ${renderMeta("Tamanho", file.size)}
                        ${renderMeta("SHA-256", file.sha256)}
                        ${renderMeta("Válido", file.valid ? "Sim" : "Não")}
                    </div>
                `).join("")}
            `;
        } catch (error) {
            resultDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
        }
    }

    async function loadBackups() {
        const backupsDiv = document.getElementById("backups");
        const resultDiv = document.getElementById("backupVerification");
        const count = document.getElementById("backupsCount");

        resultDiv.innerHTML = "";

        try {
            const response = await auditorSafeFetch(`${getApiBase()}/audit/backups`, {
                method: "GET",
                headers: auditorAuthHeaders()
            });

            const data = await auditorHandleJsonResponse(response);
            const backups = Array.isArray(data) ? data : [];
            count.innerText = backups.length;

            if (!backups.length) {
                backupsDiv.innerHTML = `<div class="result">Sem backups.</div>`;
                return;
            }

            backupsDiv.innerHTML = backups.map(backup => `
                <div class="audit-card">
                    <h3>${escapeHtml(backup.filename)}</h3>
                    ${renderMeta("Tamanho", backup.size)}
                    ${renderMeta("SHA-256", backup.sha256)}
                    ${renderMeta("Criado", backup.createdAt)}
                    <button type="button" data-action="verifyBackup" data-filename="${escapeHtml(backup.filename)}">Verificar</button>
                    <button type="button" data-action="loadBackupManifest" data-filename="${escapeHtml(backup.filename)}">Manifesto</button>
                </div>
            `).join("");
        } catch (error) {
            backupsDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
            if (count) count.innerText = "0";
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
            resultDiv.innerHTML = `
                <div class="audit-card">
                    <h3>Verificação ${escapeHtml(data.filename)}</h3>
                    ${renderMeta("Válido", data.valid ? "Sim" : "Não")}
                    ${renderMeta("SHA-256", data.sha256)}
                    ${renderMeta("Ficheiros verificados", data.checkedFiles)}
                    ${renderMeta("Mensagem", data.message)}
                </div>
            `;
        } catch (error) {
            resultDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
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

            resultDiv.innerHTML = `
                <div class="audit-card">
                    <h3>Manifesto ${escapeHtml(data.filename)}</h3>
                    ${renderMeta("Formato", data.formatVersion)}
                    ${renderMeta("Criado", data.createdAt)}
                    ${renderMeta("Total de ficheiros", data.totalFiles)}
                    ${Object.entries(exports).map(([key, value]) => renderMeta(key, value)).join("")}
                </div>
            `;
        } catch (error) {
            resultDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
        }
    }


document.addEventListener("click", event => {
    const button = event.target.closest("[data-action]");
    if (!button) return;

    const actions = {
        showAuditorPage: () => showAuditorPage(button.dataset.page),
        auditorLogout: () => auditorLogout(),
        auditorLogin,
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
