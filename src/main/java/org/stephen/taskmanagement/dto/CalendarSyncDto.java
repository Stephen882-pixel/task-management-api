package org.stephen.taskmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.stephen.taskmanagement.enums.ConflictResolutionStrategy;
import org.stephen.taskmanagement.enums.SyncDirection;
import org.stephen.taskmanagement.enums.SyncStatus;

import java.time.LocalDateTime;
import java.util.Map;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncResponse{
        private Long taskId;
        private String eventId;
        private SyncStatus syncStatus;
        private Boolean conflictResolved;
        private Map<String, Object> changesApplied;
        private LocalDateTime syncedAt;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConflictResolutionRequest {
        @NotNull(message = "Task ID is required")
        private Long taskId;

        @NotNull(message = "Resolution strategy is required")
        private ConflictResolutionStrategy strategy;

        private Map<String, Object> customResolution;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConflictResolutionResponse {
        private Long taskId;
        private String eventId;
        private ConflictResolutionStrategy appliedStrategy;
        private Map<String, Object> resolvedData;
        private LocalDateTime resolvedAt;
        private String message;
    }
}
