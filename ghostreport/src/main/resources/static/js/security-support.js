(function () {
    "use strict";

    function hasRequiredBrowserSecurityFeatures() {
        return Boolean(
            window.Promise &&
            window.fetch &&
            window.crypto &&
            typeof window.crypto.getRandomValues === "function" &&
            window.TextEncoder &&
            document.querySelector &&
            document.createElement &&
            window.Element &&
            window.Element.prototype.replaceChildren
        );
    }

    function disableInteractiveControls() {
        var controls = document.querySelectorAll("form, button, input, textarea, select");
        for (var index = 0; index < controls.length; index += 1) {
            var node = controls[index];
            if (node.tagName === "FORM") {
                node.dataset.securityUnsupported = "true";
                continue;
            }
            node.disabled = true;
        }
    }

    function showUnsupportedBrowserWarning() {
        document.documentElement.dataset.securityUnsupported = "true";

        var warning = document.createElement("div");
        warning.className = "security-support-warning";
        warning.setAttribute("role", "alert");
        warning.textContent = "Este browser nao suporta os mecanismos de seguranca necessarios para usar o GhostReport.";

        document.body.insertBefore(warning, document.body.firstChild);
        disableInteractiveControls();
    }

    if (!hasRequiredBrowserSecurityFeatures()) {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", showUnsupportedBrowserWarning);
        } else {
            showUnsupportedBrowserWarning();
        }
    }
})();
