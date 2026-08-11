package com.atwms.order.api;

import com.atwms.common.api.ApiError;
import com.atwms.order.domain.OrderRejectedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OrderRejectedExceptionMapper implements ExceptionMapper<OrderRejectedException> {

    @Override
    public Response toResponse(OrderRejectedException exception) {
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(exception.getCode(), exception.getMessage()))
                .build();
    }
}
