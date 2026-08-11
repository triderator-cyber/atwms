package com.atwms.common.outbox;

import com.atwms.common.events.EventJson;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.logging.Logger;

/**
 * Schnittstelle der Fachlogik zur Outbox.
 *
 * <p>Bewusst ohne eigene Transaktionssteuerung: Die Methode laeuft in der
 * Transaktion des Aufrufers mit. Genau darin liegt der Kern des Musters - das
 * Event wird mit demselben Commit dauerhaft wie die fachliche Aenderung. Ein
 * {@code @Transactional(REQUIRES_NEW)} an dieser Stelle wuerde die Garantie
 * zerstoeren.</p>
 */
@ApplicationScoped
public class Outbox {

    private static final Logger LOG = Logger.getLogger(Outbox.class.getName());

    @Inject
    @OutboxEntityManager
    EntityManager em;

    /**
     * Legt ein Event in der Outbox ab.
     *
     * @param eventId       Kennung des Ereignisses; wird zugleich Schluessel des
     *                      Eintrags, damit eine mehrfach zugestellte Nachricht
     *                      beim Konsumenten als Dublette erkennbar bleibt
     * @param topic         Ziel-Topic in Kafka
     * @param aggregateType fachlicher Typ des ausloesenden Objekts, z. B. "Order"
     * @param aggregateId   dessen Schluessel
     * @param eventType     Name des Ereignisses, z. B. "OrderCreated"
     * @param tenantId      Mandant, zu dem das Ereignis gehoert
     * @param payload       beliebiges Objekt, wird als JSON serialisiert
     */
    public void append(String eventId, String topic, String aggregateType, String aggregateId,
                       String eventType, String tenantId, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(eventId);
        event.setTopic(topic);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setTenantId(tenantId);
        event.setPayload(EventJson.toJson(payload));

        em.persist(event);
        LOG.fine(() -> "Outbox-Eintrag angelegt: " + event);
    }
}
