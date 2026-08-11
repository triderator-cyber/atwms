package com.atwms.order.domain;

import jakarta.ws.rs.core.Response;

/**
 * Fachliche Ablehnung einer Bestellung (gesperrter Mandant, Limit erreicht, ...).
 */
public class OrderRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final Response.Status status;

    public OrderRejectedException(String code, Response.Status status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public Response.Status getStatus() {
        return status;
    }
}
