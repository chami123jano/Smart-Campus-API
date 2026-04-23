package com.smartcampus.resources;

import com.smartcampus.db.InMemoryDatabase;
import com.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.models.Sensor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @POST
    public Response registerSensor(Sensor sensor) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "The request payload cannot be empty."))
                           .build();
        }

        if (sensor.getRoomId() == null || sensor.getRoomId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "A valid 'roomId' is required to register a sensor."))
                           .build();
        }

        if (!InMemoryDatabase.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("Room ID '" + sensor.getRoomId() + "' does not exist. Sensor cannot be linked.");
        }

        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        } else if (InMemoryDatabase.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("error", "Sensor ID '" + sensor.getId() + "' already exists. Cannot create duplicate."))
                           .build();
        }

        InMemoryDatabase.sensors.put(sensor.getId(), sensor);
        return Response.status(Response.Status.CREATED)
                       .entity(sensor)
                       .build();
    }

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        if (type == null || type.trim().isEmpty()) {
            return Response.ok(InMemoryDatabase.sensors.values()).build();
        }

        List<Sensor> filteredSensors = new ArrayList<>();
        for (Sensor sensor : InMemoryDatabase.sensors.values()) {
            if (type.equalsIgnoreCase(sensor.getType())) {
                filteredSensors.add(sensor);
            }
        }
        return Response.ok(filteredSensors).build();
    }
    // Day 3 Sub-Resource Locator to seamlessly route deep-nested historical endpoints
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updatedSensor) {
        Sensor existingSensor = InMemoryDatabase.sensors.get(sensorId);
        
        if (updatedSensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "The request payload cannot be empty."))
                           .build();
        }

        if (updatedSensor.getId() != null && !sensorId.equals(updatedSensor.getId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Sensor ID cannot be changed via PUT request. It must match the URL path ID."))
                           .build();
        }

        if (existingSensor == null) {
             return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Sensor not found")).build();
        }
        
        updatedSensor.setId(sensorId);
        updatedSensor.setCurrentValue(existingSensor.getCurrentValue()); // Preserve reading
        InMemoryDatabase.sensors.put(sensorId, updatedSensor);
        
        return Response.ok(updatedSensor).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        if (!InMemoryDatabase.sensors.containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor not found."))
                           .build();
        }
        InMemoryDatabase.sensors.remove(sensorId);
        return Response.noContent().build();
    }
}