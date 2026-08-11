package com.atwms.order.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class CreateOrderRequest {

    @NotBlank
    private String customerReference;

    @NotNull
    @DecimalMin(value = "0.01", message = "Der Betrag muss groesser als null sein.")
    private BigDecimal amount;

    @Pattern(regexp = "[A-Z]{3}", message = "Waehrung als ISO-4217-Code erwartet, z. B. EUR.")
    private String currency = "EUR";

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
}
