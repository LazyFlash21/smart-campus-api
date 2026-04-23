package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;


@Path("/discovery")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> response = new HashMap<>();

        // API metadata
        response.put("apiName", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        response.put("contact", "admin@smartcampus.westminster.ac.uk");

        // HATEOAS-style links to primary resource collections
        Map<String, String> links = new HashMap<>();
        links.put("self",    "/api/v1/discovery");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("_links", links);

        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.put("server", status);

        return Response.ok(response).build();
    }

    @GET
    @Path("/test500")
    @Produces(MediaType.APPLICATION_JSON)
    public Response test500() {
        throw new RuntimeException("Deliberate test error for demonstration");
    }

}
