package com.atwms.common.outbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Datenzugriff des Pollers auf die Outbox.
 *
 * <p>Alle Methoden laufen in einer eigenen Transaktion
 * ({@code REQUIRES_NEW}), denn der Poller arbeitet ausserhalb jeder fachlichen
 * Transaktion und soll pro Eintrag einzeln committen. Ein Fehler beim
 * fuenften Event darf die vier bereits uebertragenen nicht zurueckrollen.</p>
 */
@ApplicationScoped
public class OutboxRepository {

    @Inject
    @OutboxEntityManager
    EntityManager em;

    /**
     * Holt die naechsten unversendeten Eintraege.
     *
     * <p>{@code PESSIMISTIC_WRITE} zusammen mit {@code SKIP LOCKED} sorgt
     * dafuer, dass mehrere Instanzen des Service parallel pollen koennen, ohne
     * sich dieselben Zeilen zu greifen. Ohne diese Sperre wuerde bei zwei
     * laufenden Pods jedes Event doppelt verschickt.</p>
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public List<OutboxEvent> claimPending(int batchSize) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OutboxEvent> query = cb.createQuery(OutboxEvent.class);
        Root<OutboxEvent> event = query.from(OutboxEvent.class);

        query.select(event)
                .where(cb.equal(event.get(OutboxEvent_.status), OutboxStatus.NEW))
                .orderBy(cb.asc(event.get(OutboxEvent_.createdAt)));

        return em.createQuery(query)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", -2) // -2 = SKIP LOCKED
                .setMaxResults(batchSize)
                .getResultList();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markSent(String id) {
        OutboxEvent event = em.find(OutboxEvent.class, id);
        if (event != null) {
            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(Instant.now());
        }
    }

    /**
     * Vermerkt einen Fehlversuch. Nach {@code maxAttempts} Versuchen wandert der
     * Eintrag auf {@link OutboxStatus#FAILED} und wird nicht mehr abgeholt -
     * sonst blockiert ein dauerhaft unzustellbares Event die Warteschlange.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markAttemptFailed(String id, String error, int maxAttempts) {
        OutboxEvent event = em.find(OutboxEvent.class, id);
        if (event == null) {
            return;
        }
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(error != null && error.length() > 1024 ? error.substring(0, 1024) : error);
        if (event.getAttempts() >= maxAttempts) {
            event.setStatus(OutboxStatus.FAILED);
        }
    }

    /** Raeumt bestaetigte Eintraege ab, damit die Tabelle nicht unbegrenzt waechst. */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public int purgeSentBefore(Instant threshold) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<OutboxEvent> delete = cb.createCriteriaDelete(OutboxEvent.class);
        Root<OutboxEvent> event = delete.from(OutboxEvent.class);

        delete.where(cb.and(
                cb.equal(event.get(OutboxEvent_.status), OutboxStatus.SENT),
                cb.lessThan(event.get(OutboxEvent_.sentAt), threshold)));

        return em.createQuery(delete).executeUpdate();
    }
}
