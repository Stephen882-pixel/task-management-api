package org.stephen.taskmanagement.entity;


import jakarta.persistence.*;
import lombok.*;
import org.stephen.taskmanagement.enums.ConflictResolutionStrategy;
import org.stephen.taskmanagement.enums.SyncStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events", uniqueConstraints = {
        @UniqueConstraint(columnNames = "event_id", name = "uk_event_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    private Task task;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "calendar_id", nullable = false)
    private String calendarId;

    @Column(name = "event_title")
    private String eventTitle;

    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "event_start_time")
    private LocalDateTime eventStartTime;

    @Column(name = "event_end_time")
    private LocalDateTime eventEndTime;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "task_last_modified_at")
    private LocalDateTime taskLastModifiedAt;

    @Column(name = "calendar_last_modified_at")
    private LocalDateTime calendarLastModifiedAt;

    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus  syncStatus;

    @Column(name = "conflict_detected")
    private Boolean conflictDetected;

    @Column(name = "conflict_resolution_strategy")
    @Enumerated(EnumType.STRING)
    private ConflictResolutionStrategy conflictResolutionStrategy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        syncStatus = SyncStatus.IN_SYNC;
        conflictDetected = false;
        conflictResolutionStrategy = ConflictResolutionStrategy.TASK_WINS;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isModifiedSinceSync(){
        return taskLastModifiedAt != null &&  taskLastModifiedAt.isAfter(lastSyncedAt);
    }

    public boolean isCalendarModifiedSinceSync(){
        return calendarLastModifiedAt != null && calendarLastModifiedAt.isAfter(lastSyncedAt);
    }
    public boolean hasConflict(){
        return isModifiedSinceSync() && isCalendarModifiedSinceSync();
    }
}
