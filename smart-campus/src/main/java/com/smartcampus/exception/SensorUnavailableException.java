package com.smartcampus.exception;

/**
 * Thrown when a reading POST is attempted on a Sensor that is in MAINTENANCE status.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
