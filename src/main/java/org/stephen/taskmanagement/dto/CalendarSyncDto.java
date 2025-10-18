package org.stephen.taskmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.stephen.taskmanagement.enums.ConflictResolutionStrategy;
import org.stephen.taskmanagement.enums.SyncDirection;
import org.stephen.taskmanagement.enums.SyncStatus;

import java.time.LocalDateTime;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncEnabledResponse{
        private Long taskId;
        private String eventId;
        private String calendarId;
        private LocalDateTime syncedAt;
        private SyncStatus syncStatus;
        private Boolean conflictDetected;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder

    public static class ManualSyncRequest{
        @NotNull(message = "Task ID is required")
        private Long taskId;

        private SyncDirection syncDirection;

        @NotNull(message = "Resolution strategy is required")
        private ConflictResolutionStrategy resolutionStrategy;
    }


}
