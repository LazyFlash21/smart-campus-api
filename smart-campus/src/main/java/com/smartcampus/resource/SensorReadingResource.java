package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }


    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getReadings(sensorId);
        return Response.ok(readings).build();
    }


    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        // cannot post a reading to a sensor under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE and cannot accept new readings."
            );
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Reading body must not be null."))
                    .build();
        }

        // Auto-generate an ID if not supplied
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId("READ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        // Auto-set timestamp if not supplied
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        store.addReading(sensorId, reading);

        // update the parent sensor's currentValue for data consistency
        sensor.setCurrentValue(reading.getValue());
        store.saveSensor(sensor);

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
