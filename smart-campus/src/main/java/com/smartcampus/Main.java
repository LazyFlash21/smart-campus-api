package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) throws IOException {

        // Use package scanning — Jersey finds ALL @Path, @Provider classes automatically
        final ResourceConfig rc = new ResourceConfig()
                .packages("com.smartcampus")
                .register(JacksonFeature.class);

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);


        LOGGER.info("  Smart Campus API started successfully!");
        LOGGER.info("  Rooms   : http://localhost:8080/api/v1/rooms");
        LOGGER.info("  Sensors : http://localhost:8080/api/v1/sensors");
        LOGGER.info("  Discovery: http://localhost:8080/api/v1/discovery");
        LOGGER.info("  Press ENTER to stop the server...");


        System.in.read();
        server.shutdownNow();
    }
}
