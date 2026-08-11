package com.atwms.order.api;

import com.atwms.common.api.ApiError;
import com.atwms.order.domain.OrderService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

/**
 * Fachliche API des Order-Service.
 *
 * <p>Nirgends taucht eine Mandanten-ID als Parameter auf: sie kommt
 * ausschliesslich aus dem JWT. Ein Client kann damit gar nicht erst versuchen,
 * Daten eines fremden Mandanten anzufordern.</p>
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"tenant-admin", "user"})
public class OrderResource {

    @Inject
    OrderService orderService;

    @GET
    @Operation(summary = "Listet alle Bestellungen des eigenen Mandanten.")
    public List<OrderView> list() {
        return orderService.listOrders().stream().map(OrderView::of).toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Liefert eine einzelne Bestellung des eigenen Mandanten.")
    public Response byId(@PathParam("id") String id) {
        return orderService.findOrder(id)
                .map(order -> Response.ok(OrderView.of(order)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiError("ORDER_NOT_FOUND", "Bestellung existiert nicht."))
                        .build());
    }

    @POST
    @Operation(summary = "Legt eine Bestellung an und veroeffentlicht ein OrderCreated-Event.")
    public Response create(@Valid CreateOrderRequest request) {
        var order = orderService.placeOrder(
                request.getCustomerReference(), request.getAmount(), request.getCurrency());

        return Response.created(UriBuilder.fromResource(OrderResource.class)
                        .path(order.getId()).build())
                .entity(OrderView.of(order))
                .build();
    }
}
