package com.smartcampus.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiMetadata(@Context UriInfo uriInfo) {
        String baseUri = uriInfo.getBaseUri().toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("api_version", "v1");
        metadata.put("admin_contact", "admin@smartcampus.westminster.ac.uk");
        metadata.put("description", "Smart Campus Sensor & Room Management API");
        
        Map<String, String> links = new HashMap<>();
        links.put("rooms", baseUri + "rooms");
        links.put("sensors", baseUri + "sensors");
        metadata.put("_links", links); // HATEOAS standard representation

        return Response.ok(metadata).build();
    }
}
