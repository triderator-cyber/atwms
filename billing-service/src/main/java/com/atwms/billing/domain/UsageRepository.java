package com.atwms.billing.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class UsageRepository {

    @PersistenceContext(unitName = "billingPU")
    EntityManager em;

    /**
     * Speichert eine Verbrauchsposition, sofern das Event nicht bereits
     * verarbeitet wurde.
     *
     * @return true, wenn ein neuer Datensatz entstanden ist
     */
    @Transactional
    public boolean recordIfNew(UsageRecord record) {
        Long existing = em.createQuery(
                        "SELECT COUNT(u) FROM UsageRecord u WHERE u.sourceEventId = :eventId", Long.class)
                .setParameter("eventId", record.getSourceEventId())
                .getSingleResult();
        if (existing > 0) {
            return false;
        }
        em.persist(record);
        return true;
    }

    public BigDecimal sumForPeriod(String tenantId, String period) {
        return em.createNamedQuery(UsageRecord.SUM_FOR_PERIOD, BigDecimal.class)
                .setParameter("tenantId", tenantId)
                .setParameter("period", period)
                .getSingleResult();
    }

    public long countForPeriod(String tenantId, String period) {
        return em.createNamedQuery(UsageRecord.COUNT_FOR_PERIOD, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("period", period)
                .getSingleResult();
    }
}
