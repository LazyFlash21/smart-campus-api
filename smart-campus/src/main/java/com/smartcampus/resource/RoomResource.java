package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;


@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();


    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(new ArrayList<>(rooms)).build();
    }


    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room ID must not be null or blank."))
                    .build();
        }
        if (store.getRoom(room.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        // Ensure sensor list starts empty on creation
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }
        store.saveRoom(room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }


    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }


    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room not found: " + roomId))
                    .build();
        }
        // cannot delete a room that still has sensors assigned
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted: it still has " +
                    room.getSensorIds().size() + " sensor(s) assigned. " +
                    "Please decommission or reassign all sensors first."
            );
        }
        store.deleteRoom(roomId);
        Map<String, String> body = new HashMap<>();
        body.put("message", "Room '" + roomId + "' has been successfully decommissioned.");
        return Response.ok(body).build();
    }

    // Helper: build a consistent error JSON body
    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
