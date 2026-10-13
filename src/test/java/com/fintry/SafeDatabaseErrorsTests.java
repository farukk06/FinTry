package com.fintry;

import com.fintry.exception.GlobalExceptionHandler;
import com.fintry.security.SafeDatabaseErrors;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class SafeDatabaseErrorsTests {
    @Test void handlerLogsSafeMetadataAndReturnsMatchingCorrelationId(CapturedOutput output) {
        var failure = new DataIntegrityViolationException("SQL secret-parameter password_hash token-canary",
                new SQLException("Failing row contains private-email financial-canary", "23514"));
        var response = new GlobalExceptionHandler().handleIntegrityFailure(failure);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        String id = response.getHeaders().getFirst("X-Correlation-ID");
        assertThat(id).matches("[0-9a-f-]{36}");
        assertThat(output.getAll()).contains("FINANCIAL_INTEGRITY_FAILURE", "sqlState=23514", id)
                .doesNotContain("secret-parameter", "password_hash", "token-canary", "private-email", "financial-canary",
                        "Failing row", "SQLException", "DataIntegrityViolationException");
    }
    @Test void malformedSqlStateAndExceptionMessageAreNeverLogged(CapturedOutput output) {
        String id = SafeDatabaseErrors.record(new SQLException("sensitive-canary", "23514\nsecret-canary"), "REGISTRATION_CONFLICT");
        assertThat(output.getAll()).contains("sqlState=UNKNOWN", id).doesNotContain("sensitive-canary", "secret-canary");
    }
    @Test void lockFailureHasSafeCorrelationWithoutExceptionTrace(CapturedOutput output) {
        var response = new GlobalExceptionHandler().handleLockFailure(new SQLException("lock-sensitive-canary", "55P03"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(output.getAll()).contains("FINANCIAL_LOCK_CONFLICT", "sqlState=55P03",
                response.getHeaders().getFirst("X-Correlation-ID")).doesNotContain("lock-sensitive-canary", "SQLException");
    }
    @Test void knownFinancialDuplicateReturns409WithoutLeakingConstraintOrParameters(CapturedOutput output) {
        var violation = new org.hibernate.exception.ConstraintViolationException("private-statement-canary",
                new SQLException("private-row-canary", "23505"), "uk_virtual_account_user");
        var response = new GlobalExceptionHandler().handleIntegrityFailure(new DataIntegrityViolationException("private-wrapper-canary", violation));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(output.getAll()).contains("sqlState=23505", response.getHeaders().getFirst("X-Correlation-ID"))
                .doesNotContain("private-statement-canary", "private-row-canary", "private-wrapper-canary", "uk_virtual_account_user");
    }
}
