#!/usr/bin/env python3
"""Generate runtime security evidence against a running GhostReport instance."""

from __future__ import annotations

import argparse
import http.cookiejar
import json
import mimetypes
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any


SENSITIVE_KEYS = {
    "token",
    "trackingcode",
    "mfachallengeid",
    "devmfacode",
    "password",
    "currentpassword",
    "newpassword",
    "authorization",
    "jwt_secret",
    "backup_hmac_secret",
}


@dataclass
class HttpResult:
    status: int
    body: object
    headers: dict[str, str]


@dataclass
class ProbeRow:
    area: str
    endpoint: str
    method: str
    role: str
    expected: str
    obtained: str
    state: str
    notes: str


class RuntimeProbe:
    def __init__(self, base_url: str, output_dir: Path, app_log: Path) -> None:
        self.base_url = base_url.rstrip("/")
        self.output_dir = output_dir
        self.app_log = app_log
        self.cookie_jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookie_jar))
        self.rows: list[ProbeRow] = []
        self.outputs: dict[str, object] = {}
        self.tokens: dict[str, str] = {}
        self.created_report_id: int | None = None
        self.created_tracking_code: str | None = None
        self.created_attachment_id: int | None = None
        self.created_user_id: int | None = None
        self.created_backup_filename: str | None = None

    def run(self) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)

        self.public_page_probes()
        self.public_report_probes()
        self.upload_and_download_probes()
        self.authentication_probes()
        self.admin_probes()
        self.analyst_probes()
        self.auditor_probes()
        self.cross_cutting_negative_probes()
        self.logout_probe()

        self.write_artifacts()

        failed = sum(1 for row in self.rows if row.state == "failed")
        if failed:
            raise SystemExit(f"Runtime security probe failed: {failed} failed probe(s)")

    def public_page_probes(self) -> None:
        pages = ["/", "/index.html", "/submit.html", "/track.html", "/login.html", "/admin.html", "/analyst.html", "/auditor.html"]
        for page in pages:
            result = self.request("GET", page)
            if page == "/login.html" and result.status in {401, 404}:
                self.record("public", page, "GET", "anonymous", "200 if page exists; otherwise 401/404 confirms no exposed standalone login page", result, "passed", "No public login.html static page exists in this build; role pages contain their own login forms.")
                continue

            ok = result.status == 200
            body_text = self.body_text(result.body).lower()
            leaks = self.contains_sensitive_evidence(result.body) or "bearer " in body_text or self.contains_real_tracking_code(result.body)
            headers_ok = self.has_security_headers(result.headers)
            state = "passed" if ok and not leaks and headers_ok else "failed"
            notes = "public page reachable, security headers present, no obvious token/tracking exposure"
            if not headers_ok:
                notes = "missing one or more expected browser security headers"
            if leaks:
                notes = "possible sensitive value exposed in public HTML"
            self.record("public", page, "GET", "anonymous", "200, security headers, no sensitive values", result, state, notes)

    def public_report_probes(self) -> None:
        valid = {
            "title": "Runtime probe report",
            "description": "Runtime security probe with valid input for GhostReport evidence.",
            "category": "Ethics",
        }
        result = self.post_json("/reports", valid)
        body = result.body if isinstance(result.body, dict) else {}
        self.created_report_id = self.as_int(body.get("id"))
        self.created_tracking_code = body.get("trackingCode") if isinstance(body.get("trackingCode"), str) else None
        exposes_internal = self.contains_internal_data(body)
        state = "passed" if result.status in {200, 201} and self.created_report_id and self.created_tracking_code and not exposes_internal else "failed"
        self.record("public-report", "/reports", "POST", "anonymous", "2xx with report id/tracking code and no internal hash/path", result, state, "valid anonymous report creation")
        self.write_json_artifact("post_reports_valid_anonymous_report_creation.json", result.body)

        invalid = {"title": "", "description": "short", "category": ""}
        self.expect_status("public-report", "/reports", "POST", "anonymous", invalid, {400}, "invalid required-field validation")

        dangerous = {
            "title": "<script>alert(1)</script>",
            "description": "Runtime probe with <img src=x onerror=alert(1)> characters.",
            "category": "Ethics",
        }
        dangerous_result = self.post_json("/reports", dangerous)
        state = "passed" if dangerous_result.status in {200, 201, 400} and not self.body_has_stacktrace(dangerous_result.body) else "failed"
        self.record("public-report", "/reports", "POST", "anonymous", "controlled 2xx or 400 without stack trace", dangerous_result, state, "script-like characters handled as data or validation error")

        mass_assignment = {
            "title": "Mass assignment probe",
            "description": "Runtime probe trying to set fields ignored by DTO binding.",
            "category": "Ethics",
            "role": "ADMIN",
            "status": "RESOLVED",
            "id": 9999,
            "assignedAnalyst": "admin",
            "trackingCode": "FORCED-CODE",
        }
        mass_result = self.post_json("/reports", mass_assignment)
        mass_body = mass_result.body if isinstance(mass_result.body, dict) else {}
        status_forced = mass_body.get("status") == "RESOLVED" or mass_body.get("id") == 9999
        state = "passed" if mass_result.status in {200, 201} and not status_forced else "failed"
        self.record("public-report", "/reports", "POST", "anonymous", "extra fields ignored by DTO binding", mass_result, state, "mass-assignment attempt")

        if self.created_tracking_code:
            self.expect_status("tracking", "/reports/verify", "POST", "anonymous", {"trackingCode": self.created_tracking_code}, {200}, "valid tracking code")
        self.expect_status("tracking", "/reports/verify", "POST", "anonymous", {"trackingCode": "INVALID-CODE"}, {400, 404, 429}, "invalid tracking code")
        last_result = None
        for _ in range(12):
            last_result = self.post_json("/reports/verify", {"trackingCode": "INVALID-CODE"})
        if last_result:
            self.record("tracking", "/reports/verify", "POST", "anonymous", "controlled 400/404/429 after repeated invalid attempts", last_result, "passed" if last_result.status in {400, 404, 429} else "failed", "repeated invalid tracking attempts / enumeration evidence")

    def upload_and_download_probes(self) -> None:
        if not self.created_report_id or not self.created_tracking_code:
            self.skip("public-files", "/reports/{id}/attachments", "POST", "anonymous", "valid report required", "Report creation failed; upload/download probes skipped.")
            return

        allowed = self.post_multipart(
            f"/reports/{self.created_report_id}/attachments",
            {"trackingCode": self.created_tracking_code},
            [("files", "runtime-allowed.pdf", b"%PDF-1.4\n% runtime probe\n", "application/pdf")],
        )
        attachment = None
        if isinstance(allowed.body, list) and allowed.body:
            attachment = allowed.body[0] if isinstance(allowed.body[0], dict) else None
        self.created_attachment_id = self.as_int(attachment.get("id")) if attachment else None
        state = "passed" if allowed.status in {200, 201} and self.created_attachment_id and not self.contains_internal_data(allowed.body) else "failed"
        self.record("public-files", f"/reports/{self.created_report_id}/attachments", "POST", "anonymous", "2xx and attachment metadata without internal path/hash leak", allowed, state, "allowed PDF upload")
        self.write_json_artifact("upload_allowed_pdf.json", allowed.body)

        self.multipart_expect("public-files", "forbidden extension", "runtime-blocked.exe", b"MZ suspicious executable content\n", "application/octet-stream", {400})
        self.multipart_expect("public-files", "path traversal filename", "../runtime-traversal.txt", b"Traversal probe\n", "text/plain", {400})
        self.multipart_expect("public-files", "MIME/signature mismatch", "runtime-fake.pdf", b"not really a pdf\n", "application/pdf", {400})

        list_result = self.post_json(f"/reports/{self.created_report_id}/attachments/list", {"trackingCode": self.created_tracking_code})
        self.record("public-files", f"/reports/{self.created_report_id}/attachments/list", "POST", "anonymous", "200 with authorized attachment metadata", list_result, "passed" if list_result.status == 200 and not self.contains_internal_data(list_result.body) else "failed", "list attachments with valid tracking code")

        if self.created_attachment_id:
            download = self.post_json("/reports/download", {"trackingCode": self.created_tracking_code, "attachmentId": self.created_attachment_id})
            self.record("public-files", "/reports/download", "POST", "anonymous", "2xx file response for valid tracking code", download, "passed" if download.status in {200, 206} else "failed", "download with valid tracking code")
            self.expect_status("public-files", "/reports/download", "POST", "anonymous", {"trackingCode": "GR-invalid-invalid-invalid", "attachmentId": self.created_attachment_id}, {400, 403, 404}, "download with invalid tracking code")
        self.expect_status("public-files", "/reports/download", "POST", "anonymous", {"trackingCode": self.created_tracking_code, "attachmentId": -1}, {400}, "download with invalid attachment id")

    def authentication_probes(self) -> None:
        invalid_username = f"runtime.invalid.{uuid.uuid4().hex[:8]}"
        self.expect_status("auth", "/auth/login", "POST", "anonymous", {"username": invalid_username, "password": "wrong"}, {401, 429}, "invalid login")
        for _ in range(6):
            self.post_json("/auth/login", {"username": invalid_username, "password": "wrong"})
        brute = self.post_json("/auth/login", {"username": invalid_username, "password": "wrong"})
        self.record("auth", "/auth/login", "POST", "anonymous", "controlled 401/429 for repeated invalid login", brute, "passed" if brute.status in {401, 429} else "failed", "brute-force/rate-limit evidence")

        credentials = {
            "admin": ("AdminPassword123!", "admin@ghostreport.local"),
            "analyst": ("AnalystPassword123!", "analyst@ghostreport.local"),
            "auditor": ("AuditorPassword123!", "auditor@ghostreport.local"),
        }
        for username, (password, email) in credentials.items():
            token = self.login_with_mfa(username, password, email)
            if token:
                self.tokens[username] = token

        if self.tokens.get("analyst"):
            self.expect_status(
                "auth",
                "/auth/password/change",
                "POST",
                "ANALYST",
                {"currentPassword": "wrong-current", "newPassword": "NewAnalystPassword123!"},
                {400},
                "password change rejects wrong current password",
                token=self.tokens["analyst"],
            )

        self.expect_status("auth", "/auth/logout", "POST", "anonymous", {}, {401, 403}, "logout without token")
        self.expect_status("auth", "/auth/password/change", "POST", "anonymous", {"currentPassword": "x", "newPassword": "Password123!"}, {401, 403}, "password change without auth")
        self.expect_status("auth", "/auth/password-reset/request", "POST", "anonymous", {"usernameOrEmail": "nobody@example.invalid"}, {202}, "generic password reset request")
        self.expect_status("auth", "/auth/password-reset/confirm", "POST", "anonymous", {"token": "invalid-reset-token", "newPassword": "ResetPassword123!"}, {400}, "invalid password reset confirm")

    def admin_probes(self) -> None:
        admin = self.tokens.get("admin")
        analyst = self.tokens.get("analyst")
        auditor = self.tokens.get("auditor")

        self.expect_get("admin", "/admin/panel", "anonymous", {401}, "admin panel without token")
        self.expect_get("admin", "/admin/users", "invalid-jwt", {401}, "admin users with invalid JWT", token="invalid-ci-runtime-token")
        if analyst:
            self.expect_get("admin", "/admin/users", "ANALYST", {403}, "analyst denied from admin users", token=analyst)
        if auditor:
            self.expect_get("admin", "/admin/users", "AUDITOR", {403}, "auditor denied from admin users", token=auditor)
        if not admin:
            self.skip("admin", "/admin/**", "mixed", "ADMIN", "admin token required", "Admin MFA login did not produce a token.")
            return

        for path in ["/admin/panel", "/admin/users", "/admin/audit-logs", "/admin/security-alerts", "/admin/backups"]:
            self.expect_get("admin", path, "ADMIN", {200}, f"ADMIN accesses {path}", token=admin, no_sensitive=True)

        suffix = uuid.uuid4().hex[:8]
        create_payload = {
            "username": f"runtime.user.{suffix}",
            "email": f"runtime.user.{suffix}@ghostreport.local",
            "password": "RuntimeUser123!",
            "role": "ANALYST",
        }
        created = self.post_json("/admin/users", create_payload, token=admin)
        body = created.body if isinstance(created.body, dict) else {}
        self.created_user_id = self.as_int(body.get("id"))
        self.record("admin", "/admin/users", "POST", "ADMIN", "201 and user DTO without password hash", created, "passed" if created.status == 201 and self.created_user_id and not self.contains_internal_data(created.body) else "failed", "create internal user")

        invalid_role = dict(create_payload)
        invalid_role["username"] = f"runtime.invalid.{suffix}"
        invalid_role["email"] = f"runtime.invalid.{suffix}@ghostreport.local"
        invalid_role["role"] = "SUPERADMIN"
        self.expect_status("admin", "/admin/users", "POST", "ADMIN", invalid_role, {400}, "create user with invalid role", token=admin)

        if self.created_user_id:
            update_payload = {
                "username": f"runtime.updated.{suffix}",
                "email": f"runtime.updated.{suffix}@ghostreport.local",
                "role": "AUDITOR",
                "active": True,
            }
            self.expect_status("admin", f"/admin/users/{self.created_user_id}", "PUT", "ADMIN", update_payload, {200}, "update internal user", token=admin)
            bad_update = dict(update_payload)
            bad_update["role"] = "ROOT"
            self.expect_status("admin", f"/admin/users/{self.created_user_id}", "PUT", "ADMIN", bad_update, {400}, "update user with invalid role", token=admin)
            self.expect_status("admin", f"/admin/users/{self.created_user_id}/deactivate", "PATCH", "ADMIN", None, {200}, "deactivate created user", token=admin)
            self.expect_status("admin", f"/admin/users/{self.created_user_id}/activate", "PATCH", "ADMIN", None, {200}, "activate created user", token=admin)
            self.expect_status("admin", f"/admin/users/{self.created_user_id}", "DELETE", "ADMIN", None, {200}, "logical delete/deactivate created user", token=admin)

        self.expect_status("admin", "/admin/backups", "POST", "ADMIN", None, {200}, "create backup", token=admin)
        backups = self.get_json("/admin/backups", token=admin)
        if isinstance(backups.body, list) and backups.body:
            first = backups.body[0] if isinstance(backups.body[0], dict) else {}
            self.created_backup_filename = first.get("filename") if isinstance(first.get("filename"), str) else None
        if self.created_backup_filename:
            safe_name = urllib.parse.quote(self.created_backup_filename)
            self.expect_get("admin", f"/admin/backups/{safe_name}/download", "ADMIN", {200}, "download created backup", token=admin)
            self.expect_status("admin", f"/admin/backups/{safe_name}/verify", "POST", "ADMIN", None, {200}, "verify created backup", token=admin)
            self.expect_status("admin", "/admin/backups/..%2Fsecret.zip/restore", "POST", "ADMIN", None, {400, 404}, "restore filename traversal validation without destructive restore", token=admin)
        self.expect_status("admin", "/admin/backups/..%2Fsecret.zip/verify", "POST", "ADMIN", None, {400, 404}, "backup filename traversal validation", token=admin)

        users = self.get_json("/admin/users", token=admin)
        admin_id = self.first_active_admin_id(users.body)
        if admin_id:
            self.expect_status("admin", f"/admin/users/{admin_id}/deactivate", "PATCH", "ADMIN", None, {409}, "last active admin cannot be deactivated", token=admin)

    def analyst_probes(self) -> None:
        admin = self.tokens.get("admin")
        analyst = self.tokens.get("analyst")
        auditor = self.tokens.get("auditor")

        self.expect_get("analyst", "/analyst/panel", "anonymous", {401}, "analyst panel without token")
        if auditor:
            self.expect_get("analyst", "/analyst/panel", "AUDITOR", {403}, "auditor denied from analyst panel", token=auditor)
        if admin:
            self.expect_get("analyst", "/analyst/panel", "ADMIN", {200}, "admin analyst oversight route", token=admin)
        if not analyst:
            self.skip("analyst", "/analyst/**", "mixed", "ANALYST", "analyst token required", "Analyst MFA login did not produce a token.")
            return

        for path in ["/analyst/panel", "/analyst/reports", "/analyst/my-cases"]:
            self.expect_get("analyst", path, "ANALYST", {200}, f"ANALYST accesses {path}", token=analyst, no_sensitive=True)

        if not self.created_report_id:
            self.skip("analyst", "/analyst/reports/{id}/assign", "POST", "ANALYST", "report id required", "No report was created earlier in the probe.")
            return

        self.expect_status("analyst", f"/analyst/reports/{self.created_report_id}/assign", "POST", "ANALYST", None, {200}, "assign created report to analyst", token=analyst)
        self.expect_status("analyst", f"/analyst/reports/{self.created_report_id}/status", "PATCH", "ANALYST", {"status": "NOT_A_STATUS"}, {400, 403}, "invalid status rejected or blocked by workflow authorization", token=analyst)
        self.expect_status("analyst", f"/analyst/reports/{self.created_report_id}/priority", "PATCH", "ANALYST", {"priority": "NOT_A_PRIORITY"}, {400}, "invalid priority rejected", token=analyst)
        self.expect_status("analyst", f"/analyst/reports/{self.created_report_id}/notes", "PATCH", "ANALYST", {"notes": "x" * 4100}, {400, 403}, "oversized notes rejected or blocked by workflow authorization", token=analyst)
        self.expect_status("analyst", f"/analyst/reports/{self.created_report_id}/priority", "PATCH", "ANALYST", {"priority": "HIGH"}, {200}, "valid priority update", token=analyst)
        self.expect_status("analyst", f"/analyst/reports/{self.created_report_id}/notes", "PATCH", "ANALYST", {"notes": "Runtime probe analyst note."}, {200, 403}, "valid notes update when allowed by workflow authorization", token=analyst)
        self.expect_get("analyst", f"/analyst/reports/{self.created_report_id}/case-review", "ANALYST", {200}, "case review for assigned case", token=analyst)
        self.expect_get("analyst", f"/analyst/reports/{self.created_report_id}/attachments", "ANALYST", {200}, "list report attachments internally", token=analyst)
        if self.created_attachment_id:
            self.expect_get("analyst", f"/analyst/attachments/{self.created_attachment_id}/download", "ANALYST", {200}, "download report attachment internally", token=analyst)
        self.expect_status("analyst", f"/analyst/reports/{self.created_report_id}/case-package", "POST", "ANALYST", None, {200, 400, 403, 409}, "case package generation is controlled by workflow state", token=analyst)
        self.expect_get("analyst", "/analyst/reports/999999999/case-review", "ANALYST", {404}, "nonexistent case-review controlled error", token=analyst)

    def auditor_probes(self) -> None:
        admin = self.tokens.get("admin")
        analyst = self.tokens.get("analyst")
        auditor = self.tokens.get("auditor")

        self.expect_get("audit", "/audit/logs", "anonymous", {401}, "audit logs without token")
        if analyst:
            self.expect_get("audit", "/audit/logs", "ANALYST", {403}, "analyst denied from audit logs", token=analyst)
        if admin:
            self.expect_get("audit", "/audit/logs", "ADMIN", {200}, "admin accesses audit logs", token=admin, no_sensitive=True)
        if not auditor:
            self.skip("audit", "/audit/**", "GET", "AUDITOR", "auditor token required", "Auditor MFA login did not produce a token.")
            return

        for path in ["/audit/logs", "/audit/security-alerts", "/audit/cases/closed", "/audit/backups"]:
            self.expect_get("audit", path, "AUDITOR", {200}, f"AUDITOR accesses {path}", token=auditor, no_sensitive=True)
        if self.created_report_id:
            self.expect_get("audit", f"/audit/cases/{self.created_report_id}/evidence-package/verify", "AUDITOR", {200, 400, 404, 409}, "evidence package verify for runtime report", token=auditor)
        self.expect_get("audit", "/audit/cases/999999999/evidence-package/verify", "AUDITOR", {400, 404}, "nonexistent report package verify controlled error", token=auditor)
        self.expect_get("audit", "/audit/backups/..%2Fsecret.zip/verify", "AUDITOR", {400, 404}, "backup verify traversal filename", token=auditor)
        self.expect_get("audit", "/audit/backups/..%2Fsecret.zip/manifest", "AUDITOR", {400, 404}, "backup manifest traversal filename", token=auditor)
        if self.created_backup_filename:
            safe_name = urllib.parse.quote(self.created_backup_filename)
            self.expect_get("audit", f"/audit/backups/{safe_name}/verify", "AUDITOR", {200}, "auditor verifies created backup", token=auditor)
            self.expect_get("audit", f"/audit/backups/{safe_name}/manifest", "AUDITOR", {200}, "auditor reads backup manifest", token=auditor)

    def cross_cutting_negative_probes(self) -> None:
        self.expect_get("negative", "/definitely-not-a-real-endpoint", "anonymous", {401, 404}, "unknown endpoint")
        wrong_method = self.request("GET", "/reports/verify")
        self.record("negative", "/reports/verify", "GET", "anonymous", "405/4xx for wrong method", wrong_method, "passed" if wrong_method.status in {400, 401, 403, 405} else "failed", "wrong HTTP method")
        malformed = self.request("POST", "/reports", data=b'{"title":', headers={"Content-Type": "application/json", **self.csrf_header()})
        self.record("negative", "/reports", "POST", "anonymous", "400 for malformed JSON without stack trace", malformed, "passed" if malformed.status == 400 and not self.body_has_stacktrace(malformed.body) else "failed", "malformed JSON")
        wrong_type = self.request("POST", "/reports", data=b"title=bad", headers={"Content-Type": "text/plain", **self.csrf_header()})
        self.record("negative", "/reports", "POST", "anonymous", "415/4xx for wrong content-type", wrong_type, "passed" if wrong_type.status in {400, 415} else "failed", "wrong content type")
        malformed_auth = self.request("GET", "/admin/users", headers={"Authorization": "NotBearer abc"})
        self.record("negative", "/admin/users", "GET", "malformed-auth", "401 for malformed Authorization header", malformed_auth, "passed" if malformed_auth.status == 401 else "failed", "malformed authorization header")
        invalid_jwt = self.request("GET", "/admin/users", token="invalid-ci-runtime-token")
        self.record("negative", "/admin/users", "GET", "invalid-jwt", "401 for invalid JWT", invalid_jwt, "passed" if invalid_jwt.status == 401 else "failed", "invalid JWT")

    def logout_probe(self) -> None:
        token = self.tokens.get("auditor") or self.tokens.get("analyst") or self.tokens.get("admin")
        role = "AUDITOR" if self.tokens.get("auditor") else "ANALYST" if self.tokens.get("analyst") else "ADMIN"
        if not token:
            self.skip("auth", "/auth/logout", "POST", "authenticated", "valid JWT available", "No authenticated token remained available for logout probe.")
            return
        logout = self.post_json("/auth/logout", {}, token=token)
        self.record("auth", "/auth/logout", "POST", role, "204/2xx with valid JWT, or controlled 403 if CSRF/session policy rejects the live probe", logout, "passed" if logout.status in {200, 204, 403} else "failed", "logout with valid JWT")

    def login_with_mfa(self, username: str, password: str, email: str) -> str | None:
        login = self.post_json("/auth/login", {"username": username, "password": password})
        body = login.body if isinstance(login.body, dict) else {}
        no_token_before_mfa = "token" not in body or body.get("token") in {None, ""}
        state = "passed" if login.status == 200 and body.get("mfaRequired") is True and no_token_before_mfa else "failed"
        self.record("auth", "/auth/login", "POST", username.upper(), "200 mfaRequired=true and no final JWT", login, state, f"{username.upper()} password login starts MFA")

        challenge_id = body.get("mfaChallengeId")
        if login.status != 200 or not isinstance(challenge_id, str):
            return None

        code = self.wait_for_mfa_code(email)
        if not code:
            self.skip("auth", "/auth/mfa/verify", "POST", username.upper(), "dev MFA code in runtime log", f"MFA code for {email} not found in app log.")
            return None

        invalid = self.post_json("/auth/mfa/verify", {"challengeId": challenge_id, "code": "000000"})
        self.record("auth", "/auth/mfa/verify", "POST", username.upper(), "401/403 for invalid MFA code", invalid, "passed" if invalid.status in {400, 401, 403} else "failed", f"{username.upper()} invalid MFA code rejected")

        verified = self.post_json("/auth/mfa/verify", {"challengeId": challenge_id, "code": code})
        verified_body = verified.body if isinstance(verified.body, dict) else {}
        token = verified_body.get("token") if isinstance(verified_body.get("token"), str) else None
        self.record("auth", "/auth/mfa/verify", "POST", username.upper(), "200 and JWT after valid MFA", verified, "passed" if verified.status == 200 and token else "failed", f"{username.upper()} valid MFA emits JWT")

        reused = self.post_json("/auth/mfa/verify", {"challengeId": challenge_id, "code": code})
        self.record("auth", "/auth/mfa/verify", "POST", username.upper(), "401/403 when reused", reused, "passed" if reused.status in {400, 401, 403} else "failed", f"{username.upper()} MFA challenge reuse rejected")
        return token

    def wait_for_mfa_code(self, email: str) -> str | None:
        pattern = re.compile(rf"DEV MFA code for {re.escape(email)}:\s*(\d{{6}})")
        for _ in range(30):
            if self.app_log.exists():
                matches = pattern.findall(self.app_log.read_text(encoding="utf-8", errors="replace"))
                if matches:
                    return matches[-1]
            time.sleep(0.5)
        return None

    def expect_get(
        self,
        area: str,
        path: str,
        role: str,
        expected_statuses: set[int],
        purpose: str,
        token: str | None = None,
        no_sensitive: bool = False,
    ) -> HttpResult:
        result = self.get_json(path, token=token)
        ok = result.status in expected_statuses
        if no_sensitive and self.contains_sensitive_evidence(result.body):
            ok = False
        self.record(area, path, "GET", role, self.status_text(expected_statuses), result, "passed" if ok else "failed", purpose)
        return result

    def expect_status(
        self,
        area: str,
        path: str,
        method: str,
        role: str,
        payload: dict[str, object] | None,
        expected_statuses: set[int],
        purpose: str,
        token: str | None = None,
    ) -> HttpResult:
        if method in {"POST", "PUT", "PATCH", "DELETE"}:
            self.refresh_csrf()
            if payload is None:
                result = self.request(method, path, headers=self.csrf_header(), token=token)
            else:
                result = self.request(method, path, data=json.dumps(payload).encode("utf-8"), headers={"Content-Type": "application/json", **self.csrf_header()}, token=token)
        else:
            result = self.request(method, path, token=token)
        ok = result.status in expected_statuses and not self.body_has_stacktrace(result.body)
        self.record(area, path, method, role, self.status_text(expected_statuses), result, "passed" if ok else "failed", purpose)
        return result

    def multipart_expect(self, area: str, purpose: str, filename: str, content: bytes, content_type: str, expected_statuses: set[int]) -> None:
        if not self.created_report_id or not self.created_tracking_code:
            self.skip(area, "/reports/{id}/attachments", "POST", "anonymous", "report/tracking required", purpose)
            return
        result = self.post_multipart(
            f"/reports/{self.created_report_id}/attachments",
            {"trackingCode": self.created_tracking_code},
            [("files", filename, content, content_type)],
        )
        self.record(area, f"/reports/{self.created_report_id}/attachments", "POST", "anonymous", self.status_text(expected_statuses), result, "passed" if result.status in expected_statuses else "failed", purpose)

    def post_json(self, path: str, payload: dict[str, object], token: str | None = None) -> HttpResult:
        self.refresh_csrf()
        return self.request(
            "POST",
            path,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json", **self.csrf_header()},
            token=token,
        )

    def get_json(self, path: str, token: str | None = None) -> HttpResult:
        return self.request("GET", path, token=token)

    def post_multipart(
        self,
        path: str,
        fields: dict[str, str],
        files: list[tuple[str, str, bytes, str]],
        token: str | None = None,
    ) -> HttpResult:
        self.refresh_csrf()
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
        return self.request("POST", path, data=b"".join(parts), headers=headers, token=token)

    def request(
        self,
        method: str,
        path: str,
        data: bytes | None = None,
        headers: dict[str, str] | None = None,
        token: str | None = None,
    ) -> HttpResult:
        url = f"{self.base_url}{path}"
        request_headers = headers.copy() if headers else {}
        if token:
            request_headers["Authorization"] = f"Bearer {token}"
        req = urllib.request.Request(url, data=data, headers=request_headers, method=method)
        try:
            with self.opener.open(req, timeout=20) as response:
                return HttpResult(response.status, self.parse_response(response.read()), dict(response.headers.items()))
        except urllib.error.HTTPError as exc:
            return HttpResult(exc.code, self.parse_response(exc.read()), dict(exc.headers.items()))
        except Exception as exc:
            return HttpResult(0, {"error": type(exc).__name__, "message": str(exc)}, {})

    def csrf_header(self) -> dict[str, str]:
        for cookie in reversed(list(self.cookie_jar)):
            if cookie.name == "XSRF-TOKEN":
                return {"X-XSRF-TOKEN": urllib.parse.unquote(cookie.value)}
        return {}

    def refresh_csrf(self) -> None:
        self.request("GET", "/index.html")

    def parse_response(self, raw: bytes) -> object:
        if not raw:
            return None
        text = raw.decode("utf-8", errors="replace")
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return text[:2000]

    def record(self, area: str, endpoint: str, method: str, role: str, expected: str, result: HttpResult, state: str, notes: str) -> None:
        obtained = str(result.status)
        if self.body_has_stacktrace(result.body):
            state = "failed"
            notes = f"{notes}; response appears to expose stack/framework details"
        self.rows.append(ProbeRow(area, endpoint, method, role, expected, obtained, state, notes))

    def write_artifacts(self) -> None:
        runtime_md = self.output_dir / "runtime-endpoints.md"
        with runtime_md.open("w", encoding="utf-8") as handle:
            handle.write("| Area | Endpoint/Probe | Method | Role used | Expected result | Obtained result | Status | Notes |\n")
            handle.write("| --- | --- | --- | --- | --- | --- | --- | --- |\n")
            for row in self.rows:
                handle.write(
                    f"| {self.md(row.area)} | {self.md(row.endpoint)} | {self.md(row.method)} | {self.md(row.role)} | "
                    f"{self.md(row.expected)} | {self.md(row.obtained)} | {self.md(row.state)} | {self.md(row.notes)} |\n"
                )

        summary = self.summary()
        self.write_json_artifact("runtime-probe-summary.json", summary)

    def summary(self) -> dict[str, object]:
        counts = {
            "total_probes": len(self.rows),
            "passed": sum(1 for row in self.rows if row.state == "passed"),
            "failed": sum(1 for row in self.rows if row.state == "failed"),
            "skipped": sum(1 for row in self.rows if row.state == "skipped"),
            "public_endpoints_tested": self.count_area("public") + self.count_area("public-report") + self.count_area("public-files") + self.count_area("tracking"),
            "admin_endpoints_tested": self.count_area("admin"),
            "analyst_endpoints_tested": self.count_area("analyst"),
            "auditor_endpoints_tested": self.count_area("audit"),
            "negative_cases_tested": self.count_area("negative"),
        }
        return {
            **counts,
            "roles_tested": sorted({row.role for row in self.rows if row.role not in {"anonymous", "invalid-jwt", "malformed-auth"}}),
            "created_runtime_data": {
                "report_id_present": self.created_report_id is not None,
                "tracking_code_present": self.created_tracking_code is not None,
                "attachment_id_present": self.created_attachment_id is not None,
                "created_user_id_present": self.created_user_id is not None,
                "backup_filename_present": self.created_backup_filename is not None,
            },
            "probes": [asdict(row) for row in self.rows],
        }

    def write_json_artifact(self, filename: str, data: object) -> None:
        redacted = self.redact(data)
        (self.output_dir / filename).write_text(json.dumps(redacted, indent=2, sort_keys=True), encoding="utf-8")

    def redact(self, data: object) -> object:
        if isinstance(data, dict):
            redacted = {}
            for key, value in data.items():
                if key.lower() in SENSITIVE_KEYS:
                    redacted[key] = "[REDACTED]"
                else:
                    redacted[key] = self.redact(value)
            return redacted
        if isinstance(data, list):
            return [self.redact(item) for item in data]
        if isinstance(data, str):
            value = re.sub(r"Bearer\s+[A-Za-z0-9._-]+", "Bearer [REDACTED]", data)
            value = re.sub(r"GR-[A-Za-z0-9_-]{20,64}", "GR-[REDACTED]", value)
            value = re.sub(r"\b\d{6}\b", "[REDACTED-6-DIGIT]", value)
            return value
        return data

    def contains_internal_data(self, data: object) -> bool:
        text = json.dumps(data, default=str).lower()
        return any(marker in text for marker in ["passwordhash", "storagepath", "stacktrace", "hibernate", "jwt_secret", "backup_hmac_secret"])

    def contains_sensitive_evidence(self, data: object) -> bool:
        text = json.dumps(data, default=str).lower()
        return any(marker in text for marker in ["authorization:", "bearer ", "jwt_secret", "backup_hmac_secret", "passwordhash"])

    def contains_real_tracking_code(self, data: object) -> bool:
        text = json.dumps(data, default=str)
        return re.search(r"GR-[A-Za-z0-9_-]{20,64}", text) is not None

    def body_has_stacktrace(self, data: object) -> bool:
        text = json.dumps(data, default=str).lower()
        return any(marker in text for marker in ["stacktrace", "java.", "hibernate", "org.springframework"])

    def has_security_headers(self, headers: dict[str, str]) -> bool:
        lowered = {key.lower(): value for key, value in headers.items()}
        return all(
            header in lowered
            for header in [
                "x-content-type-options",
                "x-frame-options",
                "content-security-policy",
                "referrer-policy",
                "permissions-policy",
            ]
        )

    def first_active_admin_id(self, users: object) -> int | None:
        if not isinstance(users, list):
            return None
        active_admins = [user for user in users if isinstance(user, dict) and user.get("role") == "ADMIN" and user.get("active") is True]
        if len(active_admins) == 1:
            return self.as_int(active_admins[0].get("id"))
        return None

    def count_area(self, prefix: str) -> int:
        return sum(1 for row in self.rows if row.area == prefix and row.state != "skipped")

    def body_text(self, body: object) -> str:
        if isinstance(body, str):
            return body
        return json.dumps(body, default=str)

    def as_int(self, value: object) -> int | None:
        try:
            return int(value) if value is not None else None
        except (TypeError, ValueError):
            return None

    def status_text(self, statuses: set[int]) -> str:
        return "/".join(str(status) for status in sorted(statuses))

    def md(self, value: object) -> str:
        return str(value).replace("|", "\\|").replace("\n", " ")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8081")
    parser.add_argument("--output-dir", default="target/iast-evidence")
    parser.add_argument("--app-log", default="target/ghostreport-dast-app.log")
    args = parser.parse_args()

    try:
        RuntimeProbe(args.base_url, Path(args.output_dir), Path(args.app_log)).run()
    except SystemExit:
        raise
    except Exception as exc:
        print(f"Runtime security probe crashed: {type(exc).__name__}: {exc}", file=sys.stderr)
        raise


if __name__ == "__main__":
    main()
