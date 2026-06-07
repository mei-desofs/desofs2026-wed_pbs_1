document.getElementById("reportForm").addEventListener("submit", async (e) => {
        e.preventDefault();

        const title = document.getElementById("title").value;
        const description = document.getElementById("description").value;
        const category = document.getElementById("category").value;
        const files = document.getElementById("files").files;

        const resultDiv = document.getElementById("result");
        const submitButton = document.getElementById("submitButton");
        submitButton.disabled = true;
        resultDiv.innerHTML = `<div class="result">A criar denúncia...</div>`;

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
                resultDiv.innerHTML = `<div class="result">Denúncia criada. A enviar ${files.length} anexo(s)...</div>`;

                const formData = new FormData();
                for (let i = 0; i < files.length; i++) {
                    formData.append("files", files[i]);
                }
                formData.append("trackingCode", data.trackingCode);

                const uploadResponse = await fetch(`${API_BASE}/reports/${data.id}/attachments`, csrfFetchOptions({
                    method: "POST",
                    body: formData
                }));

                if (!uploadResponse.ok) {
                    const errorText = await uploadResponse.text();
                    console.error("Upload failed:", uploadResponse.status, errorText);
                    throw new Error(`Erro ao guardar anexos: ${uploadResponse.status} - ${errorText}`);
                }
            }

            resultDiv.innerHTML = `
            <div class="result">
                <strong>Denúncia criada com sucesso.</strong><br>
                Código: <strong>${data.trackingCode}</strong><br><br>
                A redirecionar...
            </div>
        `;

            setTimeout(() => {
                window.location.href = `/track.html?code=${data.trackingCode}&id=${data.id}`;
            }, 1500);

        } catch (error) {
            resultDiv.innerHTML = `<div class="error">${error.message}</div>`;
            submitButton.disabled = false;
        }
    });
