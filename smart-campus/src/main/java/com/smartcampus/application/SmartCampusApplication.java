package com.smartcampus.application;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // ResourceConfig in Main.java handles all registration via package scanning
}
