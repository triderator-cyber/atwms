package com.atwms.common.tenant;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * Basisklasse fuer alle mandantenbezogenen Entities (Pooled-Modell:
 * gemeinsame Tabelle, Trennung ueber die Spalte {@code tenant_id}).
 *
 * <p>Die Mandantenkennung wird beim Persistieren automatisch aus dem
 * {@link TenantContext} gesetzt, damit sie nicht in jeder Service-Methode
 * haendisch gefuellt (und dabei irgendwann vergessen) wird.</p>
 *
 * <p><strong>Wichtig:</strong> Das automatische Setzen beim Schreiben ersetzt
 * nicht das Filtern beim Lesen. Jede Query muss zusaetzlich auf
 * {@code tenantId} einschraenken - dafuer gibt es in jedem Service benannte
 * Queries, die den Mandanten als Pflichtparameter fuehren. Als dritte
 * Sicherheitsebene aktiviert das DB-Init-Skript Row Level Security in
 * PostgreSQL.</p>
 */
@MappedSuperclass
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    @PrePersist
    void applyTenant() {
        if (tenantId == null) {
            tenantId = CDI.current().select(TenantContext.class).get().requireTenantId();
        }
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
