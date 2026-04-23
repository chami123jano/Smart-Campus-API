package com.smartcampus.resources;

import com.smartcampus.db.InMemoryDatabase;
import com.smartcampus.exceptions.RoomNotEmptyException;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Response getAllRooms() {
        return Response.ok(InMemoryDatabase.rooms.values()).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "The request payload cannot be empty."))
                           .build();
        }
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        } else if (InMemoryDatabase.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("error", "Room ID '" + room.getId() + "' already exists."))
                           .build();
        }
        InMemoryDatabase.rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = InMemoryDatabase.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Room not found with ID: " + roomId))
                           .build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = InMemoryDatabase.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Room not found with ID: " + roomId))
                           .build();
        }

        // Check if there are active sensors assigned to this room to prevent data orphans
        boolean hasActiveSensors = false;
        for (Sensor sensor : InMemoryDatabase.sensors.values()) {
            if (roomId.equals(sensor.getRoomId())) {
                hasActiveSensors = true;
                break;
            }
        }

        if (hasActiveSensors) {
            throw new RoomNotEmptyException("Cannot delete room. It is currently occupied by active hardware sensors.");
        }

        InMemoryDatabase.rooms.remove(roomId);
        return Response.noContent().build();
    }

}
