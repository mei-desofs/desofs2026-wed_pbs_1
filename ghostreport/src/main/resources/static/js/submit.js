document.getElementById("reportForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const { element, message, setChildren } = window.GhostReportDom;
    const title = document.getElementById("title").value;
    const description = document.getElementById("description").value;
    const category = document.getElementById("category").value;
    const files = document.getElementById("files").files;

    const resultDiv = document.getElementById("result");
    const submitButton = document.getElementById("submitButton");
    submitButton.disabled = true;
    setChildren(resultDiv, message("result", "A criar denúncia..."));

    try {
        const response = await fetch(`${API_BASE}/reports`, csrfFetchOptions({
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                title,
                description,
                category
            })
        }));

        const data = await handleJsonResponse(response);

        if (files.length > 0) {
            setChildren(resultDiv, message("result", `Denúncia criada. A enviar ${files.length} anexo(s)...`));

            const formData = new FormData();
            for (let i = 0; i < files.length; i++) {
                formData.append("files", files[i]);
            }
            formData.append("trackingCode", data.trackingCode);

            const uploadResponse = await fetch(`${API_BASE}/reports/${encodeURIComponent(data.id)}/attachments`, csrfFetchOptions({
                method: "POST",
                body: formData
            }));

            if (!uploadResponse.ok) {
                const errorText = await uploadResponse.text();
                throw new Error(`Erro ao guardar anexos: ${uploadResponse.status} - ${errorText}`);
            }
        }

        const trackingLink = element("a", { attrs: { href: "/track.html" } }, "Acompanhar denúncia");
        setChildren(resultDiv,
            element("div", { className: "result" },
                element("strong", { text: "Denúncia criada com sucesso." }),
                element("br"),
                "Código: ",
                element("strong", { text: data.trackingCode }),
                element("br"),
                element("br"),
                "Guarda este código. Por segurança, ele não é colocado no URL.",
                element("br"),
                trackingLink
            )
        );
    } catch (error) {
        setChildren(resultDiv, message("error", error.message));
        submitButton.disabled = false;
    }
});
