package com.atwms.order.messaging;

import com.atwms.common.events.EventJson;
import com.atwms.common.events.OrderCreatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Veroeffentlicht fachliche Ereignisse auf Kafka.
 *
 * <p>Der Order-Service kennt weder Billing- noch Notification-Service. Er
 * stellt lediglich fest, dass eine Bestellung entstanden ist. Wer darauf
 * reagiert, entscheidet sich auf der Consumer-Seite - deshalb kann ein
 * weiterer Service spaeter hinzukommen, ohne dass hier eine Zeile geaendert
 * wird.</p>
 */
@ApplicationScoped
public class OrderEventPublisher {

    private static final Logger LOG = Logger.getLogger(OrderEventPublisher.class.getName());

    @Inject
    @Channel("order-created-out")
    Emitter<String> emitter;

    public void publish(OrderCreatedEvent event) {
        try {
            emitter.send(EventJson.toJson(event));
            LOG.fine(() -> "Event veroeffentlicht: " + event);
        } catch (RuntimeException e) {
            // Die Bestellung ist bereits committed. Ein fehlgeschlagenes Event
            // darf den fachlichen Vorgang nicht zurueckrollen - stattdessen
            // wird es protokolliert. Fuer garantierte Zustellung ist das
            // Transactional-Outbox-Pattern der naechste Schritt (siehe README).
            LOG.log(Level.SEVERE, "Event konnte nicht veroeffentlicht werden: " + event, e);
        }
    }
}
