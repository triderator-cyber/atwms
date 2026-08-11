package com.atwms.order.config;

import com.atwms.common.outbox.OutboxEntityManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Verbindet die Outbox aus dem gemeinsamen Modul mit der Persistence Unit
 * dieses Service.
 *
 * <p>Der EntityManager ist derselbe, den auch {@code OrderRepository}
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
