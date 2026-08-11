package com.atwms.common.outbox;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Traegt Outbox-Eintraege nach Kafka.
 *
 * <p>Bewusst kein EJB-Timer, sondern der {@code ManagedScheduledExecutorService}
 * aus Jakarta Concurrency: Diese Klasse liegt in einer Bibliothek unter
 * {@code WEB-INF/lib}, und dort ist die Erkennung von EJB-Komponenten je nach
 * Server unterschiedlich zuverlaessig. Reines CDI funktioniert ueberall.</p>
 *
 * <p>Zustellgarantie ist <em>at least once</em>: Ein Eintrag wird erst nach der
 * Bestaetigung durch Kafka als versendet markiert. Bricht der Prozess zwischen
 * Bestaetigung und Markierung ab, geht die Nachricht ein zweites Mal raus.
 * Deshalb muessen Konsumenten idempotent sein - im Billing-Service leistet das
 * die Unique Constraint auf {@code source_event_id}.</p>
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class.getName());

    @Resource
    ManagedScheduledExecutorService scheduler;

    @Inject
    OutboxRepository repository;

    @Inject
    @Channel("outbox-out")
    Emitter<String> emitter;

    @Inject
    @ConfigProperty(name = "outbox.poll.seconds", defaultValue = "2")
    long pollSeconds;

    @Inject
    @ConfigProperty(name = "outbox.batch.size", defaultValue = "50")
    int batchSize;

    @Inject
    @ConfigProperty(name = "outbox.max.attempts", defaultValue = "10")
    int maxAttempts;

    @Inject
    @ConfigProperty(name = "outbox.retention.hours", defaultValue = "168")
    long retentionHours;

    private ScheduledFuture<?> relayTask;
    private ScheduledFuture<?> purgeTask;

    void onStartup(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
        relayTask = scheduler.scheduleWithFixedDelay(
                this::relayQuietly, pollSeconds, pollSeconds, TimeUnit.SECONDS);
        purgeTask = scheduler.scheduleWithFixedDelay(
                this::purgeQuietly, 1, 1, TimeUnit.HOURS);
        LOG.info(() -> "Outbox-Relay gestartet, Intervall " + pollSeconds + "s, Batchgroesse " + batchSize);
    }

    @PreDestroy
    void onShutdown() {
        if (relayTask != null) {
            relayTask.cancel(false);
        }
        if (purgeTask != null) {
            purgeTask.cancel(false);
        }
    }

    /**
     * Ein Durchlauf: offene Eintraege holen und einzeln uebertragen.
     * Package-private, damit ein Test ihn direkt aufrufen kann.
     */
    void relay() {
        List<OutboxEvent> pending = repository.claimPending(batchSize);
        if (pending.isEmpty()) {
            return;
        }
        LOG.fine(() -> pending.size() + " Outbox-Eintraege zu uebertragen.");

        for (OutboxEvent event : pending) {
            try {
                // Auf die Bestaetigung warten: erst danach gilt der Eintrag als
                // versendet. Ohne dieses Warten wuerden wir Nachrichten als
                // zugestellt markieren, die Kafka nie erreicht haben.
                emitter.send(event.getPayload())
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);
                repository.markSent(event.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Outbox-Eintrag konnte nicht uebertragen werden: " + event, e);
                repository.markAttemptFailed(event.getId(), String.valueOf(e.getMessage()), maxAttempts);
            }
        }
    }

    private void relayQuietly() {
        try {
            relay();
        } catch (RuntimeException e) {
            // Eine Ausnahme aus einem geplanten Task wuerde die Wiederholung
            // stillschweigend beenden - deshalb wird hier alles abgefangen.
            LOG.log(Level.SEVERE, "Outbox-Durchlauf fehlgeschlagen", e);
        }
    }

    private void purgeQuietly() {
        try {
            int removed = repository.purgeSentBefore(Instant.now().minus(Duration.ofHours(retentionHours)));
            if (removed > 0) {
                LOG.info(() -> removed + " bestaetigte Outbox-Eintraege entfernt.");
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Aufraeumen der Outbox fehlgeschlagen", e);
        }
    }
}
