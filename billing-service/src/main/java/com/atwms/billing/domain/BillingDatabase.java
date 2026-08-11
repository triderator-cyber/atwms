package com.atwms.billing.domain;

import com.atwms.common.persistence.DatabaseAccess;
import com.atwms.common.persistence.Restriction;
import com.atwms.common.tenant.TenantAwareEntity_;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

/**
 * Saemtlicher Datenbankzugriff des Billing-Service.
 */
@ApplicationScoped
public class BillingDatabase extends DatabaseAccess {

    @PersistenceContext(unitName = "billingPU")
    EntityManager em;

    @Override
    protected EntityManager em() {
        return em;
    }

    /**
     * Speichert eine Verbrauchsposition, sofern das Event nicht bereits
     * verarbeitet wurde.
     *
     * @return true, wenn ein neuer Datensatz entstanden ist
     */
    @Transactional
    public boolean recordIfNew(UsageRecord record) {
        boolean known = exists(UsageRecord.class,
                (cb, usage) -> cb.equal(usage.get(UsageRecord_.sourceEventId), record.getSourceEventId()));
        if (known) {
            return false;
        }
        persist(record);
        return true;
    }

    public BigDecimal sumForPeriod(String tenantId, String period) {
        // COALESCE, damit bei einem Zeitraum ohne Buchungen 0 statt null herauskommt.
        return aggregate(UsageRecord.class, BigDecimal.class,
                (cb, usage) -> cb.coalesce(cb.sum(usage.get(UsageRecord_.amount)), BigDecimal.ZERO),
                periodOfTenant(tenantId, period));
    }

    public long countForPeriod(String tenantId, String period) {
        return count(UsageRecord.class, periodOfTenant(tenantId, period));
    }

    /**
     * Gemeinsame Einschraenkung beider Auswertungen. Genau dafuer lohnt sich die
     * Criteria API: eine Teilbedingung laesst sich als Methode ausdruecken und
     * wiederverwenden, statt sie in mehreren Abfragetexten zu wiederholen.
     */
    private Restriction<UsageRecord> periodOfTenant(String tenantId, String period) {
        return (cb, usage) -> cb.and(
                cb.equal(usage.get(TenantAwareEntity_.tenantId), tenantId),
                cb.equal(usage.get(UsageRecord_.billingPeriod), period));
    }
}
