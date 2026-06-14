#!/usr/bin/env python3
"""Generate runtime security evidence against a running GhostReport instance."""

from __future__ import annotations

import argparse
import http.cookiejar
import json
import mimetypes
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path


class RuntimeProbe:
    def __init__(self, base_url: str, output_dir: Path, app_log: Path) -> None:
        self.base_url = base_url.rstrip("/")
        self.output_dir = output_dir
        self.app_log = app_log
        self.cookie_jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookie_jar))
        self.rows: list[tuple[str, str, str]] = []
        self.outputs: dict[str, object] = {}

    def run(self) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.get("/index.html", "Public frontend and CSRF cookie bootstrap")
        self.get("/admin/users", "Protected admin endpoint without token")
        self.get("/admin/users", "Protected admin endpoint with invalid JWT", token="invalid-ci-runtime-token")

        self.public_report_probes()
        self.upload_probes()
        self.authenticated_role_probes()
        self.error_handling_probes()

        self.write_artifacts()

    def public_report_probes(self) -> None:
        valid = {
            "title": "Runtime probe report",
            "description": "Runtime security probe with valid input for GhostReport evidence.",
            "category": "Ethics",
        }
        status, body = self.post_json("/reports", valid, "Valid anonymous report creation")
        report_id = body.get("id") if isinstance(body, dict) else None
        tracking_code = body.get("trackingCode") if isinstance(body, dict) else None
        self.outputs["report_id"] = report_id
        self.outputs["tracking_code_present"] = bool(tracking_code)
        self.outputs["valid_report_exposes_internal_hash"] = self.contains_internal_data(body)

        invalid = {"title": "", "description": "short", "category": ""}
        self.post_json("/reports", invalid, "Invalid report required-field validation")

        dangerous = {
            "title": "<script>alert(1)</script>",
            "description": "Runtime probe with <img src=x onerror=alert(1)> characters.",
            "category": "Ethics",
        }
        self.post_json("/reports", dangerous, "Dangerous characters treated as data")

        mass_assignment = {
            "title": "Mass assignment probe",
            "description": "Runtime probe trying to set fields ignored by DTO binding.",
            "category": "Ethics",
            "role": "ADMIN",
            "status": "RESOLVED",
            "id": 9999,
            "trackingCode": "FORCED-CODE",
        }
        self.post_json("/reports", mass_assignment, "Mass-assignment attempt through extra JSON fields")

        if tracking_code:
            self.post_json("/reports/verify", {"trackingCode": tracking_code}, "Valid tracking code verification")
        self.post_json("/reports/verify", {"trackingCode": "INVALID-CODE"}, "Invalid tracking code verification")
        last_status = "not exercised"
        for _ in range(6):
            last_status, _ = self.post_json(
                "/reports/verify",
                {"trackingCode": "INVALID-CODE"},
                "Repeated invalid tracking attempts",
                record=False,
            )
        self.rows.append(("POST /reports/verify", "Repeated invalid tracking attempts", str(last_status)))

    def upload_probes(self) -> None:
        report_id = self.outputs.get("report_id")
        tracking_code = self.outputs.get("tracking_code_present")
        actual_tracking_code = None
        # Keep the actual tracking code out of generated evidence files.
        valid_response = self.read_json_artifact("post_reports_valid_anonymous_report_creation.json")
        if isinstance(valid_response, dict):
            actual_tracking_code = valid_response.get("trackingCode")

        if not report_id or not actual_tracking_code:
            self.rows.append(("POST /reports/{id}/attachments", "Upload probes", "not exercised"))
            return

        self.post_multipart(
            f"/reports/{report_id}/attachments",
            {"trackingCode": actual_tracking_code},
            [("files", "runtime-allowed.pdf", b"%PDF-1.4\n% runtime probe\n", "application/pdf")],
            "Allowed upload probe",
        )
        self.post_multipart(
            f"/reports/{report_id}/attachments",
            {"trackingCode": actual_tracking_code},
            [("files", "runtime-blocked.exe", b"MZ suspicious executable content\n", "application/octet-stream")],
            "Blocked extension upload probe",
        )
        self.post_multipart(
            f"/reports/{report_id}/attachments",
            {"trackingCode": actual_tracking_code},
            [("files", "runtime-fake.pdf", b"not really a pdf\n", "application/pdf")],
            "Suspicious content signature upload probe",
        )
        self.post_multipart(
            f"/reports/{report_id}/attachments",
            {"trackingCode": actual_tracking_code},
            [("files", "../runtime-traversal.txt", b"Traversal probe\n", "text/plain")],
            "Path traversal filename upload probe",
        )

        self.rows.append(("Upload max size", "Maximum size validation", "covered by Maven runtime tests"))
        self.rows.append(("ZIP Slip", "ZIP traversal validation", "covered by backup/package tests"))

    def authenticated_role_probes(self) -> None:
        credentials = {
            "admin": ("AdminPassword123!", "admin@ghostreport.local"),
            "analyst": ("AnalystPassword123!", "analyst@ghostreport.local"),
            "auditor": ("AuditorPassword123!", "auditor@ghostreport.local"),
        }
        tokens: dict[str, str] = {}
        for username, (password, email) in credentials.items():
            token = self.login_with_mfa(username, password, email)
            if token:
                tokens[username] = token

        admin = tokens.get("admin")
        analyst = tokens.get("analyst")
        auditor = tokens.get("auditor")

        if admin:
            self.get("/admin/users", "ADMIN accesses /admin/users", token=admin)
            self.get("/analyst/panel", "ADMIN accesses analyst oversight route", token=admin)
            self.get("/audit/logs", "ADMIN accesses audit logs", token=admin)
        if analyst:
            self.get("/analyst/panel", "ANALYST accesses analyst endpoint", token=analyst)
            self.get("/admin/users", "ANALYST denied from admin endpoint", token=analyst)
            self.get("/audit/logs", "ANALYST denied from audit endpoint", token=analyst)
        if auditor:
            self.get("/audit/logs", "AUDITOR accesses audit endpoint", token=auditor)
            self.get("/admin/users", "AUDITOR denied from admin endpoint", token=auditor)
            self.get("/analyst/panel", "AUDITOR denied from analyst endpoint", token=auditor)

    def error_handling_probes(self) -> None:
        status, body = self.request("GET", "/definitely-not-a-real-endpoint")
        self.rows.append(("GET /definitely-not-a-real-endpoint", "Unknown endpoint error handling", str(status)))
        self.outputs["unknown_endpoint_exposes_stacktrace"] = self.body_has_stacktrace(body)

    def login_with_mfa(self, username: str, password: str, email: str) -> str | None:
        status, body = self.post_json(
            "/auth/login",
            {"username": username, "password": password},
            f"{username.upper()} password login starts MFA",
        )
        if status != 200 or not isinstance(body, dict) or not body.get("mfaRequired"):
            return None

        code = self.wait_for_mfa_code(email)
        if not code:
            self.rows.append(("POST /auth/mfa/verify", f"{username.upper()} MFA code captured from dev log", "not found"))
            return None

        self.post_json(
            "/auth/mfa/verify",
            {"challengeId": body.get("mfaChallengeId"), "code": "000000"},
            f"{username.upper()} invalid MFA code rejected",
        )

        status, verified = self.post_json(
            "/auth/mfa/verify",
            {"challengeId": body.get("mfaChallengeId"), "code": code},
            f"{username.upper()} valid MFA emits JWT",
        )
        if status == 200 and isinstance(verified, dict):
            return verified.get("token")
        return None

    def wait_for_mfa_code(self, email: str) -> str | None:
        pattern = re.compile(rf"DEV MFA code for {re.escape(email)}:\s*(\d{{6}})")
        for _ in range(20):
            if self.app_log.exists():
                matches = pattern.findall(self.app_log.read_text(encoding="utf-8", errors="replace"))
                if matches:
                    return matches[-1]
            time.sleep(0.5)
        return None

    def get(self, path: str, purpose: str, token: str | None = None) -> tuple[int, object]:
        status, body = self.request("GET", path, token=token)
        self.rows.append((f"GET {path}", purpose, str(status)))
        return status, body

    def post_json(self, path: str, payload: dict[str, object], purpose: str, record: bool = True) -> tuple[int, object]:
        body = json.dumps(payload).encode("utf-8")
        headers = {"Content-Type": "application/json", **self.csrf_header()}
        status, response = self.request("POST", path, data=body, headers=headers)
        if record:
            self.rows.append((f"POST {path}", purpose, str(status)))
            self.write_json_artifact(f"post_{self.safe_name(path)}_{self.safe_name(purpose)}.json", response)
        return status, response

    def post_multipart(
        self,
        path: str,
        fields: dict[str, str],
        files: list[tuple[str, str, bytes, str]],
        purpose: str,
    ) -> tuple[int, object]:
        boundary = f"----ghostreport-runtime-{uuid.uuid4().hex}"
        parts: list[bytes] = []
        for name, value in fields.items():
            parts.append(f"--{boundary}\r\n".encode())
            parts.append(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode())
            parts.append(value.encode())
            parts.append(b"\r\n")
        for field, filename, content, content_type in files:
            parts.append(f"--{boundary}\r\n".encode())
            disposition = f'Content-Disposition: form-data; name="{field}"; filename="{filename}"\r\n'
            parts.append(disposition.encode())
            parts.append(f"Content-Type: {content_type or mimetypes.guess_type(filename)[0] or 'application/octet-stream'}\r\n\r\n".encode())
            parts.append(content)
            parts.append(b"\r\n")
        parts.append(f"--{boundary}--\r\n".encode())
        headers = {"Content-Type": f"multipart/form-data; boundary={boundary}", **self.csrf_header()}
        status, response = self.request("POST", path, data=b"".join(parts), headers=headers)
        self.rows.append((f"POST {path}", purpose, str(status)))
        self.write_json_artifact(f"upload_{self.safe_name(purpose)}.json", response)
        return status, response

    def request(
        self,
        method: str,
        path: str,
        data: bytes | None = None,
        headers: dict[str, str] | None = None,
        token: str | None = None,
    ) -> tuple[int, object]:
        url = f"{self.base_url}{path}"
        request_headers = headers.copy() if headers else {}
        if token:
            request_headers["Authorization"] = f"Bearer {token}"
        req = urllib.request.Request(url, data=data, headers=request_headers, method=method)
        try:
            with self.opener.open(req, timeout=20) as response:
                return response.status, self.parse_response(response.read())
        except urllib.error.HTTPError as exc:
            return exc.code, self.parse_response(exc.read())
        except Exception as exc:
            return 0, {"error": type(exc).__name__, "message": str(exc)}

    def csrf_header(self) -> dict[str, str]:
        for cookie in self.cookie_jar:
            if cookie.name == "XSRF-TOKEN":
                return {"X-XSRF-TOKEN": urllib.parse.unquote(cookie.value)}
        return {}

    def parse_response(self, raw: bytes) -> object:
        if not raw:
            return None
        text = raw.decode("utf-8", errors="replace")
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return text[:1000]

    def write_artifacts(self) -> None:
        runtime_md = self.output_dir / "runtime-endpoints.md"
        with runtime_md.open("w", encoding="utf-8") as handle:
            handle.write("| Endpoint/probe | Purpose | HTTP status / result |\n")
            handle.write("| --- | --- | --- |\n")
            for endpoint, purpose, status in self.rows:
                handle.write(f"| {endpoint} | {purpose} | {status} |\n")

        self.write_json_artifact("runtime-probe-summary.json", self.outputs)

    def write_json_artifact(self, filename: str, data: object) -> None:
        redacted = self.redact(data)
        (self.output_dir / filename).write_text(json.dumps(redacted, indent=2, sort_keys=True), encoding="utf-8")

    def read_json_artifact(self, filename: str) -> object:
        path = self.output_dir / filename
        if not path.exists():
            return None
        return json.loads(path.read_text(encoding="utf-8"))

    def redact(self, data: object) -> object:
        if isinstance(data, dict):
            redacted = {}
            for key, value in data.items():
                if key.lower() in {"token", "trackingcode", "mfachallengeid", "devmfacode"}:
                    redacted[key] = "[REDACTED]"
                else:
                    redacted[key] = self.redact(value)
            return redacted
        if isinstance(data, list):
            return [self.redact(item) for item in data]
        return data

    def contains_internal_data(self, data: object) -> bool:
        text = json.dumps(data, default=str).lower()
        return any(marker in text for marker in ["password", "hash", "stacktrace", "exception", "hibernate", "jwt_secret"])

    def body_has_stacktrace(self, data: object) -> bool:
        text = json.dumps(data, default=str).lower()
        return any(marker in text for marker in ["stacktrace", "java.", "hibernate", "org.springframework"])

    def safe_name(self, value: str) -> str:
        return re.sub(r"[^a-zA-Z0-9]+", "_", value).strip("_").lower()[:80] or "artifact"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8081")
    parser.add_argument("--output-dir", default="target/iast-evidence")
    parser.add_argument("--app-log", default="target/ghostreport-dast-app.log")
    args = parser.parse_args()

    RuntimeProbe(args.base_url, Path(args.output_dir), Path(args.app_log)).run()


if __name__ == "__main__":
    main()
