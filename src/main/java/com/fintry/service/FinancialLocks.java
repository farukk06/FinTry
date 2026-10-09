package com.fintry.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Call only inside a transaction, before its first lock. No session-wide setting is changed. */
@Component
public class FinancialLocks {
    @PersistenceContext private EntityManager entityManager;

    public void configureTimeout() {
        entityManager.createNativeQuery("select set_config('lock_timeout', '5s', true)")
                .getSingleResult();
    }
}
