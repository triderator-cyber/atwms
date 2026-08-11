package com.atwms.billing.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class BillingReadinessCheck implements HealthCheck {

    @PersistenceContext(unitName = "billingPU")
    EntityManager em;

    @Override
    public HealthCheckResponse call() {
        try {
            em.createNativeQuery("SELECT 1").getSingleResult();
            return HealthCheckResponse.up("billing-database");
        } catch (RuntimeException e) {
            return HealthCheckResponse.builder()
                    .name("billing-database")
                    .down()
                    .withData("error", String.valueOf(e.getMessage()))
                    .build();
        }
    }
}
