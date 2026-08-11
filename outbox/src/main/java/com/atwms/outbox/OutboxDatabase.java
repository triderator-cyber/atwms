package com.atwms.outbox;

import com.atwms.common.persistence.DatabaseAccess;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Datenbankzugriff des Outbox-Pollers.
 *
 * <p>Alle Methoden laufen in einer eigenen Transaktion
 * ({@code REQUIRES_NEW}), denn der Poller arbeitet ausserhalb jeder fachlichen
 * Transaktion und soll pro Eintrag einzeln committen. Ein Fehler beim fuenften
 * Event darf die vier bereits uebertragenen nicht zurueckrollen.</p>
 *
 * <p>Das Schreiben in die Outbox liegt bewusst nicht hier, sondern in
 * {@link Outbox}: Dort darf gerade <em>keine</em> eigene Transaktion
 * aufgespannt werden, weil das Event zusammen mit der fachlichen Aenderung
 * committen muss.</p>
 */
@ApplicationScoped
public class OutboxDatabase extends DatabaseAccess {

    @Inject
    @OutboxEntityManager
    EntityManager em;

    @Override
    protected EntityManager em() {
        return em;
    }

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
        return select(OutboxEvent.class,
                        (cb, event) -> cb.equal(event.get(OutboxEvent_.status), OutboxStatus.NEW),
                        (cb, event) -> List.of(cb.asc(event.get(OutboxEvent_.createdAt))))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", -2) // -2 = SKIP LOCKED
                .setMaxResults(batchSize)
                .getResultList();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markSent(String id) {
        byKey(OutboxEvent.class, id).ifPresent(event -> {
            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(Instant.now());
        });
    }

    /**
     * Vermerkt einen Fehlversuch. Nach {@code maxAttempts} Versuchen wandert der
     * Eintrag auf {@link OutboxStatus#FAILED} und wird nicht mehr abgeholt -
     * sonst blockiert ein dauerhaft unzustellbares Event die Warteschlange.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markAttemptFailed(String id, String error, int maxAttempts) {
        byKey(OutboxEvent.class, id).ifPresent(event -> {
            event.setAttempts(event.getAttempts() + 1);
            event.setLastError(error != null && error.length() > 1024 ? error.substring(0, 1024) : error);
            if (event.getAttempts() >= maxAttempts) {
                event.setStatus(OutboxStatus.FAILED);
            }
        });
    }

    /** Raeumt bestaetigte Eintraege ab, damit die Tabelle nicht unbegrenzt waechst. */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public int purgeSentBefore(Instant threshold) {
        return deleteWhere(OutboxEvent.class, (cb, event) -> cb.and(
                cb.equal(event.get(OutboxEvent_.status), OutboxStatus.SENT),
                cb.lessThan(event.get(OutboxEvent_.sentAt), threshold)));
    }
}
