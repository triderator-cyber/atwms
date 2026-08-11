package com.atwms.common.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @Test
    void requireTenantIdFailsWhenNoTenantWasResolved() {
        TenantContext context = new TenantContext();

        assertThatThrownBy(context::requireTenantId)
                .isInstanceOf(UnresolvedTenantException.class);
    }

    @Test
    void requireTenantIdFailsForBlankTenant() {
        TenantContext context = new TenantContext();
        context.initialise("   ", "pro", "user-1");

        assertThatThrownBy(context::requireTenantId)
                .isInstanceOf(UnresolvedTenantException.class);
    }

    @Test
    void exposesResolvedTenant() {
        TenantContext context = new TenantContext();
        context.initialise("acme", "enterprise", "user-1");

        assertThat(context.requireTenantId()).isEqualTo("acme");
        assertThat(context.getTier()).contains("enterprise");
        assertThat(context.getSubject()).contains("user-1");
    }
}
