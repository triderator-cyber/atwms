package com.atwms.tenant.api;

import com.atwms.common.api.ApiError;
import com.atwms.common.tenant.TenantContext;
import com.atwms.tenant.domain.Tenant;
import com.atwms.tenant.domain.TenantRepository;
import com.atwms.tenant.domain.TenantStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

/**
 * Control-Plane-API zur Mandantenverwaltung.
 *
 * <p>Rollenmodell:</p>
 * <ul>
 *   <li>{@code platform-admin} - Betreiber der Plattform, sieht alle Mandanten.</li>
 *   <li>{@code tenant-admin} - Administrator eines einzelnen Mandanten,
 *       sieht ausschliesslich den eigenen Datensatz.</li>
 *   <li>{@code service} - technische Service-Accounts der uebrigen
 *       Microservices (Client Credentials Flow in Keycloak).</li>
 * </ul>
 */
@Path("/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TenantResource {

    @Inject
    TenantRepository repository;

    @Inject
    TenantContext tenantContext;

    @Context
    SecurityContext securityContext;

    @GET
    @RolesAllowed("platform-admin")
    @Operation(summary = "Listet alle Mandanten der Plattform.")
    public List<TenantView> list() {
        return repository.findAll().stream().map(TenantView::of).toList();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"platform-admin", "tenant-admin", "user"})
    @Operation(summary = "Liefert den Mandanten des aktuell angemeldeten Nutzers.")
    public Response currentTenant() {
        return repository.findById(tenantContext.requireTenantId())
                .map(tenant -> Response.ok(TenantView.of(tenant)).build())
                .orElseGet(() -> notFound(tenantContext.requireTenantId()));
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"platform-admin", "tenant-admin", "service"})
    @Operation(summary = "Liefert einen einzelnen Mandanten.")
    public Response byId(@PathParam("id") String id) {
        // Wer kein Plattform-Administrator ist, darf ausschliesslich den eigenen
        // Mandanten lesen. Ohne diese Pruefung waere die Mandantentrennung an
        // dieser Stelle durch simples Hochzaehlen der ID zu umgehen.
        if (!isPlatformAdmin() && !id.equals(tenantContext.getTenantId().orElse(null))) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ApiError("TENANT_MISMATCH",
                            "Zugriff auf fremde Mandanten ist nicht erlaubt."))
                    .build();
        }
        return repository.findById(id)
                .map(tenant -> Response.ok(TenantView.of(tenant)).build())
                .orElseGet(() -> notFound(id));
    }

    @POST
    @RolesAllowed("platform-admin")
    @Operation(summary = "Legt einen neuen Mandanten an (Onboarding).")
    public Response create(@Valid CreateTenantRequest request) {
        if (repository.exists(request.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("TENANT_EXISTS",
                            "Mandant '" + request.getId() + "' existiert bereits."))
                    .build();
        }
        Tenant tenant = new Tenant(request.getId(), request.getDisplayName());
        tenant.setTier(request.getTier());
        tenant.setMonthlyOrderLimit(request.getMonthlyOrderLimit());
        // In einem echten Onboarding folgt hier zusaetzlich das Provisioning:
        // Keycloak-Gruppe anlegen, ggf. Schema erzeugen, Willkommens-Mail ausloesen.
        // Deshalb bleibt der Status zunaechst PROVISIONING.
        repository.create(tenant);

        return Response.created(UriBuilder.fromResource(TenantResource.class)
                        .path(tenant.getId()).build())
                .entity(TenantView.of(tenant))
                .build();
    }

    @PUT
    @Path("/{id}/status")
    @RolesAllowed("platform-admin")
    @Operation(summary = "Aendert den Status eines Mandanten (z. B. Sperre bei Zahlungsverzug).")
    public Response changeStatus(@PathParam("id") String id, TenantStatus status) {
        return repository.updateStatus(id, status)
                .map(tenant -> Response.ok(TenantView.of(tenant)).build())
                .orElseGet(() -> notFound(id));
    }

    private boolean isPlatformAdmin() {
        // Die grobe Rollenpruefung ist ueber @RolesAllowed bereits erfolgt;
        // hier geht es nur noch um die feinere Unterscheidung innerhalb der
        // erlaubten Rollen.
        return securityContext != null && securityContext.isUserInRole("platform-admin");
    }

    private static Response notFound(String id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ApiError("TENANT_NOT_FOUND", "Mandant '" + id + "' existiert nicht."))
                .build();
    }
}
