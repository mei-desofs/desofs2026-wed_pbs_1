window.GhostReportDom = (() => {
    function asText(value) {
        return String(value ?? "");
    }

    function asDisplayText(value) {
        const text = asText(value).trim();
        return text || "-";
    }

    function normalizeChildren(children) {
        return children
            .flat(Infinity)
            .filter(child => child !== null && child !== undefined)
            .map(child => child instanceof Node ? child : document.createTextNode(asText(child)));
    }

    function element(tagName, options = {}, ...children) {
        const node = document.createElement(tagName);

        if (options.className) {
            node.className = asText(options.className);
        }

        if (options.text !== undefined) {
            node.textContent = asText(options.text);
        }

        if (options.dataset) {
            Object.entries(options.dataset).forEach(([key, value]) => {
                if (value !== null && value !== undefined) {
                    node.dataset[key] = asText(value);
                }
            });
        }

        if (options.attrs) {
            Object.entries(options.attrs).forEach(([key, value]) => {
                if (value !== null && value !== undefined) {
                    node.setAttribute(key, asText(value));
                }
            });
        }

        node.append(...normalizeChildren(children));
        return node;
    }

    function setChildren(target, ...children) {
        const node = typeof target === "string" ? document.getElementById(target) : target;
        if (!node) {
            return;
        }

        node.replaceChildren(...normalizeChildren(children));
    }

    function setText(target, value) {
        const node = typeof target === "string" ? document.getElementById(target) : target;
        if (node) {
            node.textContent = asText(value);
        }
    }

    function message(className, text) {
        return element("div", { className, text });
    }

    function metaLine(label, value) {
        return element("div", { className: "meta-line" },
            element("span", { className: "meta-label", text: label }),
            element("span", { text: asDisplayText(value) })
        );
    }

    function actionButton(text, action, dataset = {}, className = "") {
        return element("button", {
            className,
            attrs: { type: "button" },
            dataset: { action, ...dataset }
        }, text);
    }

    function clearQueryString() {
        if (window.location.search) {
            window.history.replaceState(null, document.title, `${window.location.pathname}${window.location.hash || ""}`);
        }
    }

    return {
        asDisplayText,
        element,
        setChildren,
        setText,
        message,
        metaLine,
        actionButton,
        clearQueryString
    };
})();
