package com.atwms.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/**
 * Ein Mandant der Plattform.
 *
 * <p>Diese Entity gehoert zur <em>Control Plane</em> und ist deshalb bewusst
 * <strong>nicht</strong> von {@code TenantAwareEntity} abgeleitet: sie
 * beschreibt die Mandanten, statt selbst einem Mandanten zu gehoeren. Zugriff
 * darauf haben nur Plattform-Administratoren sowie - lesend und auf den
 * eigenen Datensatz beschraenkt - die uebrigen Services.</p>
 */
@Entity
@Table(name = "tenant")
@NamedQuery(name = Tenant.FIND_ALL, query = "SELECT t FROM Tenant t ORDER BY t.createdAt DESC")
public class Tenant {

    public static final String FIND_ALL = "Tenant.findAll";

    /**
     * Fachlicher Schluessel, identisch mit dem tenant_id-Claim im JWT.
     * Bewusst ein sprechender String ("acme") statt einer generierten UUID -
     * das erleichtert Support, Logs und Schema-Namen im Silo-Modell erheblich.
     */
    @Id
    @Column(name = "id", length = 64)
    @NotBlank
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,62}",
             message = "Mandantenkennung darf nur Kleinbuchstaben, Ziffern und Bindestriche enthalten.")
    private String id;

    @Column(name = "display_name", nullable = false, length = 200)
    @NotBlank
    private String displayName;

    @Column(name = "tier", nullable = false, length = 32)
    private String tier = "free";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantStatus status = TenantStatus.PROVISIONING;

    @Enumerated(EnumType.STRING)
    @Column(name = "isolation_model", nullable = false, length = 32)
    private IsolationModel isolationModel = IsolationModel.POOLED;

    /** Obergrenze fuer Bestellungen pro Monat; wird vom Order-Service abgefragt. */
    @Column(name = "monthly_order_limit", nullable = false)
    private int monthlyOrderLimit = 100;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Tenant() {
    }

    public Tenant(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public boolean isWritable() {
        return status == TenantStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public IsolationModel getIsolationModel() {
        return isolationModel;
    }

    public void setIsolationModel(IsolationModel isolationModel) {
        this.isolationModel = isolationModel;
    }

    public int getMonthlyOrderLimit() {
        return monthlyOrderLimit;
    }

    public void setMonthlyOrderLimit(int monthlyOrderLimit) {
        this.monthlyOrderLimit = monthlyOrderLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
