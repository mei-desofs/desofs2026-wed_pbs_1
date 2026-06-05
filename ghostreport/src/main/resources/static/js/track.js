const params = new URLSearchParams(window.location.search);
    document.getElementById("trackingCode").value = params.get("code") || "";

    document.getElementById("verifyForm").addEventListener("submit", async (e) => {
        e.preventDefault();

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

            resultDiv.innerHTML = `
                <div class="result">
                    <strong>Denuncia encontrada</strong><br><br>

                    <b>Categoria:</b> ${escapeHtml(data.category)}<br>
                    <b>Estado:</b> ${escapeHtml(data.status)}<br>

                    <hr>

                    <h3>Anexos</h3>
                    <div id="attachmentsList">A carregar...</div>
                </div>
            `;

            loadAttachments(data.id, trackingCode);

        } catch (error) {
            resultDiv.innerHTML = `<div class="error">${escapeHtml(error.message)}</div>`;
        }
    });

    async function loadAttachments(reportId, trackingCode) {
        const listDiv = document.getElementById("attachmentsList");

        try {
            const response = await fetch(`${API_BASE}/reports/${reportId}/attachments/list`, csrfFetchOptions({
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ trackingCode })
            }));

            const data = await handleJsonResponse(response);

            if (!data.length) {
                listDiv.innerHTML = "Sem anexos.";
                return;
            }

            listDiv.innerHTML = data.map(a => `
                <div class="card">
                    <strong>${escapeHtml(a.originalName || `Anexo #${a.id}`)}</strong><br>
                    Tipo: ${escapeHtml(a.mimeType || "desconhecido")}<br>
                    Tamanho: ${formatBytes(a.size)}
                </div>
            `).join("");

        } catch (err) {
            listDiv.innerHTML = "Erro ao carregar anexos";
        }
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
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
