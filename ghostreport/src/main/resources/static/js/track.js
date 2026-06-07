window.GhostReportDom.clearQueryString();

document.getElementById("verifyForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const { element, message, metaLine, setChildren } = window.GhostReportDom;
    const trackingCode = document.getElementById("trackingCode").value;
    const resultDiv = document.getElementById("result");

    try {
        const response = await fetch(`${API_BASE}/reports/verify`, csrfFetchOptions({
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ trackingCode })
        }));

        const data = await handleJsonResponse(response);
        const attachmentsList = element("div", { attrs: { id: "attachmentsList" } }, "A carregar...");

        setChildren(resultDiv,
            element("div", { className: "result" },
                element("strong", { text: "Denúncia encontrada" }),
                element("br"),
                element("br"),
                metaLine("Categoria", data.category),
                metaLine("Estado", data.status),
                element("hr"),
                element("h3", { text: "Anexos" }),
                attachmentsList
            )
        );

        loadAttachments(data.id, trackingCode);
    } catch (error) {
        setChildren(resultDiv, message("error", error.message));
    }
});

async function loadAttachments(reportId, trackingCode) {
    const { element, message, metaLine, setChildren } = window.GhostReportDom;
    const listDiv = document.getElementById("attachmentsList");

    try {
        const response = await fetch(`${API_BASE}/reports/${encodeURIComponent(reportId)}/attachments/list`, csrfFetchOptions({
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ trackingCode })
        }));

        const data = await handleJsonResponse(response);
        const attachments = Array.isArray(data) ? data : [];

        if (!attachments.length) {
            setChildren(listDiv, "Sem anexos.");
            return;
        }

        setChildren(listDiv, attachments.map(attachment => element("div", { className: "card" },
            element("strong", { text: attachment.originalName || `Anexo #${attachment.id}` }),
            metaLine("Tipo", attachment.mimeType || "desconhecido"),
            metaLine("Tamanho", formatBytes(attachment.size))
        )));
    } catch {
        setChildren(listDiv, message("error", "Erro ao carregar anexos"));
    }
}

function formatBytes(bytes) {
    if (!bytes) {
        return "0 B";
    }

    const units = ["B", "KB", "MB"];
    let size = Number(bytes);
    let unit = 0;

    while (size >= 1024 && unit < units.length - 1) {
        size = size / 1024;
        unit++;
    }

    return `${size.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}
