package org.stephen.taskmanagement.entity;


import jakarta.persistence.*;
import lombok.*;
import org.stephen.taskmanagement.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name ="created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private CalendarEvent calendarEvent;

    @Column(name = "calendar_sync_enabled")
    private Boolean calendarSyncEnabled = true;

    @Column(name = "calendar_synced_at")
    private LocalDateTime calendarSyncedAt;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE},fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getTasks().add(this);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getTasks().remove(this);
    }

    public boolean isCalendarSynced() {
        return calendarEvent != null && calendarSyncEnabled;
    }

    public boolean hasCalendarConflict() {
        return calendarEvent != null && calendarEvent.getConflictDetected();
    }


}
