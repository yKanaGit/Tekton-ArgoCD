package com.example.logistics.integration;

import com.example.logistics.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Path("/integration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntegrationResource {

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/api/shipment-orders")
    @Transactional
    public Response receiveApiOrder(ShipmentOrderRequest request) {
        return processOrder(request, Channel.API);
    }

    @POST
    @Path("/edi/shipment-orders")
    @Transactional
    public Response receiveEdiOrder(ShipmentOrderRequest request) {
        return processOrder(request, Channel.EDI);
    }

    @POST
    @Path("/file/shipment-orders")
    @Transactional
    public Response receiveFileOrder(ShipmentOrderRequest request) {
        return processOrder(request, Channel.FILE);
    }

    @GET
    @Path("/status/{correlationId}")
    public Response getStatus(@PathParam("correlationId") String correlationId) {
        IntegrationEvent event = IntegrationEvent.findByCorrelationId(correlationId);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("連携追跡番号が見つかりません: " + correlationId))
                    .build();
        }

        IntegrationResponse response = new IntegrationResponse(
                event.correlationId,
                event.shipperCode,
                event.siteCode,
                event.channel.name(),
                event.status.name(),
                event.receivedAt,
                event.message
        );
        return Response.ok(response).build();
    }

    @POST
    @Path("/retry/{correlationId}")
    @Transactional
    public Response retry(@PathParam("correlationId") String correlationId) {
        IntegrationEvent event = IntegrationEvent.findByCorrelationId(correlationId);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("連携追跡番号が見つかりません: " + correlationId))
                    .build();
        }

        if (event.status != IntegrationStatus.FAILED) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("エラー状態のイベントのみ再試行できます。現在のステータス: " + event.status))
                    .build();
        }

        event.status = IntegrationStatus.RECEIVED;
        event.message = "再試行実行: " + LocalDateTime.now();

        IntegrationResponse response = new IntegrationResponse(
                event.correlationId,
                event.shipperCode,
                event.siteCode,
                event.channel.name(),
                event.status.name(),
                event.receivedAt,
                event.message
        );
        return Response.ok(response).build();
    }

    @GET
    @Path("/events")
    public List<IntegrationEvent> listEvents(
            @QueryParam("shipperCode") String shipperCode,
            @QueryParam("status") String status) {

        if (shipperCode != null && !shipperCode.isEmpty()) {
            return IntegrationEvent.findByShipperCode(shipperCode);
        }

        if (status != null && !status.isEmpty()) {
            try {
                IntegrationStatus statusEnum = IntegrationStatus.valueOf(status.toUpperCase());
                return IntegrationEvent.list("status", statusEnum);
            } catch (IllegalArgumentException e) {
                throw new WebApplicationException("Invalid status: " + status, Response.Status.BAD_REQUEST);
            }
        }

        return IntegrationEvent.listAll();
    }

    private Response processOrder(ShipmentOrderRequest request, Channel channel) {
        String correlationId = UUID.randomUUID().toString();

        Shipper shipper = Shipper.findByShipperCode(request.shipperCode);
        if (shipper == null) {
            IntegrationEvent failedEvent = createEvent(correlationId, request.shipperCode, null,
                    channel, IntegrationStatus.FAILED,
                    "荷主が見つかりません: " + request.shipperCode, serializeRequest(request));
            failedEvent.persist();

            IntegrationResponse response = new IntegrationResponse(
                    correlationId, request.shipperCode, null,
                    channel.name(), "FAILED", failedEvent.receivedAt,
                    failedEvent.message
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }

        Site originSite = Site.findBySiteCode(request.originSiteCode);
        Site destinationSite = Site.findBySiteCode(request.destinationSiteCode);

        if (originSite == null || destinationSite == null) {
            String errorMsg = "拠点が見つかりません: " +
                    (originSite == null ? request.originSiteCode : request.destinationSiteCode);
            IntegrationEvent failedEvent = createEvent(correlationId, request.shipperCode,
                    request.originSiteCode != null ? request.originSiteCode : request.destinationSiteCode,
                    channel, IntegrationStatus.FAILED, errorMsg, serializeRequest(request));
            failedEvent.persist();

            IntegrationResponse response = new IntegrationResponse(
                    correlationId, request.shipperCode, null,
                    channel.name(), "FAILED", failedEvent.receivedAt, errorMsg
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }

        String message;
        switch (channel) {
            case API:
                message = "API連携で出荷指示を受け付けました";
                break;
            case EDI:
                message = "EDI連携で出荷指示を受け付けました";
                break;
            case FILE:
                message = "ファイル連携で出荷指示を受け付けました";
                break;
            default:
                message = "出荷指示を受け付けました";
        }

        IntegrationEvent event = createEvent(correlationId, request.shipperCode, request.originSiteCode,
                channel, IntegrationStatus.RECEIVED, message, serializeRequest(request));
        event.persist();

        ShipmentOrder order = new ShipmentOrder();
        order.orderNumber = "ORD-" + correlationId.substring(0, 8).toUpperCase();
        order.shipper = shipper;
        order.originSite = originSite;
        order.destinationSite = destinationSite;
        order.itemDescription = request.itemDescription;
        order.quantity = request.quantity;
        order.status = IntegrationStatus.RECEIVED;
        order.correlationId = correlationId;
        order.createdAt = LocalDateTime.now();
        order.persist();

        IntegrationResponse response = new IntegrationResponse(
                correlationId, request.shipperCode, request.originSiteCode,
                channel.name(), "RECEIVED", event.receivedAt, event.message
        );
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    private IntegrationEvent createEvent(String correlationId, String shipperCode, String siteCode,
                                         Channel channel, IntegrationStatus status,
                                         String message, String payload) {
        IntegrationEvent event = new IntegrationEvent();
        event.correlationId = correlationId;
        event.shipperCode = shipperCode;
        event.siteCode = siteCode;
        event.channel = channel;
        event.status = status;
        event.receivedAt = LocalDateTime.now();
        event.message = message;
        event.payload = payload;
        return event;
    }

    private String serializeRequest(ShipmentOrderRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize request\"}";
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
