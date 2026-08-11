package com.atwms.billing.domain;

import com.atwms.common.tenant.TenantAwareEntity_;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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
        if (existsBySourceEvent(record.getSourceEventId())) {
            return false;
        }
        em.persist(record);
        return true;
    }

    private boolean existsBySourceEvent(String sourceEventId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<UsageRecord> usage = query.from(UsageRecord.class);

        query.select(cb.count(usage))
                .where(cb.equal(usage.get(UsageRecord_.sourceEventId), sourceEventId));

        return em.createQuery(query).getSingleResult() > 0;
    }

    public BigDecimal sumForPeriod(String tenantId, String period) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<UsageRecord> usage = query.from(UsageRecord.class);

        // COALESCE, damit bei einem Zeitraum ohne Buchungen 0 statt null herauskommt.
        query.select(cb.coalesce(cb.sum(usage.get(UsageRecord_.amount)), BigDecimal.ZERO))
                .where(periodOfTenant(cb, usage, tenantId, period));

        return em.createQuery(query).getSingleResult();
    }

    public long countForPeriod(String tenantId, String period) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<UsageRecord> usage = query.from(UsageRecord.class);

        query.select(cb.count(usage))
                .where(periodOfTenant(cb, usage, tenantId, period));

        return em.createQuery(query).getSingleResult();
    }

    /**
     * Gemeinsame Einschraenkung beider Abfragen. Genau hierfuer lohnt sich die
     * Criteria API: Teilbedingungen lassen sich als Methode ausdruecken und
     * wiederverwenden, statt sie in mehreren Abfragetexten zu wiederholen.
     */
    private jakarta.persistence.criteria.Predicate periodOfTenant(
            CriteriaBuilder cb, Root<UsageRecord> usage, String tenantId, String period) {
        return cb.and(
                cb.equal(usage.get(TenantAwareEntity_.tenantId), tenantId),
                cb.equal(usage.get(UsageRecord_.billingPeriod), period));
    }
}
