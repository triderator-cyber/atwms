package com.atwms.tenant.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TenantRepository {

    @PersistenceContext(unitName = "tenantPU")
    EntityManager em;

    public Optional<Tenant> findById(String id) {
        return Optional.ofNullable(em.find(Tenant.class, id));
    }

    public List<Tenant> findAll() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tenant> query = cb.createQuery(Tenant.class);
        Root<Tenant> tenant = query.from(Tenant.class);

        query.select(tenant).orderBy(cb.desc(tenant.get(Tenant_.createdAt)));

        return em.createQuery(query).getResultList();
    }

    public boolean exists(String id) {
        return em.find(Tenant.class, id) != null;
    }

    @Transactional
    public Tenant create(Tenant tenant) {
        em.persist(tenant);
        return tenant;
    }

    @Transactional
    public Optional<Tenant> updateStatus(String id, TenantStatus status) {
        return findById(id).map(tenant -> {
            tenant.setStatus(status);
            return tenant;
        });
    }
}
