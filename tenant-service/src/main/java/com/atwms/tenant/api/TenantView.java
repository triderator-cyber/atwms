package com.atwms.tenant.api;

import com.atwms.tenant.domain.IsolationModel;
import com.atwms.tenant.domain.Tenant;
import com.atwms.tenant.domain.TenantStatus;

import java.time.Instant;

/**
 * Nach aussen sichtbare Sicht auf einen Mandanten.
 *
 * <p>Eigener DTO statt der Entity: die REST-Schnittstelle ist ein oeffentlicher
 * Vertrag und soll sich nicht automatisch mitaendern, wenn das Datenmodell
 * angepasst wird.</p>
 */
public class TenantView {

    private String id;
    private String displayName;
    private String tier;
    private TenantStatus status;
    private IsolationModel isolationModel;
    private int monthlyOrderLimit;
    private boolean writable;
    private Instant createdAt;

    public static TenantView of(Tenant tenant) {
        TenantView view = new TenantView();
        view.id = tenant.getId();
        view.displayName = tenant.getDisplayName();
        view.tier = tenant.getTier();
        view.status = tenant.getStatus();
        view.isolationModel = tenant.getIsolationModel();
        view.monthlyOrderLimit = tenant.getMonthlyOrderLimit();
        view.writable = tenant.isWritable();
        view.createdAt = tenant.getCreatedAt();
        return view;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public IsolationModel getIsolationModel() {
        return isolationModel;
    }

    public void setIsolationModel(IsolationModel isolationModel) {
        this.isolationModel = isolationModel;
    }

    public int getMonthlyOrderLimit() {
        return monthlyOrderLimit;
    }

    public void setMonthlyOrderLimit(int monthlyOrderLimit) {
        this.monthlyOrderLimit = monthlyOrderLimit;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
