package com.atwms.tenant.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness-Probe: der Service meldet sich erst dann bereit, wenn seine
 * Datenbank tatsaechlich erreichbar ist. Kubernetes nimmt den Pod sonst zu
 * frueh in den Lastverteiler auf und die ersten Requests laufen ins Leere.
 */
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @PersistenceContext(unitName = "tenantPU")
    EntityManager em;

    @Override
    public HealthCheckResponse call() {
        try {
            em.createNativeQuery("SELECT 1").getSingleResult();
            return HealthCheckResponse.up("tenant-database");
        } catch (RuntimeException e) {
            return HealthCheckResponse.builder()
                    .name("tenant-database")
                    .down()
                    .withData("error", String.valueOf(e.getMessage()))
                    .build();
        }
    }
}
