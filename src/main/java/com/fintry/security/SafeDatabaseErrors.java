package com.fintry.security;

import java.sql.SQLException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Log only caller-owned codes, a validated SQLState, and an internally generated correlation ID. */
public final class SafeDatabaseErrors {
    private static final Logger LOG = LoggerFactory.getLogger(SafeDatabaseErrors.class);
    private SafeDatabaseErrors() { }

    public static String record(Throwable failure, String code) {
        String correlationId = UUID.randomUUID().toString();
        String state = "UNKNOWN";
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < 20; depth++, cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null
                    && sql.getSQLState().matches("[0-9A-Z]{5}")) {
                state = sql.getSQLState();
                break;
            }
        }
        LOG.warn("database_failure code={} sqlState={} correlationId={}", code, state, correlationId);
        return correlationId;
    }
}
