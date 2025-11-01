package com.example.devicemanagement.handlers.exceptions.model;

public class DuplicateResourceException extends RuntimeException {

    private static final String MESSAGE = " already exists!";

    public DuplicateResourceException(String resource) {
        super(resource + MESSAGE);
    }
}
