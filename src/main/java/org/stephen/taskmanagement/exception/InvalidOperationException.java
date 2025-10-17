package org.stephen.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends ApplicationException{
    public InvalidOperationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
