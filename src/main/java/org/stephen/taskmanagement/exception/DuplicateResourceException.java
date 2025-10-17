package org.stephen.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends  ApplicationException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue),
                HttpStatus.CONFLICT);
    }
}
