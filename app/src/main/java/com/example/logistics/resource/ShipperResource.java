package com.example.logistics.resource;

import com.example.logistics.model.Shipper;
import com.example.logistics.model.Site;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/shippers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShipperResource {

    @GET
    public List<Shipper> listAll() {
        return Shipper.listAll();
    }

    @GET
    @Path("/{id}")
    public Shipper getById(@PathParam("id") Long id) {
        Shipper shipper = Shipper.findById(id);
        if (shipper == null) {
            throw new WebApplicationException("Shipper not found", Response.Status.NOT_FOUND);
        }
        return shipper;
    }

    @GET
    @Path("/code/{shipperCode}")
    public Shipper getByCode(@PathParam("shipperCode") String shipperCode) {
        Shipper shipper = Shipper.findByShipperCode(shipperCode);
        if (shipper == null) {
            throw new WebApplicationException("Shipper not found", Response.Status.NOT_FOUND);
        }
        return shipper;
    }

    @POST
    @Transactional
    public Response create(Shipper shipper) {
        shipper.persist();
        return Response.status(Response.Status.CREATED).entity(shipper).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Shipper update(@PathParam("id") Long id, Shipper updatedShipper) {
        Shipper shipper = Shipper.findById(id);
        if (shipper == null) {
            throw new WebApplicationException("Shipper not found", Response.Status.NOT_FOUND);
        }
        shipper.shipperCode = updatedShipper.shipperCode;
        shipper.name = updatedShipper.name;
        shipper.contactName = updatedShipper.contactName;
        shipper.contactEmail = updatedShipper.contactEmail;
        shipper.contactPhone = updatedShipper.contactPhone;
        return shipper;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Shipper shipper = Shipper.findById(id);
        if (shipper == null) {
            throw new WebApplicationException("Shipper not found", Response.Status.NOT_FOUND);
        }
        shipper.delete();
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/sites")
    public List<Site> getSites(@PathParam("id") Long id) {
        Shipper shipper = Shipper.findById(id);
        if (shipper == null) {
            throw new WebApplicationException("Shipper not found", Response.Status.NOT_FOUND);
        }
        return Site.findByShipper(id);
    }
}
