package org.stephen.taskmanagement.exception;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private HttpStatus status;
    private int statusCode;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> fieldErrors;

}
