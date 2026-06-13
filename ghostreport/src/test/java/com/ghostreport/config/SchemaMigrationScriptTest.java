package com.ghostreport.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationScriptTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/schema/postgresql/001_audit_alert_metadata.sql"
    );

    @Test
    void auditAndAlertMetadataMigrationBackfillsBeforeNotNullConstraints() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("ADD COLUMN IF NOT EXISTS correlation_id varchar(80)");
        assertThat(sql).contains("ADD COLUMN IF NOT EXISTS integrity_hash varchar(64)");
        assertThat(sql).contains("SET correlation_id = 'legacy-' || id");
        assertThat(sql).contains("md5(concat_ws('|'");
        assertThat(sql).contains("ALTER COLUMN correlation_id SET NOT NULL");
        assertThat(sql).contains("ALTER COLUMN integrity_hash SET NOT NULL");

        assertThat(sql.indexOf("ADD COLUMN IF NOT EXISTS correlation_id varchar(80)"))
                .isLessThan(sql.indexOf("SET correlation_id = 'legacy-' || id"));
        assertThat(sql.indexOf("SET correlation_id = 'legacy-' || id"))
                .isLessThan(sql.indexOf("ALTER COLUMN correlation_id SET NOT NULL"));
        assertThat(sql.indexOf("ADD COLUMN IF NOT EXISTS integrity_hash varchar(64)"))
                .isLessThan(sql.indexOf("ALTER COLUMN integrity_hash SET NOT NULL"));
    }
}
