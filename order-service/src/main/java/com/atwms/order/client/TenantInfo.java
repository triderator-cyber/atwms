package com.atwms.order.client;

/**
 * Ausschnitt der Tenant-Service-Antwort, den der Order-Service tatsaechlich braucht.
 *
 * <p>Bewusst nur die benoetigten Felder: JSON-B ignoriert unbekannte Properties,
 * dadurch kann der Tenant-Service seine Antwort erweitern, ohne dass hier etwas
 * angepasst werden muss (Tolerant Reader).</p>
 */
public class TenantInfo {

    private String id;
    private String status;
    private String tier;
    private boolean writable;
    private int monthlyOrderLimit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public int getMonthlyOrderLimit() {
        return monthlyOrderLimit;
    }

    public void setMonthlyOrderLimit(int monthlyOrderLimit) {
        this.monthlyOrderLimit = monthlyOrderLimit;
    }
}
