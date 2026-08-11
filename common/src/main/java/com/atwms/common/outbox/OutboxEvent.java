package com.atwms.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Ein noch zu veroeffentlichendes Integrationsevent.
 *
 * <p>Diese Entity ist bewusst <strong>nicht</strong> von
 * {@code TenantAwareEntity} abgeleitet. Der Poller laeuft ohne HTTP-Request und
 * damit ohne Mandantenkontext; die Mandantenkennung reist deshalb als normale
 * Spalte mit und wandert von dort in das Event.</p>
 *
 * <p>Der Index auf {@code status, created_at} ist kein Beiwerk: der Poller
 * fragt genau danach, und ohne ihn wuerde jede Abfrage die gesamte - mit der
 * Zeit sehr grosse - Tabelle lesen.</p>
 */
@Entity
@Table(name = "outbox_event",
       indexes = @Index(name = "idx_outbox_status_created", columnList = "status, created_at"))
public class OutboxEvent {

    /** Wandert als eventId in die Nachricht und ist der Schluessel fuer die Idempotenz beim Konsumenten. */
    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    /** Fachlicher Typ des ausloesenden Objekts, z. B. "Order". */
    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    /** Schluessel des ausloesenden Objekts - spaeter der Kafka-Key fuer die Reihenfolge. */
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "payload", nullable = false, length = 16384)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status = OutboxStatus.NEW;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    public OutboxEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return "OutboxEvent[" + eventType + " " + aggregateType + "/" + aggregateId
                + ", tenant=" + tenantId + ", status=" + status + "]";
    }
}
