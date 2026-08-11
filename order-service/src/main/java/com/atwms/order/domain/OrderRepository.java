package com.atwms.order.domain;

import com.atwms.common.tenant.TenantAwareEntity_;
import com.atwms.common.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Datenzugriff fuer Bestellungen.
 *
 * <p>Das Repository ist die einzige Stelle, an der Abfragen entstehen, und
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
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OrderEntity> query = cb.createQuery(OrderEntity.class);
        Root<OrderEntity> order = query.from(OrderEntity.class);

        query.select(order)
                .where(cb.equal(order.get(TenantAwareEntity_.tenantId), tenantContext.requireTenantId()))
                .orderBy(cb.desc(order.get(OrderEntity_.createdAt)));

        return em.createQuery(query).getResultList();
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
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<OrderEntity> order = query.from(OrderEntity.class);

        query.select(cb.count(order))
                .where(cb.and(
                        cb.equal(order.get(TenantAwareEntity_.tenantId), tenantContext.requireTenantId()),
                        cb.greaterThanOrEqualTo(order.get(OrderEntity_.createdAt), since)));

        return em.createQuery(query).getSingleResult();
    }

    @Transactional
    public OrderEntity save(OrderEntity order) {
        em.persist(order);
        em.flush();
        return order;
    }
}
