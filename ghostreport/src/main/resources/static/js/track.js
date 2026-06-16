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
        const attachmentCount = Number(data.attachmentCount || 0);
        const attachmentText = attachmentCount === 1
            ? "Existe 1 anexo associado a esta denúncia."
            : `Existem ${attachmentCount} anexos associados a esta denúncia.`;

        setChildren(resultDiv,
            element("div", { className: "result" },
                element("strong", { text: "Denúncia encontrada" }),
                element("br"),
                element("br"),
                metaLine("Categoria", data.category),
                metaLine("Estado", data.status),
                element("hr"),
                element("h3", { text: "Anexos" }),
                element("p", { text: attachmentCount > 0
                        ? `${attachmentText} Os detalhes e o download são tratados pela equipa interna para proteger dados sensíveis.`
                        : "Sem anexos associados." })
            )
        );
    } catch (error) {
        setChildren(resultDiv, message("error", error.message));
    }
});
