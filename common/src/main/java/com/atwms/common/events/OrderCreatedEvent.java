package com.atwms.common.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Integrationsevent: eine Bestellung wurde angelegt.
 *
 * <p>Bewusst ein eigener, flacher DTO-Typ und nicht die JPA-Entity des
 * Order-Service. Das Event ist der oeffentliche Vertrag zwischen den Services;
 * die interne Datenstruktur des Producers darf sich unabhaengig davon
 * weiterentwickeln.</p>
 *
 * <p>Das Feld {@code tenantId} reist bewusst im Event mit: der konsumierende
 * Service hat keinen HTTP-Request und damit kein JWT, aus dem er den Mandanten
 * ableiten koennte.</p>
 *
 * <p>{@code schemaVersion} erlaubt spaetere Formataenderungen ohne Big-Bang-
 * Deployment: Consumer koennen alte und neue Versionen parallel verarbeiten.</p>
 */
public class OrderCreatedEvent {

    private int schemaVersion = 1;
    private String eventId;
    private String tenantId;
    private String orderId;
    private String customerReference;
    private BigDecimal amount;
    private String currency;
    private Instant occurredAt;

    public OrderCreatedEvent() {
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String toString() {
        return "OrderCreatedEvent[orderId=" + orderId + ", tenantId=" + tenantId
                + ", amount=" + amount + " " + currency + "]";
    }
}
