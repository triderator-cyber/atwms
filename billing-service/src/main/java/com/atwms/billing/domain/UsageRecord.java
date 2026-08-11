package com.atwms.billing.domain;

import com.atwms.common.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Verbrauchsposition eines Mandanten, aus der spaeter die Rechnung entsteht.
 *
 * <p>Die Unique Constraint auf {@code source_event_id} macht die Verarbeitung
 * idempotent: Kafka garantiert "at least once", eine Nachricht kann also
 * mehrfach ankommen (etwa nach einem Rebalancing oder einem Neustart des
 * Consumers). Ohne diese Absicherung wuerde der Mandant dieselbe Bestellung
 * mehrfach in Rechnung gestellt bekommen - einer der haeufigsten und
 * teuersten Fehler in ereignisgetriebenen Architekturen.</p>
 */
@Entity
@Table(name = "usage_record",
       uniqueConstraints = @UniqueConstraint(name = "uq_usage_source_event",
                                             columnNames = "source_event_id"),
       indexes = @Index(name = "idx_usage_tenant_period", columnList = "tenant_id, billing_period"))
@NamedQuery(name = UsageRecord.SUM_FOR_PERIOD,
            query = "SELECT COALESCE(SUM(u.amount), 0) FROM UsageRecord u "
                    + "WHERE u.tenantId = :tenantId AND u.billingPeriod = :period")
@NamedQuery(name = UsageRecord.COUNT_FOR_PERIOD,
            query = "SELECT COUNT(u) FROM UsageRecord u "
                    + "WHERE u.tenantId = :tenantId AND u.billingPeriod = :period")
public class UsageRecord extends TenantAwareEntity {

    public static final String SUM_FOR_PERIOD = "UsageRecord.sumForPeriod";
    public static final String COUNT_FOR_PERIOD = "UsageRecord.countForPeriod";

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    /** ID des ausloesenden Events - Grundlage der Idempotenz. */
    @Column(name = "source_event_id", nullable = false, length = 64)
    private String sourceEventId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Abrechnungszeitraum im Format yyyy-MM. */
    @Column(name = "billing_period", nullable = false, length = 7)
    private String billingPeriod;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt = Instant.now();

    public UsageRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
