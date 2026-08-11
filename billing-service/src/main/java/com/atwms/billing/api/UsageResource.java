package com.atwms.billing.api;

import com.atwms.billing.domain.UsageRepository;
import com.atwms.common.tenant.TenantContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.time.YearMonth;

/**
 * Verbrauchsuebersicht des eigenen Mandanten.
 */
@Path("/usage")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"tenant-admin", "platform-admin"})
public class UsageResource {

    @Inject
    UsageRepository repository;

    @Inject
    TenantContext tenantContext;

    @GET
    @Path("/current")
    @Operation(summary = "Verbrauch des eigenen Mandanten im angegebenen Abrechnungszeitraum.")
    public UsageSummary current(@QueryParam("period") @DefaultValue("") String period) {
        String tenantId = tenantContext.requireTenantId();
        String effectivePeriod = period.isBlank() ? YearMonth.now().toString() : period;

        UsageSummary summary = new UsageSummary();
        summary.setTenantId(tenantId);
        summary.setPeriod(effectivePeriod);
        summary.setOrderCount(repository.countForPeriod(tenantId, effectivePeriod));
        summary.setTotalAmount(repository.sumForPeriod(tenantId, effectivePeriod));
        return summary;
    }
}
