const {
    element,
    setChildren,
    setText,
    message,
    metaLine,
    actionButton
} = window.GhostReportDom;

let analystAuth = null;
let analystUsername = null;
let analystMfaChallengeId = null;
const ANALYST_ALLOWED_ROLES = ["ANALYST", "ADMIN"];

function clearMessages() {
    ["loginError", "mfaError", "globalResult", "globalError", "caseResult", "caseError"].forEach(id => setText(id, ""));
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

function showPage(pageId) {
    clearMessages();

    document.querySelectorAll(".analyst-page").forEach(page => {
        page.classList.add("hidden");
        page.style.display = "none";
    });

    const page = document.getElementById(pageId);
    page.classList.remove("hidden");
    page.style.display = "block";

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
    analystAuth = window.GhostReportAuth.getAuthHeader(ANALYST_ALLOWED_ROLES);

    return {
        "Authorization": analystAuth,
        ...extra
    };
}

async function safeFetch(url, options = {}) {
    const response = typeof csrfFetch === "function"
        ? await csrfFetch(url, options)
        : await fetch(url, typeof csrfFetchOptions === "function" ? csrfFetchOptions(options) : options);
    const authFlowRequest = String(url).includes("/auth/login") || String(url).includes("/auth/mfa/verify");

    if (!authFlowRequest && response.status === 401) {
        analystAuth = null;
        analystUsername = null;
        window.GhostReportAuth.clearSession();
        throw new Error("Sessão expirada. Faz login novamente.");
    }

    if (!authFlowRequest && response.status === 403) {
        throw new Error("Sem permissões para executar esta ação.");
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
    hideElement("analystLoginPanel");
    hideElement("loginSection");
    hideElement("mfaSection");
    showElement("dashboard");

    hideElement("publicNav");
    showElement("analystNav", "flex");

    showPage("submittedPage");
    loadSubmittedReports();
    loadMyCases();
}

async function login() {
    clearMessages();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
        setText("loginError", "Preenche utilizador e password.");
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

        if (loginData.mfaRequired) {
            analystAuth = null;
            window.GhostReportAuth.clearSession();
            analystUsername = loginData.username;
            analystMfaChallengeId = loginData.mfaChallengeId;
            hideElement("loginSection");
            showElement("mfaSection");
            return;
        }

        window.GhostReportAuth.setSession(loginData);
        analystAuth = window.GhostReportAuth.getAuthHeader(ANALYST_ALLOWED_ROLES);

        await validateSession();
        analystUsername = loginData.username;

        showDashboard();
    } catch (error) {
        analystAuth = null;
        analystUsername = null;
        window.GhostReportAuth.clearSession();
        setText("loginError", error.message || "Login inválido.");
    }
}

async function verifyAnalystMfa() {
    clearMessages();

    const code = document.getElementById("mfaCode").value.trim();
    if (!analystMfaChallengeId || !code) {
        setText("mfaError", "Introduz o codigo de verificacao.");
        return;
    }

    try {
        const response = await safeFetch(`${API_BASE}/auth/mfa/verify`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                challengeId: analystMfaChallengeId,
                code
            })
        });
        const data = await handleJsonResponse(response);
        window.GhostReportAuth.setSession(data);
        analystAuth = window.GhostReportAuth.getAuthHeader(ANALYST_ALLOWED_ROLES);
        analystUsername = data.username;
        analystMfaChallengeId = null;
        document.getElementById("mfaCode").value = "";

        await validateSession();
        showDashboard();
    } catch (error) {
        analystAuth = null;
        window.GhostReportAuth.clearSession();
        setText("mfaError", error.message || "Codigo invalido ou expirado.");
    }
}

function cancelAnalystMfa() {
    analystMfaChallengeId = null;
    analystUsername = null;
    window.GhostReportAuth.clearSession();
    document.getElementById("mfaCode").value = "";
    hideElement("mfaSection");
    showElement("loginSection");
}

async function logout(reload = true) {
    await window.GhostReportAuth.logout(analystAuth);

    analystAuth = null;
    analystUsername = null;
    analystMfaChallengeId = null;

    if (reload) {
        location.reload();
    } else {
        showElement("publicNav", "flex");
        hideElement("analystNav");
        hideElement("dashboard");
        hideElement("mfaSection");
        showElement("analystLoginPanel");
        showElement("loginSection", "flex");
    }
}

