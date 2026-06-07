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

function clearMessages() {
    ["loginError", "globalResult", "globalError", "caseResult", "caseError"].forEach(id => setText(id, ""));
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
    const fetchOptions = typeof csrfFetchOptions === "function"
        ? csrfFetchOptions(options)
        : options;
    const response = await fetch(url, fetchOptions);

    if (response.status === 401 || response.status === 403) {
        analystAuth = null;
        analystUsername = null;
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
        analystAuth = `${loginData.tokenType} ${loginData.token}`;

        await validateSession();
        analystUsername = loginData.username;

        showDashboard();
    } catch (error) {
        analystAuth = null;
        analystUsername = null;
        setText("loginError", error.message || "Login inválido.");
    }
}

async function logout(reload = true) {
    if (typeof revokeCurrentToken === "function") {
        await revokeCurrentToken(analystAuth);
    }

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
