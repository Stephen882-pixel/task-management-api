package org.stephen.taskmanagement.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class tasksListResponseDto {
    private Long id;
    private String title;
    private String status;
    private LocalDateTime dueDate;
    private Integer tagCount;
}
