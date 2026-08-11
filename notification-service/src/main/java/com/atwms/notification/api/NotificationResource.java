package com.atwms.notification.api;

import com.atwms.common.tenant.TenantContext;
import com.atwms.notification.domain.Notification;
import com.atwms.notification.domain.NotificationLog;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"tenant-admin", "user"})
public class NotificationResource {

    @Inject
    NotificationLog notificationLog;

    @Inject
    TenantContext tenantContext;

    @GET
    @Operation(summary = "Zuletzt versendete Benachrichtigungen des eigenen Mandanten.")
    public List<Notification> recent() {
        return notificationLog.recentFor(tenantContext.requireTenantId());
    }
}
