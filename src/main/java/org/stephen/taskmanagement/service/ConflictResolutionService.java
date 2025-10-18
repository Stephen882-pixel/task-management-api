package org.stephen.taskmanagement.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stephen.taskmanagement.dto.CalendarSyncDto;
import org.stephen.taskmanagement.entity.CalendarEvent;
import org.stephen.taskmanagement.entity.SyncHistory;
import org.stephen.taskmanagement.entity.Task;
import org.stephen.taskmanagement.enums.SyncDirection;
import org.stephen.taskmanagement.enums.SyncStatus;
import org.stephen.taskmanagement.enums.SyncType;
import org.stephen.taskmanagement.enums.TaskStatus;
import org.stephen.taskmanagement.exception.InvalidOperationException;
import org.stephen.taskmanagement.exception.ResourceNotFoundException;
import org.stephen.taskmanagement.repository.CalendarEventRepository;
import org.stephen.taskmanagement.repository.SyncHistoryRepository;
import org.stephen.taskmanagement.repository.TaskRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConflictResolutionService {
    private final Calendar googleCalendar;
    private final TaskRepository taskRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final SyncHistoryRepository syncHistoryRepository;

    public CalendarSyncDto.ConflictResolutionResponse resolveConflict(
            CalendarSyncDto.ConflictResolutionRequest request) {
        log.info("Resolving conflict for task: {} using strategy: {}",
                request.getTaskId(), request.getStrategy());

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(request.getTaskId())));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if (calendarEvent == null || !calendarEvent.getConflictDetected()) {
            throw new InvalidOperationException("No conflict detected for this task");
        }

        Map<String, Object> resolvedData = new HashMap<>();

        try {
            switch (request.getStrategy()) {
                case TASK_WINS -> resolveWithTaskWins(task, calendarEvent, resolvedData);
                case CALENDAR_WINS -> resolveWithCalendarWins(task, calendarEvent, resolvedData);
                case MANUAL -> resolveManually(task, calendarEvent, request, resolvedData);
                case MERGE -> resolveWithMerge(task, calendarEvent, resolvedData);
            }

            // Update sync metadata
            calendarEvent.setConflictDetected(false);
            calendarEvent.setSyncStatus(SyncStatus.IN_SYNC);
            calendarEvent.setConflictResolutionStrategy(request.getStrategy());
            calendarEvent.setLastSyncedAt(LocalDateTime.now());
            calendarEvent.setTaskLastModifiedAt(task.getUpdatedAt());

            calendarEventRepository.save(calendarEvent);
            taskRepository.save(task);

            // Log resolution
            SyncHistory history = SyncHistory.builder()
                    .calendarEvent(calendarEvent)
                    .syncType(SyncType.MANUAL)
                    .syncDirection(SyncDirection.BIDIRECTIONAL)
                    .syncStatus(SyncStatus.IN_SYNC)
                    .changesApplied(resolvedData.toString())
                    .build();
            syncHistoryRepository.save(history);

            log.info("Conflict resolved for task: {} using strategy: {}",
                    request.getTaskId(), request.getStrategy());

            return CalendarSyncDto.ConflictResolutionResponse.builder()
                    .taskId(task.getId())
                    .eventId(calendarEvent.getEventId())
                    .appliedStrategy(request.getStrategy())
                    .resolvedData(resolvedData)
                    .resolvedAt(LocalDateTime.now())
                    .message("Conflict resolved successfully using " + request.getStrategy() + " strategy")
                    .build();

        } catch (IOException e) {
            log.error("Failed to resolve conflict for task: {}", task.getId(), e);
            throw new InvalidOperationException("Failed to resolve conflict: " + e.getMessage());
        }
    }

    private void resolveWithTaskWins(Task task, CalendarEvent calendarEvent,
                                     Map<String, Object> resolvedData) throws IOException {
        log.debug("Applying TASK_WINS strategy for task: {}", task.getId());

        Event googleEvent = googleCalendar.events()
                .get(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                .execute();

        // Update event with task values
        googleEvent.setSummary(task.getTitle());
        googleEvent.setDescription(task.getDescription());

        if (task.getDueDate() != null) {
            com.google.api.client.util.DateTime dateTime =
                    new com.google.api.client.util.DateTime(
                            java.util.Date.from(task.getDueDate().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    );
            googleEvent.getStart().setDateTime(dateTime);
            googleEvent.getEnd().setDateTime(dateTime);
        }

        googleCalendar.events()
                .update(calendarEvent.getCalendarId(), calendarEvent.getEventId(), googleEvent)
                .execute();

        resolvedData.put("strategy", "TASK_WINS");
        resolvedData.put("taskTitle", task.getTitle());
        resolvedData.put("taskStatus", task.getStatus());
        resolvedData.put("taskDueDate", task.getDueDate());
        resolvedData.put("calendarUpdated", true);
    }

    private void resolveWithCalendarWins(Task task, CalendarEvent calendarEvent,
                                         Map<String, Object> resolvedData) throws IOException {
        log.debug("Applying CALENDAR_WINS strategy for task: {}", task.getId());

        Event googleEvent = googleCalendar.events()
                .get(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                .execute();


        task.setTitle(googleEvent.getSummary());
        task.setDescription(googleEvent.getDescription());

        if (googleEvent.getStart() != null && googleEvent.getStart().getDateTime() != null) {
            com.google.api.client.util.DateTime startDateTime = googleEvent.getStart().getDateTime();


            LocalDateTime dueDate = Instant.ofEpochMilli(startDateTime.getValue())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            task.setDueDate(dueDate);
        }


        resolvedData.put("strategy", "CALENDAR_WINS");
        resolvedData.put("taskTitle", task.getTitle());
        resolvedData.put("taskDueDate", task.getDueDate());
        resolvedData.put("taskUpdated", true);
    }

    private void resolveWithMerge(Task task, CalendarEvent calendarEvent,
                                  Map<String, Object> resolvedData) throws IOException {
        log.debug("Applying MERGE strategy for task: {}", task.getId());

        Event googleEvent = googleCalendar.events()
                .get(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                .execute();

        Map<String, String> mergedChanges = new HashMap<>();

        // Compare and merge fields
        if (!task.getTitle().equals(googleEvent.getSummary())) {
            // Prefer the more recently modified version
            task.setTitle(googleEvent.getSummary());
            mergedChanges.put("title", "merged to: " + googleEvent.getSummary());
        }

        if (googleEvent.getDescription() != null &&
                !googleEvent.getDescription().equals(task.getDescription())) {
            task.setDescription(googleEvent.getDescription());
            mergedChanges.put("description", "merged to: " + googleEvent.getDescription());
        }

        if (googleEvent.getStart() != null && googleEvent.getStart().getDateTime() != null && task.getDueDate() != null) {
            com.google.api.client.util.DateTime startDateTime = googleEvent.getStart().getDateTime();


            LocalDateTime calendarDueDate = Instant.ofEpochMilli(startDateTime.getValue())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();


            if (!task.getDueDate().equals(calendarDueDate)) {
                task.setDueDate(calendarDueDate);
                mergedChanges.put("dueDate", "merged to: " + calendarDueDate);
            }
        }


        resolvedData.put("strategy", "MERGE");
        resolvedData.put("mergedFields", mergedChanges);
        resolvedData.put("taskUpdated", true);
    }

    private void resolveManually(Task task, CalendarEvent calendarEvent,
                                 CalendarSyncDto.ConflictResolutionRequest request,
                                 Map<String, Object> resolvedData) {
        log.debug("Applying MANUAL strategy for task: {}", task.getId());

        if (request.getCustomResolution() == null || request.getCustomResolution().isEmpty()) {
            throw new InvalidOperationException("Custom resolution data is required for MANUAL strategy");
        }

        request.getCustomResolution().forEach((key, value) -> {
            switch (key.toLowerCase()) {
                case "title" -> task.setTitle((String) value);
                case "description" -> task.setDescription((String) value);
                case "status" -> task.setStatus(TaskStatus.valueOf((String) value));
                case "duedate" -> task.setDueDate((LocalDateTime) value);
            }
        });

        resolvedData.put("strategy", "MANUAL");
        resolvedData.put("customChanges", request.getCustomResolution());
        resolvedData.put("taskUpdated", true);
    }

    public Map<String, Object> analyzeConflict(Long taskId) throws IOException {
        log.info("Analyzing conflict for task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(taskId)));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if (calendarEvent == null) {
            throw new InvalidOperationException("Task is not synced with calendar");
        }

        Map<String, Object> analysis = new HashMap<>();

        Event googleEvent = googleCalendar.events()
                .get(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                .execute();

        // Compare fields
        Map<String, Map<String, Object>> fieldComparison = new HashMap<>();

        // Title
        Map<String, Object> titleComparison = new HashMap<>();
        titleComparison.put("taskValue", task.getTitle());
        titleComparison.put("calendarValue", googleEvent.getSummary());
        titleComparison.put("conflict", !task.getTitle().equals(googleEvent.getSummary()));
        fieldComparison.put("title", titleComparison);

        Map<String, Object> dueDateComparison = new HashMap<>();

        com.google.api.client.util.DateTime startDateTime = googleEvent.getStart().getDateTime();
        if (startDateTime == null) {
            startDateTime = googleEvent.getStart().getDate();
        }


        LocalDateTime calendarDueDate = Instant.ofEpochMilli(startDateTime.getValue())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        dueDateComparison.put("taskValue", task.getDueDate());
        dueDateComparison.put("calendarValue", calendarDueDate);
        dueDateComparison.put("conflict", !task.getDueDate().equals(calendarDueDate));
        fieldComparison.put("dueDate", dueDateComparison);

        Map<String, Object> descriptionComparison = new HashMap<>();
        descriptionComparison.put("taskValue", task.getDescription());
        descriptionComparison.put("calendarValue", googleEvent.getDescription());
        descriptionComparison.put("conflict", !Objects.equals(task.getDescription(), googleEvent.getDescription()));
        fieldComparison.put("description", descriptionComparison);

        analysis.put("taskId", taskId);
        analysis.put("eventId", calendarEvent.getEventId());
        analysis.put("fieldComparison", fieldComparison);
        analysis.put("taskLastModified", calendarEvent.getTaskLastModifiedAt());
        analysis.put("calendarLastModified", calendarEvent.getCalendarLastModifiedAt());
        analysis.put("lastSynced", calendarEvent.getLastSyncedAt());

        return analysis;

    }
}
