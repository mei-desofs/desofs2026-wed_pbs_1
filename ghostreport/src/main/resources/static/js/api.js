const API_BASE = "http://localhost:8081";

async function handleJsonResponse(response) {
    const contentType = response.headers.get("content-type");

    let data = null;
    if (contentType && contentType.includes("application/json")) {
        data = await response.json();
    } else {
        data = await response.text();
    }

    if (!response.ok) {
        const errorMessage = data?.error || data?.message || JSON.stringify(data);
        throw new Error(errorMessage);
    }

    return data;
}

function basicAuthHeader(username, password) {
    return "Basic " + btoa(`${username}:${password}`);
}