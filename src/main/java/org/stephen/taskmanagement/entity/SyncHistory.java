package org.stephen.taskmanagement.entity;


import jakarta.persistence.*;
import lombok.*;
import org.stephen.taskmanagement.enums.SyncDirection;
import org.stephen.taskmanagement.enums.SyncStatus;
import org.stephen.taskmanagement.enums.SyncType;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_event_id", nullable = false)
    private CalendarEvent calendarEvent;

    @Column(name = "sync_type")
    @Enumerated(EnumType.STRING)
    private SyncType syncType;

    @Column(name = "sync_direction")
    @Enumerated(EnumType.STRING)
    private SyncDirection syncDirection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;

    @Column(name = "changes_applied", columnDefinition = "TEXT")
    private String changesApplied; // JSON serialized changes

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @PrePersist
    protected void onCreate() {
        syncedAt = LocalDateTime.now();
    }
}
