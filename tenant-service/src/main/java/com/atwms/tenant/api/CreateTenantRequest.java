package com.atwms.tenant.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateTenantRequest {

    @NotBlank
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,62}")
    private String id;

    @NotBlank
    private String displayName;

    private String tier = "free";

    @Min(1)
    private int monthlyOrderLimit = 100;

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

    public int getMonthlyOrderLimit() {
        return monthlyOrderLimit;
    }

    public void setMonthlyOrderLimit(int monthlyOrderLimit) {
        this.monthlyOrderLimit = monthlyOrderLimit;
    }
}
