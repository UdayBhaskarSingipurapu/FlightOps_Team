package com.project.flightOps.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String resourceName, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resourceName, field, value));
    }
}
