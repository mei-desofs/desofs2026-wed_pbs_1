let analystAuth = sessionStorage.getItem("analystAuth");
    let analystUsername = sessionStorage.getItem("analystUsername");

    window.addEventListener("load", async () => {
        if (analystAuth) {
            try {
                await validateSession();
                showDashboard();
            } catch {
                logout(false);
            }
        }
    });

    function clearMessages() {
        ["loginError", "globalResult", "globalError", "caseResult", "caseError"].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.innerText = "";
        });
    }

    function showPage(pageId) {
        clearMessages();

        document.querySelectorAll(".analyst-page").forEach(page => {
            page.style.display = "none";
        });

        document.getElementById(pageId).style.display = "block";

        document.querySelectorAll("#analystNav button").forEach(button => {
            button.classList.remove("active");
        });

        const activeButton = Array.from(document.querySelectorAll("#analystNav button"))
            .find(button => button.dataset.page === pageId);

        if (activeButton) {
            activeButton.classList.add("active");
        }

        if (pageId === "submittedPage") {
            loadSubmittedReports();
        }

        if (pageId === "myCasesPage") {
            loadMyCases();
        }

        if (pageId === "historyPage") {
            loadHistory();
        }
    }

    function authHeaders(extra = {}) {
        if (!analystAuth) {
            throw new Error("Sessão inválida. Faz login novamente.");
        }

        return {
            "Authorization": analystAuth,
            ...extra
        };
    }

    async function safeFetch(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            credentials: "omit",
            headers: {
                ...(options.headers || {})
            }
        });

        if (response.status === 401) {
            sessionStorage.removeItem("analystAuth");
            analystAuth = null;
            throw new Error("Sessão expirada ou credenciais inválidas.");
        }

        return response;
    }

    async function validateSession() {
        const response = await safeFetch(`${API_BASE}/analyst/panel`, {
            headers: authHeaders()
        });

        if (!response.ok) {
            throw new Error("Login inválido.");
        }
    }

    function showDashboard() {
        document.getElementById("analystLoginPanel").style.display = "none";
        document.getElementById("loginSection").style.display = "none";
        document.getElementById("dashboard").style.display = "block";

        document.getElementById("publicNav").style.display = "none";
        document.getElementById("analystNav").style.display = "flex";

        showPage("submittedPage");

        loadSubmittedReports();
        loadMyCases();
    }

    async function login() {
        clearMessages();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;

        if (!username || !password) {
            document.getElementById("loginError").innerText = "Preenche utilizador e password.";
            return;
        }

        try {
            const loginResponse = await safeFetch(`${API_BASE}/auth/login`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, password })
            });
            const loginData = await handleJsonResponse(loginResponse);
            analystAuth = `${loginData.tokenType} ${loginData.token}`;

            await validateSession();
            sessionStorage.setItem("analystAuth", analystAuth);
            sessionStorage.setItem("analystUsername", loginData.username);
            analystUsername = loginData.username;

            showDashboard();
        } catch (error) {
            sessionStorage.removeItem("analystAuth");
            analystAuth = null;
            document.getElementById("loginError").innerText = error.message || "Login inválido.";
        }
    }

    async function logout(reload = true) {
        if (typeof revokeCurrentToken === "function") {
            await revokeCurrentToken(analystAuth);
        }

        sessionStorage.removeItem("analystAuth");
        sessionStorage.removeItem("analystUsername");

        analystAuth = null;
        analystUsername = null;

        if (reload) {
            location.reload();
        } else {
            document.getElementById("publicNav").style.display = "flex";
            document.getElementById("analystNav").style.display = "none";
            document.getElementById("dashboard").style.display = "none";
            document.getElementById("analystLoginPanel").style.display = "block";
            document.getElementById("loginSection").style.display = "block";
        }
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function jsString(value) {
        return String(value ?? "")
            .replaceAll("\\", "\\\\")
            .replaceAll("`", "\\`")
            .replaceAll("${", "\\${");
    }

    function renderMeta(label, value) {
        return `
            <div class="meta-line">
                <span class="meta-label">${escapeHtml(label)}</span>
                <span>${escapeHtml(value || "-")}</span>
            </div>
        `;
    }

    function formatBytes(size) {
        const value = Number(size || 0);
        if (value < 1024) return `${value} B`;
        if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
        return `${(value / (1024 * 1024)).toFixed(1)} MB`;
    }

    function selectCase(reportId, priority = "MEDIUM", notes = "") {
        clearMessages();
        setManageReadonly(false);

        document.getElementById("selectedReportId").value = reportId;
        document.getElementById("selectedReportLabel").innerText = reportId;
        document.getElementById("newNotes").value = notes || "";

        if (priority) {
            document.getElementById("newPriority").value = priority;
        }

        document.getElementById("caseResult").innerText = `Caso ${reportId} selecionado.`;
        showPage("managePage");
        loadAttachmentsForReport(reportId);
    }

    async function loadSubmittedReports() {
        const div = document.getElementById("submittedReports");
        const count = document.getElementById("submittedCount");

        try {
            const response = await safeFetch(`${API_BASE}/analyst/reports`, {
                headers: authHeaders()
            });

            const data = await handleJsonResponse(response);
            const submitted = data.filter(r => r.status === "SUBMITTED");
            count.innerText = submitted.length;

            if (!submitted.length) {
                div.innerHTML = `<div class="result">Não existem denúncias pendentes.</div>`;
                return;
            }

            div.innerHTML = submitted.map(r => `
                <div class="case-card">
                    <h3>${escapeHtml(r.title || `Denúncia #${r.id}`)}</h3>
                    ${renderMeta("Report ID", r.id)}
                    ${renderMeta("Categoria", r.category)}
                    ${renderMeta("Estado", r.status)}
                    ${renderMeta("Descrição", r.description)}

                    <div class="case-actions">
                        <button type="button" data-action="assignToMe" data-report-id="${r.id}">Assumir caso</button>
                        <button type="button" data-action="selectCase" data-report-id="${r.id}">Selecionar</button>
                    </div>
                </div>
            `).join("");

        } catch (error) {
            div.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
            if (count) count.innerText = "0";
        }
    }

    async function loadMyCases() {
        const div = document.getElementById("myCases");
        const count = document.getElementById("myCasesCount");

        try {
            const response = await safeFetch(`${API_BASE}/analyst/my-cases`, {
                headers: authHeaders()
            });

            const data = await handleJsonResponse(response);

            const activeCases = data.filter(c =>
                c.reportStatus !== "RESOLVED" && c.reportStatus !== "REJECTED"
            );
            count.innerText = activeCases.length;

            if (!activeCases.length) {
                div.innerHTML = `<div class="result">Não tens casos atribuídos.</div>`;
                return;
            }

            div.innerHTML = activeCases.map(c => `
                <div class="case-card">
                    <h3>Caso #${escapeHtml(c.reportId)}</h3>
                    ${renderMeta("CaseReview ID", c.caseReviewId)}
                    ${renderMeta("Analista", c.assignedAnalystUsername)}
                    ${renderMeta("Prioridade", c.priority)}
                    ${renderMeta("Estado", c.reportStatus)}
                    ${renderMeta("Notas", c.notes)}

                    <div class="case-actions">
                        <button type="button" data-action="selectCase" data-report-id="${c.reportId}" data-priority="${escapeHtml(c.priority || "MEDIUM")}" data-notes="${escapeHtml(c.notes || "")}">Selecionar</button>
                    </div>
                </div>
            `).join("");

        } catch (error) {
            div.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
            if (count) count.innerText = "0";
        }
    }

    async function loadHistory() {
        const div = document.getElementById("historyCases");
        const count = document.getElementById("historyCount");

        try {
            const response = await safeFetch(`${API_BASE}/analyst/my-cases`, {
                headers: authHeaders()
            });

            const data = await handleJsonResponse(response);

            const historyCases = data.filter(c =>
                c.reportStatus === "RESOLVED" || c.reportStatus === "REJECTED"
            );
            count.innerText = historyCases.length;

            if (!historyCases.length) {
                div.innerHTML = `<div class="result">Ainda não existem casos no histórico.</div>`;
                return;
            }

            div.innerHTML = historyCases.map(c => `
                <div class="case-card">
                    <h3>Caso #${escapeHtml(c.reportId)}</h3>
                    ${renderMeta("CaseReview ID", c.caseReviewId)}
                    ${renderMeta("Analista", c.assignedAnalystUsername)}
                    ${renderMeta("Prioridade", c.priority)}
                    ${renderMeta("Estado", c.reportStatus)}
                    ${renderMeta("Notas", c.notes)}

                    <div class="case-actions">
                        <button type="button" data-action="viewCaseOnly" data-report-id="${c.reportId}" data-priority="${escapeHtml(c.priority || "MEDIUM")}" data-notes="${escapeHtml(c.notes || "")}">
                            Ver detalhes
                        </button>

                        <button class="package-btn" type="button" data-action="generateCasePackage" data-report-id="${c.reportId}">
                            Gerar pacote
                        </button>
                    </div>

                    <div id="package-result-${c.reportId}"></div>
                </div>
            `).join("");

        } catch (error) {
            div.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
            if (count) count.innerText = "0";
        }
    }

    async function generateCasePackage(reportId = null) {
        clearMessages();

        try {
            const id = reportId || selectedReportIdOrFail();

            const response = await safeFetch(`${API_BASE}/analyst/reports/${id}/case-package`, {
                method: "POST",
                headers: authHeaders()
            });

            const data = await handleJsonResponse(response);

            const packageBoxId = `package-result-${id}`;
            const box = document.getElementById(packageBoxId);

            const html = `
                <div class="package-result">
                    <strong>Pacote de evidências gerado</strong>
                    <p>Caso #${escapeHtml(data.reportId)}</p>
                    <p>Estado: ${escapeHtml(data.status)}</p>
                    <p>Ficheiros gerados:</p>
                    <ul>
                        ${data.generatedFiles.map(file => `<li>${escapeHtml(file)}</li>`).join("")}
                    </ul>
                </div>
            `;

            if (box) {
                box.innerHTML = html;
            } else {
                alert(`Pacote de evidências do caso ${id} gerado com sucesso.`);
            }

        } catch (error) {
            alert(error.message);
        }
    }

    async function assignToMe(reportId) {
        clearMessages();

        try {
            const response = await safeFetch(`${API_BASE}/analyst/reports/${reportId}/assign`, {
                method: "POST",
                headers: authHeaders()
            });

            await handleJsonResponse(response);

            document.getElementById("globalResult").innerText = `Caso ${reportId} atribuído com sucesso.`;

            await loadSubmittedReports();
            await loadMyCases();

            showPage("myCasesPage");

        } catch (error) {
            document.getElementById("globalError").innerText = error.message;
            alert(error.message);
        }
    }

    function selectedReportIdOrFail() {
        const reportId = document.getElementById("selectedReportId").value;

        if (!reportId) {
            throw new Error("Seleciona um caso primeiro.");
        }

        return reportId;
    }

    async function updateStatus() {
        clearMessages();

        try {
            const reportId = selectedReportIdOrFail();
            const status = document.getElementById("newStatus").value;

            const response = await safeFetch(`${API_BASE}/analyst/reports/${reportId}/status`, {
                method: "PATCH",
                headers: authHeaders({
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({ status })
            });

            await handleJsonResponse(response);

            document.getElementById("caseResult").innerText = "Estado atualizado.";
            loadSubmittedReports();
            loadMyCases();
            loadHistory();

        } catch (error) {
            document.getElementById("caseError").innerText = error.message;
        }
    }

    function setManageReadonly(readonly) {
        document.getElementById("newStatus").disabled = readonly;
        document.getElementById("newPriority").disabled = readonly;
        document.getElementById("newNotes").readOnly = readonly;

        const statusBtn = document.getElementById("updateStatusBtn");
        const priorityBtn = document.getElementById("updatePriorityBtn");
        const notesBtn = document.getElementById("updateNotesBtn");

        if (statusBtn) statusBtn.style.display = readonly ? "none" : "inline-block";
        if (priorityBtn) priorityBtn.style.display = readonly ? "none" : "inline-block";
        if (notesBtn) notesBtn.style.display = readonly ? "none" : "inline-block";
    }

    function viewCaseOnly(reportId, priority = "MEDIUM", notes = "") {
        selectCase(reportId, priority, notes);
        setManageReadonly(true);
    }

    async function updatePriority() {
        clearMessages();

        try {
            const reportId = selectedReportIdOrFail();
            const priority = document.getElementById("newPriority").value;

            const response = await safeFetch(`${API_BASE}/analyst/reports/${reportId}/priority`, {
                method: "PATCH",
                headers: authHeaders({
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({ priority })
            });

            await handleJsonResponse(response);

            document.getElementById("caseResult").innerText = "Prioridade atualizada.";
            loadMyCases();

        } catch (error) {
            document.getElementById("caseError").innerText = error.message;
        }
    }

    async function updateNotes() {
        clearMessages();

        try {
            const reportId = selectedReportIdOrFail();
            const notes = document.getElementById("newNotes").value;

            const response = await safeFetch(`${API_BASE}/analyst/reports/${reportId}/notes`, {
                method: "PATCH",
                headers: authHeaders({
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({ notes })
            });

            await handleJsonResponse(response);

            document.getElementById("caseResult").innerText = "Notas atualizadas.";
            loadMyCases();

        } catch (error) {
            document.getElementById("caseError").innerText = error.message;
        }
    }

    async function loadAttachmentsForReport(reportId) {
        const div = document.getElementById("attachments");

        try {
            const response = await safeFetch(`${API_BASE}/analyst/reports/${reportId}/attachments`, {
                headers: authHeaders()
            });

            const data = await handleJsonResponse(response);

            if (!data.length) {
                div.innerHTML = `<div class="result">Este caso não tem anexos.</div>`;
                return;
            }

            div.innerHTML = data.map(a => `
                <div class="case-card">
                    <h3>${escapeHtml(a.originalName || `Anexo #${a.id}`)}</h3>
                    ${renderMeta("ID", a.id)}
                    ${renderMeta("Tipo", a.mimeType)}
                    ${renderMeta("Tamanho", formatBytes(a.size))}

                    <div class="case-actions">
                        <button type="button" data-action="downloadAttachment" data-attachment-id="${a.id}">Download</button>
                    </div>
                </div>
            `).join("");

        } catch (error) {
            div.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
        }
    }

    async function downloadAttachment(attachmentId) {
        try {
            const response = await safeFetch(`${API_BASE}/analyst/attachments/${attachmentId}/download`, {
                headers: authHeaders()
            });

            if (!response.ok) {
                throw new Error("Erro no download.");
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");

            a.href = url;
            a.download = `attachment-${attachmentId}`;
            document.body.appendChild(a);
            a.click();
            a.remove();

            window.URL.revokeObjectURL(url);

        } catch (error) {
            alert(error.message);
        }
    }


document.addEventListener("click", event => {
    const button = event.target.closest("[data-action]");
    if (!button) return;

    const actions = {
        showPage: () => showPage(button.dataset.page),
        logout: () => logout(),
        login,
        loadSubmittedReports,
        loadMyCases,
        updateStatus,
        updatePriority,
        updateNotes,
        loadHistory,
        assignToMe: () => assignToMe(Number(button.dataset.reportId)),
        selectCase: () => selectCase(Number(button.dataset.reportId), button.dataset.priority, button.dataset.notes || ""),
        viewCaseOnly: () => viewCaseOnly(Number(button.dataset.reportId), button.dataset.priority, button.dataset.notes || ""),
        generateCasePackage: () => generateCasePackage(Number(button.dataset.reportId)),
        downloadAttachment: () => downloadAttachment(Number(button.dataset.attachmentId))
    };

    const action = actions[button.dataset.action];
    if (action) action();
});
