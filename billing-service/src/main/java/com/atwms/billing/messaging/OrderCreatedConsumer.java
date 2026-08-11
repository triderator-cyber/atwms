package com.atwms.billing.messaging;

import com.atwms.common.events.EventJson;
import com.atwms.common.events.OrderCreatedEvent;
import com.atwms.billing.domain.UsageRecord;
import com.atwms.billing.domain.UsageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verarbeitet {@code OrderCreated}-Events und schreibt daraus Verbrauchspositionen.
 *
 * <p>Der Billing-Service ruft den Order-Service nie auf und wird von ihm auch
 * nicht aufgerufen - beide kennen nur das Event. Faellt der Billing-Service
 * aus, bleiben die Nachrichten in Kafka liegen und werden nach dem Neustart
 * nachgeholt; die Bestellannahme laeuft in der Zwischenzeit unbeeintraechtigt
 * weiter.</p>
 *
 * <p>Der Mandant kommt hier aus der Nachricht und nicht aus einem JWT: ein
 * Consumer hat keinen HTTP-Request und damit keinen Sicherheitskontext.
 * Deshalb transportiert jedes Event seine {@code tenantId} selbst.</p>
 *
 * <p>Hinweis: Die Methode arbeitet blockierend (JDBC). In SmallRye laesst sich
 * das ueber {@code io.smallrye.common.annotation.Blocking} sauber auf einen
 * Worker-Thread auslagern - fuer den Einstieg bewusst weggelassen, um keine
 * implementierungsspezifische Abhaengigkeit einzufuehren.</p>
 */
@ApplicationScoped
public class OrderCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(OrderCreatedConsumer.class.getName());
    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyy-MM")
            .withZone(ZoneOffset.UTC);

    @Inject
    UsageRepository repository;

    @Incoming("order-created-in")
    public void onOrderCreated(String payload) {
        try {
            OrderCreatedEvent event = EventJson.fromJson(payload, OrderCreatedEvent.class);

            UsageRecord record = new UsageRecord();
            record.setTenantId(event.getTenantId());
            record.setSourceEventId(event.getEventId());
            record.setOrderId(event.getOrderId());
            record.setAmount(event.getAmount());
            record.setCurrency(event.getCurrency());
            record.setBillingPeriod(PERIOD.format(event.getOccurredAt()));

            if (repository.recordIfNew(record)) {
                LOG.fine(() -> "Verbrauch verbucht fuer Mandant " + event.getTenantId());
            } else {
                LOG.fine(() -> "Event " + event.getEventId() + " war bereits verbucht - ignoriert.");
            }
        } catch (RuntimeException e) {
            // Eine unverarbeitbare Nachricht darf den Consumer nicht dauerhaft
            // blockieren. In Produktion gehoert sie in ein Dead-Letter-Topic.
            LOG.log(Level.SEVERE, "OrderCreated-Event konnte nicht verarbeitet werden: " + payload, e);
        }
    }
}
