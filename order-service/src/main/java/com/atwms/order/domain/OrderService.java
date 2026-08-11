package com.atwms.order.domain;

import com.atwms.common.events.OrderCreatedEvent;
import com.atwms.common.events.Topics;
import com.atwms.common.outbox.Outbox;
import com.atwms.common.tenant.TenantContext;
import com.atwms.order.client.TenantInfo;
import com.atwms.order.client.TenantPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fachlogik des Order-Service.
 *
 * <p>Zeigt das typische Zusammenspiel in einer Microservice-Landschaft:
 * ein synchroner Aufruf dort, wo eine Entscheidung sofort gebraucht wird
 * (darf dieser Mandant ueberhaupt bestellen?), und ein asynchrones Event
 * dort, wo andere Services nur informiert werden muessen.</p>
 */
@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository repository;

    @Inject
    TenantPolicy tenantPolicy;

    @Inject
    TenantContext tenantContext;

    @Inject
    Outbox outbox;

    public List<OrderEntity> listOrders() {
        return repository.findAllForCurrentTenant();
    }

    public Optional<OrderEntity> findOrder(String id) {
        return repository.findById(id);
    }

    /**
     * Legt eine Bestellung an und hinterlegt das zugehoerige Event in der Outbox.
     *
     * <p>{@code @Transactional} klammert beides in eine Transaktion. Damit gibt
     * es nur noch zwei moegliche Ausgaenge: Bestellung und Event sind
     * gespeichert, oder keines von beidem. Der frueher moegliche Fall - Bestellung
     * committed, Event verloren, weil der Prozess dazwischen abstuerzte - kann
     * nicht mehr eintreten.</p>
     *
     * <p>Die Pruefungen davor stehen bewusst ausserhalb der Schreiboperationen:
     * ein abgelehnter Auftrag soll gar nichts hinterlassen.</p>
     */
    @Transactional
    public OrderEntity placeOrder(String customerReference, BigDecimal amount, String currency) {
        TenantInfo tenant = tenantPolicy.currentTenant()
                .orElseThrow(() -> new OrderRejectedException(
                        "TENANT_UNAVAILABLE",
                        Response.Status.SERVICE_UNAVAILABLE,
                        "Mandantenstatus ist derzeit nicht pruefbar. Bitte spaeter erneut versuchen."));

        if (!tenant.isWritable()) {
            throw new OrderRejectedException(
                    "TENANT_NOT_WRITABLE",
                    Response.Status.CONFLICT,
                    "Mandant befindet sich im Status '" + tenant.getStatus()
                            + "' und kann keine Bestellungen anlegen.");
        }

        Instant monthStart = Instant.now().truncatedTo(ChronoUnit.DAYS).minus(30, ChronoUnit.DAYS);
        long used = repository.countSince(monthStart);
        if (used >= tenant.getMonthlyOrderLimit()) {
            throw new OrderRejectedException(
                    "QUOTA_EXCEEDED",
                    Response.Status.TOO_MANY_REQUESTS,
                    "Monatslimit von " + tenant.getMonthlyOrderLimit() + " Bestellungen ist erreicht.");
        }

        OrderEntity order = repository.save(new OrderEntity(customerReference, amount, currency));

        // Kein Kafka-Aufruf an dieser Stelle: das Event geht in dieselbe
        // Datenbanktransaktion. Die Uebertragung uebernimmt spaeter das
        // OutboxRelay, unabhaengig vom Antwortzeitpunkt dieses Requests.
        //
        // Die eventId entsteht hier und dient zugleich als Schluessel des
        // Outbox-Eintrags. Dadurch traegt eine mehrfach zugestellte Nachricht
        // immer dieselbe Kennung, und Konsumenten erkennen die Dublette.
        String eventId = UUID.randomUUID().toString();
        outbox.append(
                eventId,
                Topics.ORDER_CREATED,
                "Order",
                order.getId(),
                "OrderCreated",
                tenantContext.requireTenantId(),
                toEvent(order, eventId));
        return order;
    }

    private OrderCreatedEvent toEvent(OrderEntity order, String eventId) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(eventId);
        event.setTenantId(tenantContext.requireTenantId());
        event.setOrderId(order.getId());
        event.setCustomerReference(order.getCustomerReference());
        event.setAmount(order.getAmount());
        event.setCurrency(order.getCurrency());
        event.setOccurredAt(order.getCreatedAt());
        return event;
    }
}