async function restoreAnalystSession() {
    const session = window.GhostReportAuth.getSession(ANALYST_ALLOWED_ROLES);
    if (!session) {
        return;
    }

    try {
        analystAuth = window.GhostReportAuth.getAuthHeader(ANALYST_ALLOWED_ROLES);
        analystUsername = session.username;
        await validateSession();
        showDashboard();
    } catch {
        analystAuth = null;
        analystUsername = null;
        window.GhostReportAuth.clearSession();
    }
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
    setText("selectedReportLabel", reportId);
    document.getElementById("newNotes").value = notes || "";

    if (priority) {
        document.getElementById("newPriority").value = priority;
    }

    setText("caseResult", `Caso ${reportId} selecionado.`);
    showPage("managePage");
    loadAttachmentsForReport(reportId);
}

function renderSubmittedReport(report) {
    return element("div", { className: "case-card" },
        element("h3", { text: report.title || `Denúncia #${report.id}` }),
        metaLine("Report ID", report.id),
        metaLine("Categoria", report.category),
        metaLine("Estado", report.status),
        metaLine("Descrição", report.description),
        element("div", { className: "case-actions" },
            actionButton("Assumir caso", "assignToMe", { reportId: report.id }),
            actionButton("Selecionar", "selectCase", { reportId: report.id })
        )
    );
}

function renderCase(caseReview, readonly = false) {
    const action = readonly ? "viewCaseOnly" : "selectCase";
    const label = readonly ? "Ver detalhes" : "Selecionar";

    return element("div", { className: "case-card" },
        element("h3", { text: `Caso #${caseReview.reportId}` }),
        metaLine("CaseReview ID", caseReview.caseReviewId),
        metaLine("Analista", caseReview.assignedAnalystUsername),
        metaLine("Prioridade", caseReview.priority),
        metaLine("Estado", caseReview.reportStatus),
        metaLine("Notas", caseReview.notes),
        element("div", { className: "case-actions" },
            actionButton(label, action, {
                reportId: caseReview.reportId,
                priority: caseReview.priority || "MEDIUM",
                notes: caseReview.notes || ""
            }),
            readonly ? actionButton("Gerar pacote", "generateCasePackage", { reportId: caseReview.reportId }, "package-btn") : null
        ),
        readonly ? element("div", { attrs: { id: `package-result-${caseReview.reportId}` } }) : null
    );
}

async function loadSubmittedReports() {
    const div = document.getElementById("submittedReports");
    const count = document.getElementById("submittedCount");

    try {
        const response = await safeFetch(`${API_BASE}/analyst/reports`, {
            headers: authHeaders()
        });

        const data = await handleJsonResponse(response);
        const submitted = Array.isArray(data) ? data.filter(r => r.status === "SUBMITTED") : [];
        count.textContent = submitted.length;

        if (!submitted.length) {
            setChildren(div, message("result", "Não existem denúncias pendentes."));
            return;
        }

        setChildren(div, submitted.map(renderSubmittedReport));
    } catch (error) {
        setChildren(div, message("error", error.message));
        if (count) count.textContent = "0";
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
        const cases = Array.isArray(data) ? data : [];
        const activeCases = cases.filter(c => c.reportStatus !== "RESOLVED" && c.reportStatus !== "REJECTED");
        count.textContent = activeCases.length;

        if (!activeCases.length) {
            setChildren(div, message("result", "Não tens casos atribuídos."));
            return;
        }

        setChildren(div, activeCases.map(item => renderCase(item, false)));
    } catch (error) {
        setChildren(div, message("error", error.message));
        if (count) count.textContent = "0";
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
        const cases = Array.isArray(data) ? data : [];
        const historyCases = cases.filter(c => c.reportStatus === "RESOLVED" || c.reportStatus === "REJECTED");
        count.textContent = historyCases.length;

        if (!historyCases.length) {
            setChildren(div, message("result", "Ainda não existem casos no histórico."));
            return;
        }

        setChildren(div, historyCases.map(item => renderCase(item, true)));
    } catch (error) {
        setChildren(div, message("error", error.message));
        if (count) count.textContent = "0";
    }
}

