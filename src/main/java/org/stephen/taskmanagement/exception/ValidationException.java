package org.stephen.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApplicationException{
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}

