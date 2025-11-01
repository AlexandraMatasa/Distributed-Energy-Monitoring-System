package com.example.usermanagement.handlers.exceptions.model;

public class ResourceNotFoundException extends RuntimeException {

    private static final String MESSAGE = " was not found!";

    public ResourceNotFoundException(String resource) {
        super(resource + MESSAGE);
    }
}