async function generateCasePackage(reportId = null) {
    clearMessages();

    try {
        const id = reportId || selectedReportIdOrFail();
        const response = await safeFetch(`${API_BASE}/analyst/reports/${encodeURIComponent(id)}/case-package`, {
            method: "POST",
            headers: authHeaders()
        });

        const data = await handleJsonResponse(response);
        const box = document.getElementById(`package-result-${id}`);
        const files = Array.isArray(data.generatedFiles) ? data.generatedFiles : [];
        const result = element("div", { className: "package-result" },
            element("strong", { text: "Pacote de evidências gerado" }),
            element("p", { text: `Caso #${data.reportId}` }),
            element("p", { text: `Estado: ${data.status}` }),
            element("p", { text: "Ficheiros gerados:" }),
            element("ul", {}, files.map(file => element("li", { text: file })))
        );

        if (box) {
            setChildren(box, result);
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
        const response = await safeFetch(`${API_BASE}/analyst/reports/${encodeURIComponent(reportId)}/assign`, {
            method: "POST",
            headers: authHeaders()
        });

        await handleJsonResponse(response);

        setText("globalResult", `Caso ${reportId} atribuído com sucesso.`);

        await loadSubmittedReports();
        await loadMyCases();
        showPage("myCasesPage");
    } catch (error) {
        setText("globalError", error.message);
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

        const response = await safeFetch(`${API_BASE}/analyst/reports/${encodeURIComponent(reportId)}/status`, {
            method: "PATCH",
            headers: authHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify({ status })
        });

        await handleJsonResponse(response);

        setText("caseResult", "Estado atualizado.");
        loadSubmittedReports();
        loadMyCases();
        loadHistory();
    } catch (error) {
        setText("caseError", error.message);
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

        const response = await safeFetch(`${API_BASE}/analyst/reports/${encodeURIComponent(reportId)}/priority`, {
            method: "PATCH",
            headers: authHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify({ priority })
        });

        await handleJsonResponse(response);

        setText("caseResult", "Prioridade atualizada.");
        loadMyCases();
    } catch (error) {
        setText("caseError", error.message);
    }
}

async function updateNotes() {
    clearMessages();

    try {
        const reportId = selectedReportIdOrFail();
        const notes = document.getElementById("newNotes").value;

        const response = await safeFetch(`${API_BASE}/analyst/reports/${encodeURIComponent(reportId)}/notes`, {
            method: "PATCH",
            headers: authHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify({ notes })
        });

        await handleJsonResponse(response);

        setText("caseResult", "Notas atualizadas.");
        loadMyCases();
    } catch (error) {
        setText("caseError", error.message);
    }
}

function renderAttachment(attachment) {
    return element("div", { className: "case-card" },
        element("h3", { text: attachment.originalName || `Anexo #${attachment.id}` }),
        metaLine("ID", attachment.id),
        metaLine("Tipo", attachment.mimeType),
        metaLine("Tamanho", formatBytes(attachment.size)),
        element("div", { className: "case-actions" },
            actionButton("Download", "downloadAttachment", { attachmentId: attachment.id })
        )
    );
}

async function loadAttachmentsForReport(reportId) {
    const div = document.getElementById("attachments");

    try {
        const response = await safeFetch(`${API_BASE}/analyst/reports/${encodeURIComponent(reportId)}/attachments`, {
            headers: authHeaders()
        });

        const data = await handleJsonResponse(response);
        const attachments = Array.isArray(data) ? data : [];

        if (!attachments.length) {
            setChildren(div, message("result", "Este caso não tem anexos."));
            return;
        }

        setChildren(div, attachments.map(renderAttachment));
    } catch (error) {
        setChildren(div, message("error", error.message));
    }
}

async function downloadAttachment(attachmentId) {
    try {
        const response = await safeFetch(`${API_BASE}/analyst/attachments/${encodeURIComponent(attachmentId)}/download`, {
            headers: authHeaders()
        });

        if (!response.ok) {
            throw new Error("Erro no download.");
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");

        link.href = url;
        link.download = `attachment-${attachmentId}`;
        document.body.appendChild(link);
        link.click();
        link.remove();

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
        verifyAnalystMfa,
        cancelAnalystMfa,
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

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", restoreAnalystSession);
} else {
    restoreAnalystSession();
}
