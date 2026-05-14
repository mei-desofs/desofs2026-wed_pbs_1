# GhostReport Secure Logging

## Logging policy

Application logs and persisted security alerts must avoid user-submitted secrets and filesystem details.

Do not log:

- tracking codes or tracking code hashes;
- report descriptions, titles, or free-text complaint content;
- original attachment filenames;
- absolute filesystem paths or stored path values;
- raw path traversal input or backup filenames;
- exception messages in HTTP error responses when they may contain internal details.

Allowed operational context:

- internal numeric IDs such as report IDs, attachment IDs, user IDs, and target IDs;
- role names and event/action names;
- generic event outcomes such as upload requested, case package generated, or backup integrity failed.

`spring.jpa.show-sql` is disabled in the default configuration to avoid SQL and bound data appearing in logs.
