package com.atwms.order.domain;

import com.atwms.common.persistence.DatabaseAccess;
import com.atwms.common.persistence.Restriction;
import com.atwms.common.tenant.TenantAwareEntity_;
import com.atwms.common.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Saemtlicher Datenbankzugriff des Order-Service.
 *
 * <p>Die Mandanteneinschraenkung ist als eigene {@link Restriction} formuliert
 * und wird von jeder Abfrage wiederverwendet. Damit kann sie nicht mehr
 * vergessen werden, und man sieht an einer Stelle, worauf die Mandantentrennung
 * in diesem Service beruht.</p>
 */
@ApplicationScoped
public class OrderDatabase extends DatabaseAccess {

    @PersistenceContext(unitName = "orderPU")
    EntityManager em;

    @Inject
    TenantContext tenantContext;

    @Override
    protected EntityManager em() {
        return em;
    }

    /** Einschraenkung auf den Mandanten des aktuellen Requests. */
    private Restriction<OrderEntity> ownTenant() {
        String tenantId = tenantContext.requireTenantId();
        return (cb, order) -> cb.equal(order.get(TenantAwareEntity_.tenantId), tenantId);
    }

    public List<OrderEntity> findAllForCurrentTenant() {
        return list(OrderEntity.class, ownTenant(),
                (cb, order) -> List.of(cb.desc(order.get(OrderEntity_.createdAt))));
    }

    /**
     * Laedt eine Bestellung und prueft dabei die Mandantenzugehoerigkeit.
     * Ein Treffer aus einem fremden Mandanten wird wie "nicht gefunden"
     * behandelt - so laesst sich ueber die Antwort nicht einmal ableiten,
     * ob die ID ueberhaupt existiert.
     */
    public Optional<OrderEntity> findById(String id) {
        return byKey(OrderEntity.class, id)
                .filter(order -> order.getTenantId().equals(tenantContext.requireTenantId()));
    }

    public long countSince(Instant since) {
        return count(OrderEntity.class, ownTenant().and(
                (cb, order) -> cb.greaterThanOrEqualTo(order.get(OrderEntity_.createdAt), since)));
    }

    @Transactional
    public OrderEntity save(OrderEntity order) {
        persist(order);
        em.flush();
        return order;
    }
}
