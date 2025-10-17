package org.stephen.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException{

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),
                HttpStatus.NOT_FOUND);
    }
}
