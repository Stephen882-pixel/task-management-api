package org.stephen.taskmanagement.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequestDto {

    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @FutureOrPresent(message = "Due date must be in the future or present")
    private LocalDateTime dueDate;
    private String status;
    private Set<String> tagNames;
}
