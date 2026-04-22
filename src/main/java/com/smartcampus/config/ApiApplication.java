package com.smartcampus.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/v1")
public class ApiApplication extends ResourceConfig {
    public ApiApplication() {
        // Register endpoints / resources
        packages("com.smartcampus.resources", "com.smartcampus.exceptions", "com.smartcampus.filters");
    }
}
