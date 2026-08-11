package com.atwms.order.config;

import com.atwms.outbox.OutboxEntityManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Verbindet das Outbox-Modul mit der Persistence Unit
 * dieses Service.
 *
 * <p>Der EntityManager ist derselbe, den auch {@code OrderDatabase}
 * verwendet. Nur dadurch landen Bestellung und Outbox-Eintrag in einer
 * gemeinsamen Transaktion.</p>
 */
@ApplicationScoped
public class OutboxPersistenceProducer {

    @Produces
    @OutboxEntityManager
    @PersistenceContext(unitName = "orderPU")
    EntityManager entityManager;
}
