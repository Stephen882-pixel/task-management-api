package org.stephen.taskmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class createTaskRequestDto {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @FutureOrPresent(message = "Due date must be in the future or present")
    private LocalDateTime dueDate;

    @NotNull(message = "Status is required")
    private String status;

    @NotEmpty(message = "At least one tag is required")
    private Set<String> tagNames;
}
