(function () {
    const SESSION_KEY = "ghostreport.auth.session";
    let memorySession = null;

    function storage() {
        try {
            const probe = "__ghostreport_probe__";
            window.sessionStorage.setItem(probe, probe);
            window.sessionStorage.removeItem(probe);
            return window.sessionStorage;
        } catch {
            return null;
        }
    }

    function now() {
        return Date.now();
    }

    function normalizeRole(role) {
        return String(role || "").toUpperCase();
    }

    function normalizeRoles(roles) {
        return (Array.isArray(roles) ? roles : [roles])
            .map(normalizeRole)
            .filter(Boolean);
    }

    function readSession() {
        const store = storage();
        const raw = store ? store.getItem(SESSION_KEY) : null;
        const candidate = raw || (memorySession ? JSON.stringify(memorySession) : null);

        if (!candidate) {
            return null;
        }

        try {
            const session = JSON.parse(candidate);
            if (!session.token || isExpired(session)) {
                clearSession();
                return null;
            }
            return session;
        } catch {
            clearSession();
            return null;
        }
    }

    function persistSession(session) {
        memorySession = session;
        const store = storage();
        if (store) {
            store.setItem(SESSION_KEY, JSON.stringify(session));
        }
    }

    function isExpired(session) {
        return Number(session.expiresAt || 0) > 0 && Number(session.expiresAt) <= now();
    }

    function setSession(authResponse) {
        if (!authResponse || !authResponse.token) {
            clearSession();
            throw new Error("Resposta de autenticação inválida.");
        }

        const tokenType = authResponse.tokenType || "Bearer";
        const expiresInSeconds = Number(authResponse.expiresInSeconds || 0);
        const session = {
            token: authResponse.token,
            tokenType,
            username: authResponse.username || "",
            role: normalizeRole(authResponse.role),
            expiresAt: expiresInSeconds > 0 ? now() + expiresInSeconds * 1000 : null
        };

        persistSession(session);
        return session;
    }

    function clearSession() {
        memorySession = null;
        const store = storage();
        if (store) {
            store.removeItem(SESSION_KEY);
        }
    }

    function getSession(allowedRoles = []) {
        const session = readSession();
        if (!session) {
            return null;
        }

        const roles = normalizeRoles(allowedRoles);
        if (roles.length && !roles.includes(normalizeRole(session.role))) {
            return null;
        }

        return session;
    }

    function requireSession(allowedRoles = []) {
        const session = readSession();
        if (!session) {
            throw new Error("Sessão inválida. Faz login novamente.");
        }

        const roles = normalizeRoles(allowedRoles);
        if (roles.length && !roles.includes(normalizeRole(session.role))) {
            throw new Error("Sem permissões para esta área.");
        }

        return session;
    }

    function getAuthHeader(allowedRoles = []) {
        const session = requireSession(allowedRoles);
        return `${session.tokenType || "Bearer"} ${session.token}`;
    }

    async function authenticatedFetch(url, options = {}, config = {}) {
        const headers = {
            ...(options.headers || {}),
            "Authorization": getAuthHeader(config.allowedRoles || [])
        };
        const fetchOptions = typeof csrfFetchOptions === "function"
            ? csrfFetchOptions({ ...options, headers })
            : { ...options, headers };
        const response = await fetch(url, fetchOptions);

        if (response.status === 401) {
            clearSession();
            throw new Error(config.invalidSessionMessage || "Sessão expirada. Faz login novamente.");
        }

        if (response.status === 403) {
            throw new Error(config.forbiddenMessage || "Sem permissões para executar esta ação.");
        }

        return response;
    }

    async function logout(authHeader) {
        const header = authHeader || (() => {
            try {
                return getAuthHeader();
            } catch {
                return null;
            }
        })();

        try {
            if (header && typeof revokeCurrentToken === "function") {
                await revokeCurrentToken(header);
            }
        } finally {
            clearSession();
        }
    }

    window.GhostReportAuth = {
        setSession,
        getSession,
        requireSession,
        getAuthHeader,
        authenticatedFetch,
        clearSession,
        logout
    };
})();
