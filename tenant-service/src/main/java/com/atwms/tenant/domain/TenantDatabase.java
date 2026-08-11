package com.atwms.tenant.domain;

import com.atwms.common.persistence.DatabaseAccess;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Saemtlicher Datenbankzugriff des Tenant-Service.
 *
 * <p>Der Service besitzt genau diese eine Zugriffsklasse. Kommt spaeter eine
 * weitere Entity dazu, wandern deren Abfragen ebenfalls hierher - nicht in eine
 * zweite Klasse. Dadurch gibt es einen einzigen Ort, an dem sich beantworten
 * laesst, welche Abfragen dieser Service ueberhaupt stellt.</p>
 */
@ApplicationScoped
public class TenantDatabase extends DatabaseAccess {

    @PersistenceContext(unitName = "tenantPU")
    EntityManager em;

    @Override
    protected EntityManager em() {
        return em;
    }

    public Optional<Tenant> findById(String id) {
        return byKey(Tenant.class, id);
    }

    public List<Tenant> findAll() {
        return list(Tenant.class, null,
                (cb, tenant) -> List.of(cb.desc(tenant.get(Tenant_.createdAt))));
    }

    public boolean exists(String id) {
        return findById(id).isPresent();
    }

    @Transactional
    public Tenant create(Tenant tenant) {
        return persist(tenant);
    }

    @Transactional
    public Optional<Tenant> updateStatus(String id, TenantStatus status) {
        return findById(id).map(tenant -> {
            tenant.setStatus(status);
            return tenant;
        });
    }
}
