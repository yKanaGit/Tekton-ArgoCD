package com.example.logistics.resource;

import com.example.logistics.model.IntegrationPartner;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/partners")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntegrationPartnerResource {

    @GET
    public List<IntegrationPartner> listAll() {
        return IntegrationPartner.listAll();
    }

    @GET
    @Path("/{id}")
    public IntegrationPartner getById(@PathParam("id") Long id) {
        IntegrationPartner partner = IntegrationPartner.findById(id);
        if (partner == null) {
            throw new WebApplicationException("Integration Partner not found", Response.Status.NOT_FOUND);
        }
        return partner;
    }

    @POST
    @Transactional
    public Response create(IntegrationPartner partner) {
        partner.persist();
        return Response.status(Response.Status.CREATED).entity(partner).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public IntegrationPartner update(@PathParam("id") Long id, IntegrationPartner updatedPartner) {
        IntegrationPartner partner = IntegrationPartner.findById(id);
        if (partner == null) {
            throw new WebApplicationException("Integration Partner not found", Response.Status.NOT_FOUND);
        }
        partner.partnerCode = updatedPartner.partnerCode;
        partner.name = updatedPartner.name;
        partner.type = updatedPartner.type;
        return partner;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        IntegrationPartner partner = IntegrationPartner.findById(id);
        if (partner == null) {
            throw new WebApplicationException("Integration Partner not found", Response.Status.NOT_FOUND);
        }
        partner.delete();
        return Response.noContent().build();
    }
}
