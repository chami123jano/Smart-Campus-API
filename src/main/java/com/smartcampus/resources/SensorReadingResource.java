package com.smartcampus.resources;

import com.smartcampus.db.InMemoryDatabase;
import com.smartcampus.exceptions.SensorUnavailableException;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Sub-Resource locator class - No top-level @Path required
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    // Constructed via the parent SensorResource locator with context path param
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        if (!InMemoryDatabase.sensors.containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor ID '" + sensorId + "' does not exist."))
                           .build();
        }

        List<SensorReading> readings = InMemoryDatabase.sensorReadings.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "The request payload cannot be empty."))
                           .build();
        }

        Sensor parentSensor = InMemoryDatabase.sensors.get(sensorId);
        if (parentSensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor ID '" + sensorId + "' does not exist. Cannot append reading."))
                           .build();
        }

        if ("OFFLINE".equalsIgnoreCase(parentSensor.getStatus()) || "MAINTENANCE".equalsIgnoreCase(parentSensor.getStatus())) {
            throw new SensorUnavailableException("Cannot accept readings. Sensor '" + sensorId + "' is currently " + parentSensor.getStatus() + ".");
        }

        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Initialize timestamp if client omitted it
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // 1. Add historical record explicitly into the nested database map
        InMemoryDatabase.sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);

        // 2. Crucial CW requirement: Overwrite the currentValue state on the parent entity dynamically
        parentSensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED)
                       .entity(reading)
                       .build();
    }
}