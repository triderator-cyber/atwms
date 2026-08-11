package com.atwms.common.tenant;

import jakarta.enterprise.context.RequestScoped;

import java.io.Serializable;
import java.util.Optional;

/**
 * Request-bezogener Mandantenkontext.
 *
 * <p>Wird pro HTTP-Request genau einmal durch den {@link TenantResolutionFilter}
 * aus dem JWT befuellt und steht danach ueber CDI-Injection in jeder Schicht
 * (Resource, Service, Datenbankzugriff, Entity-Listener) zur Verfuegung.</p>
 *
 * <p>Bewusst {@code @RequestScoped}: damit ist ausgeschlossen, dass ein
 * Mandantenkontext versehentlich ueber Request-Grenzen hinweg "haengen bleibt" -
 * der klassische Fehler bei ThreadLocal-basierten Loesungen in Thread-Pools.</p>
 */
@RequestScoped
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String tier;
    private String subject;

    /**
     * @return die Mandantenkennung des aktuellen Requests
     * @throws UnresolvedTenantException wenn der Request keinen Mandanten traegt
     */
    public String requireTenantId() {
        if (tenantId == null || tenantId.isBlank()) {
            throw new UnresolvedTenantException(
                    "Kein tenant_id-Claim im Request vorhanden - Zugriff nicht moeglich.");
        }
        return tenantId;
    }

    public Optional<String> getTenantId() {
        return Optional.ofNullable(tenantId);
    }

    public Optional<String> getTier() {
        return Optional.ofNullable(tier);
    }

    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    void initialise(String tenantId, String tier, String subject) {
        this.tenantId = tenantId;
        this.tier = tier;
        this.subject = subject;
    }

    @Override
    public String toString() {
        return "TenantContext[tenantId=" + tenantId + ", tier=" + tier + "]";
    }
}
