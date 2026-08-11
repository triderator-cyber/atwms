package com.atwms.order.domain;

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
 * Datenzugriff fuer Bestellungen.
 *
 * <p>Das Repository ist die einzige Stelle, an der Queries entstehen, und
 * setzt den Mandantenfilter selbst - die aufrufende Fachlogik kann ihn damit
 * nicht vergessen.</p>
 */
@ApplicationScoped
public class OrderRepository {

    @PersistenceContext(unitName = "orderPU")
    EntityManager em;

    @Inject
    TenantContext tenantContext;

    public List<OrderEntity> findAllForCurrentTenant() {
        return em.createNamedQuery(OrderEntity.FIND_BY_TENANT, OrderEntity.class)
                .setParameter("tenantId", tenantContext.requireTenantId())
                .getResultList();
    }

    /**
     * Laedt eine Bestellung und prueft dabei die Mandantenzugehoerigkeit.
     * Ein Treffer aus einem fremden Mandanten wird wie "nicht gefunden"
     * behandelt - so laesst sich ueber die Antwort nicht einmal ableiten,
     * ob die ID ueberhaupt existiert.
     */
    public Optional<OrderEntity> findById(String id) {
        return Optional.ofNullable(em.find(OrderEntity.class, id))
                .filter(order -> order.getTenantId().equals(tenantContext.requireTenantId()));
    }

    public long countSince(Instant since) {
        return em.createNamedQuery(OrderEntity.COUNT_BY_TENANT_SINCE, Long.class)
                .setParameter("tenantId", tenantContext.requireTenantId())
                .setParameter("since", since)
                .getSingleResult();
    }

    @Transactional
    public OrderEntity save(OrderEntity order) {
        em.persist(order);
        em.flush();
        return order;
    }
}
