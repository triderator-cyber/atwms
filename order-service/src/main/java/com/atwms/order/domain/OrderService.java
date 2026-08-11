package com.atwms.order.domain;

import com.atwms.common.events.OrderCreatedEvent;
import com.atwms.common.tenant.TenantContext;
import com.atwms.order.client.TenantInfo;
import com.atwms.order.client.TenantPolicy;
import com.atwms.order.messaging.OrderEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    OrderEventPublisher eventPublisher;

    public List<OrderEntity> listOrders() {
        return repository.findAllForCurrentTenant();
    }

    public Optional<OrderEntity> findOrder(String id) {
        return repository.findById(id);
    }

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

        // Erst nach erfolgreichem Commit informieren wir die uebrige Landschaft.
        eventPublisher.publish(toEvent(order));
        return order;
    }

    private OrderCreatedEvent toEvent(OrderEntity order) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTenantId(tenantContext.requireTenantId());
        event.setOrderId(order.getId());
        event.setCustomerReference(order.getCustomerReference());
        event.setAmount(order.getAmount());
        event.setCurrency(order.getCurrency());
        event.setOccurredAt(order.getCreatedAt());
        return event;
    }
}
