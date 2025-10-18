package org.stephen.taskmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.stephen.taskmanagement.enums.ConflictResolutionStrategy;

public class CalendarSyncDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnableSyncRequest {
        @NotNull(message = "Task ID is required")
        private Long taskId;

        private String calendarId;

        @NotNull(message = "Conflict resolution strategy is required")
        private ConflictResolutionStrategy conflictResolutionStrategy;
    }
}
