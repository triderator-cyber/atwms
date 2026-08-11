package com.atwms.common.api;

import com.atwms.common.tenant.UnresolvedTenantException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/**
 * Ein Request ohne aufloesbaren Mandanten ist immer ein Sicherheitsproblem,
 * niemals ein fachlicher Fehler - deshalb 403 und eine bewusst nichtssagende
 * Fehlermeldung nach aussen.
 */
@Provider
public class UnresolvedTenantExceptionMapper implements ExceptionMapper<UnresolvedTenantException> {

    private static final Logger LOG = Logger.getLogger(UnresolvedTenantExceptionMapper.class.getName());

    @Override
    public Response toResponse(UnresolvedTenantException exception) {
        LOG.warning("Mandantenkontext konnte nicht aufgeloest werden: " + exception.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError("TENANT_UNRESOLVED", "Kein gueltiger Mandantenkontext."))
                .build();
    }
}
