package com.atwms.order.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Typsicherer Zugriff auf den Tenant-Service (synchroner Service-zu-Service-Aufruf).
 *
 * <p>{@code @RegisterClientHeaders} sorgt zusammen mit der Property
 * {@code org.eclipse.microprofile.rest.client.propagateHeaders=Authorization}
 * dafuer, dass das eingehende JWT weitergereicht wird. Der Tenant-Service
 * bekommt damit denselben Sicherheitskontext und kann die Berechtigung selbst
 * pruefen, statt dem Aufrufer blind zu vertrauen.</p>
 *
 * <p>Die Basis-URL steht nicht im Code, sondern kommt ueber MicroProfile Config
 * aus {@code tenant-api/mp-rest/url} - in Kubernetes typischerweise der
 * Service-Name, lokal die Adresse aus docker-compose.</p>
 */
@RegisterRestClient(configKey = "tenant-api")
@RegisterClientHeaders
@Path("/api/tenants")
public interface TenantClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    TenantInfo byId(@PathParam("id") String id);
}
