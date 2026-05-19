package com.example.logistics.resource;

import com.example.logistics.model.Shipper;
import com.example.logistics.model.Site;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/sites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SiteResource {

    @GET
    public List<Site> listAll() {
        return Site.listAll();
    }

    @GET
    @Path("/{id}")
    public Site getById(@PathParam("id") Long id) {
        Site site = Site.findById(id);
        if (site == null) {
            throw new WebApplicationException("Site not found", Response.Status.NOT_FOUND);
        }
        return site;
    }

    @GET
    @Path("/code/{siteCode}")
    public Site getByCode(@PathParam("siteCode") String siteCode) {
        Site site = Site.findBySiteCode(siteCode);
        if (site == null) {
            throw new WebApplicationException("Site not found", Response.Status.NOT_FOUND);
        }
        return site;
    }

    @POST
    @Transactional
    public Response create(SiteCreateRequest request) {
        Shipper shipper = Shipper.findById(request.shipperId);
        if (shipper == null) {
            throw new WebApplicationException("Shipper not found", Response.Status.BAD_REQUEST);
        }

        Site site = new Site();
        site.siteCode = request.siteCode;
        site.name = request.name;
        site.address = request.address;
        site.shipper = shipper;
        site.persist();

        return Response.status(Response.Status.CREATED).entity(site).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Site update(@PathParam("id") Long id, SiteCreateRequest request) {
        Site site = Site.findById(id);
        if (site == null) {
            throw new WebApplicationException("Site not found", Response.Status.NOT_FOUND);
        }

        if (request.shipperId != null && !request.shipperId.equals(site.shipper.id)) {
            Shipper newShipper = Shipper.findById(request.shipperId);
            if (newShipper == null) {
                throw new WebApplicationException("Shipper not found", Response.Status.BAD_REQUEST);
            }
            site.shipper = newShipper;
        }

        site.siteCode = request.siteCode;
        site.name = request.name;
        site.address = request.address;

        return site;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Site site = Site.findById(id);
        if (site == null) {
            throw new WebApplicationException("Site not found", Response.Status.NOT_FOUND);
        }
        site.delete();
        return Response.noContent().build();
    }

    public static class SiteCreateRequest {
        public Long shipperId;
        public String siteCode;
        public String name;
        public String address;
    }
}
