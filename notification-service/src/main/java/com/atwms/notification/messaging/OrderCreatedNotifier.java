package com.atwms.notification.messaging;

import com.atwms.common.events.EventJson;
import com.atwms.common.events.OrderCreatedEvent;
import com.atwms.notification.domain.Notification;
import com.atwms.notification.domain.NotificationLog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zweiter, voellig unabhaengiger Consumer desselben Events.
 *
 * <p>Das ist der praktische Kern der ereignisgetriebenen Entkopplung: dieser
 * Service wurde hinzugefuegt, ohne dass am Order-Service auch nur eine Zeile
 * geaendert werden musste. Er nutzt lediglich eine eigene Consumer-Group und
 * erhaelt damit denselben Nachrichtenstrom wie der Billing-Service.</p>
 */
@ApplicationScoped
public class OrderCreatedNotifier {

    private static final Logger LOG = Logger.getLogger(OrderCreatedNotifier.class.getName());

    @Inject
    NotificationLog notificationLog;

    @Incoming("order-created-in")
    public void onOrderCreated(String payload) {
        try {
            OrderCreatedEvent event = EventJson.fromJson(payload, OrderCreatedEvent.class);

            Notification notification = new Notification(
                    event.getTenantId(),
                    "Bestellung " + event.getOrderId() + " eingegangen",
                    "Ihre Bestellung ueber " + event.getAmount() + " " + event.getCurrency()
                            + " (Referenz " + event.getCustomerReference() + ") wurde angelegt.");

            // Hier wuerde der eigentliche Versand stehen (SMTP, Push, Webhook).
            LOG.info(() -> "[MAIL/" + notification.getTenantId() + "] " + notification.getSubject());
            notificationLog.record(notification);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Benachrichtigung konnte nicht erzeugt werden: " + payload, e);
        }
    }
}
