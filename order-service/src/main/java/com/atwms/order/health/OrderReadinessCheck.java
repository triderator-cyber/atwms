package com.atwms.order.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class OrderReadinessCheck implements HealthCheck {

    @PersistenceContext(unitName = "orderPU")
    EntityManager em;

    @Override
    public HealthCheckResponse call() {
        try {
            em.createNativeQuery("SELECT 1").getSingleResult();
            return HealthCheckResponse.up("order-database");
        } catch (RuntimeException e) {
            return HealthCheckResponse.builder()
                    .name("order-database")
                    .down()
                    .withData("error", String.valueOf(e.getMessage()))
                    .build();
        }
    }
}
