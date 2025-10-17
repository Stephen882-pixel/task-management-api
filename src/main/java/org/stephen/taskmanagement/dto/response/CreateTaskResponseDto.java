package org.stephen.taskmanagement.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<TasksListResponseDto> tags;
}
