package com.atwms.order.api;

import com.atwms.order.domain.OrderEntity;
import com.atwms.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderView {

    private String id;
    private String customerReference;
    private BigDecimal amount;
    private String currency;
    private OrderStatus status;
    private Instant createdAt;

    public static OrderView of(OrderEntity order) {
        OrderView view = new OrderView();
        view.id = order.getId();
        view.customerReference = order.getCustomerReference();
        view.amount = order.getAmount();
        view.currency = order.getCurrency();
        view.status = order.getStatus();
        view.createdAt = order.getCreatedAt();
        // tenantId wird bewusst NICHT nach aussen gegeben - der Client kennt
        // seinen Mandanten ohnehin, und interne Schluessel gehoeren nicht in
        // eine oeffentliche API.
        return view;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public void setCustomerReference(String customerReference) {
        this.customerReference = customerReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
