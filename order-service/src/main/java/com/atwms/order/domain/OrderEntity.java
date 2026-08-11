package com.atwms.order.domain;

import com.atwms.common.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Bestellung eines Mandanten.
 *
 * <p>Erbt {@code tenant_id} von {@link TenantAwareEntity}. Alle Queries sind
 * als NamedQuery definiert und fuehren den Mandanten als Pflichtparameter -
 * so kann keine Abfrage "aus Versehen" ohne Mandantenfilter entstehen.</p>
 */
@Entity
@Table(name = "orders",
       indexes = @Index(name = "idx_orders_tenant", columnList = "tenant_id"))
@NamedQuery(name = OrderEntity.FIND_BY_TENANT,
            query = "SELECT o FROM OrderEntity o WHERE o.tenantId = :tenantId ORDER BY o.createdAt DESC")
@NamedQuery(name = OrderEntity.COUNT_BY_TENANT_SINCE,
            query = "SELECT COUNT(o) FROM OrderEntity o WHERE o.tenantId = :tenantId AND o.createdAt >= :since")
public class OrderEntity extends TenantAwareEntity {

    public static final String FIND_BY_TENANT = "Order.findByTenant";
    public static final String COUNT_BY_TENANT_SINCE = "Order.countByTenantSince";

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "customer_reference", nullable = false, length = 128)
    private String customerReference;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status = OrderStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OrderEntity() {
    }

    public OrderEntity(String customerReference, BigDecimal amount, String currency) {
        this.customerReference = customerReference;
        this.amount = amount;
        this.currency = currency;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public void setCustomerReference(String customerReference) {
        this.customerReference = customerReference;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
