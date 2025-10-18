package org.stephen.taskmanagement.service;


import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stephen.taskmanagement.config.CalendarSyncProperties;
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
import org.stephen.taskmanagement.mappers.CalendarMapper;
import org.stephen.taskmanagement.repository.CalendarEventRepository;
import org.stephen.taskmanagement.repository.SyncHistoryRepository;
import org.stephen.taskmanagement.repository.TaskRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CalendarSyncService {
    private final Calendar googleCalendar;
    private final CalendarEventRepository calendarEventRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final TaskRepository taskRepository;
    private final CalendarMapper calendarMapper;
    private final CalendarSyncProperties syncProperties;
    private final ConflictResolutionService conflictResolutionService;


    public CalendarSyncDto.SyncEnabledResponse enableSync(CalendarSyncDto.EnableSyncRequest request){
        log.info("Enabling calendar sync for task: {}", request.getTaskId());

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(request.getTaskId())));


        if (task.getCalendarEvent() != null) {
            throw new InvalidOperationException("Task is already synced with calendar");
        }

        try {
            Event googleEvent = createGoogleCalendarEvent(task);
            String calendarId = request.getCalendarId() != null
                    ? request.getCalendarId()
                    : syncProperties.getPrimaryCalendarId();

            Event createdEvent = googleCalendar.events()
                    .insert(calendarId, googleEvent)
                    .execute();

            CalendarEvent calendarEvent = CalendarEvent.builder()
                    .task(task)
                    .eventId(createdEvent.getId())
                    .calendarId(calendarId)
                    .eventTitle(createdEvent.getSummary())
                    .eventDescription(createdEvent.getDescription())
                    .eventStartTime(fromGoogleDateTime(createdEvent.getStart()))
                    .eventEndTime(fromGoogleDateTime(createdEvent.getEnd()))
                    .syncStatus(SyncStatus.IN_SYNC)
                    .conflictDetected(false)
                    .conflictResolutionStrategy(request.getConflictResolutionStrategy())
                    .taskLastModifiedAt(task.getUpdatedAt())
                    .calendarLastModifiedAt(LocalDateTime.now())
                    .lastSyncedAt(LocalDateTime.now())
                    .build();

            task.setCalendarEvent(calendarEvent);
            task.setCalendarSyncEnabled(true);
            task.setCalendarSyncedAt(LocalDateTime.now());

            CalendarEvent savedEvent = calendarEventRepository.save(calendarEvent);
            taskRepository.save(task);

            logSyncHistory(savedEvent, SyncType.INITIAL_SYNC,
                    SyncDirection.TASK_TO_CALENDAR, SyncStatus.IN_SYNC, null);

            log.info("Calendar sync enabled successfully for task: {} with event: {}",
                    task.getId(), createdEvent.getId());

            return CalendarSyncDto.SyncEnabledResponse.builder()
                    .taskId(task.getId())
                    .eventId(createdEvent.getId())
                    .calendarId(calendarId)
                    .syncedAt(LocalDateTime.now())
                    .syncStatus(SyncStatus.IN_SYNC)
                    .conflictDetected(false)
                    .message("Calendar sync enabled successfully")
                    .build();

        } catch (IOException e){
            log.error("Failed to create Google Calendar event for task: {}", task.getId(), e);
            throw new InvalidOperationException("Failed to sync with Google Calendar: " + e.getMessage());
        }

    }

    public CalendarSyncDto.SyncResponse syncTaskToCalendar(Long taskId){
        log.info("Syncing task changes to calendar: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(taskId)));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if (calendarEvent == null || !task.getCalendarSyncEnabled()) {
            throw new InvalidOperationException("Task is not synced with calendar");
        }

        try{
            Event googleEvent = googleCalendar.events()
                    .get(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                    .execute();
            Event updatedEvent = updateGoogleCalendarEvent(googleEvent, task);
            googleEvent = googleCalendar.events()
                    .update(calendarEvent.getCalendarId(), calendarEvent.getEventId(), updatedEvent)
                    .execute();

            // Update sync metadata
            calendarEvent.setEventTitle(updatedEvent.getSummary());
            calendarEvent.setEventDescription(updatedEvent.getDescription());
            calendarEvent.setEventStartTime(fromGoogleDateTime(updatedEvent.getStart()));
            calendarEvent.setEventEndTime(fromGoogleDateTime(updatedEvent.getEnd()));
            calendarEvent.setCalendarLastModifiedAt(LocalDateTime.now());
            calendarEvent.setLastSyncedAt(LocalDateTime.now());
            calendarEvent.setSyncStatus(SyncStatus.IN_SYNC);
            calendarEvent.setConflictDetected(false);

            calendarEventRepository.save(calendarEvent);
            task.setCalendarSyncedAt(LocalDateTime.now());
            taskRepository.save(task);

            Map<String, Object> changes = Map.of(
                    "title", task.getTitle(),
                    "status", task.getStatus(),
                    "dueDate", task.getDueDate()
            );

            logSyncHistory(calendarEvent, SyncType.AUTOMATIC,
                    SyncDirection.TASK_TO_CALENDAR, SyncStatus.IN_SYNC,
                    changes.toString());

            log.info("Task successfully synced to calendar: {}", taskId);

            return CalendarSyncDto.SyncResponse.builder()
                    .taskId(taskId)
                    .eventId(calendarEvent.getEventId())
                    .syncStatus(SyncStatus.IN_SYNC)
                    .conflictResolved(false)
                    .changesApplied(changes)
                    .syncedAt(LocalDateTime.now())
                    .message("Task synced to calendar successfully")
                    .build();
        } catch (IOException e){
            log.error("Failed to sync task to calendar: {}", taskId, e);
            calendarEvent.setSyncStatus(SyncStatus.SYNC_FAILED);
            calendarEventRepository.save(calendarEvent);
            throw new InvalidOperationException("Failed to sync with Google Calendar: " + e.getMessage());
        }

    }

    public CalendarSyncDto.SyncResponse syncCalendarToTask(Long taskId){
        log.info("Syncing calendar changes to task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(taskId)));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if (calendarEvent == null || !task.getCalendarSyncEnabled()) {
            throw new InvalidOperationException("Task is not synced with calendar");
        }

        try{
            Event googleEvent = googleCalendar.events()
                    .get(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                    .execute();

            // Detect conflicts
            boolean hasConflict = detectConflict(task, calendarEvent, googleEvent);

            if (hasConflict) {
                log.warn("Conflict detected between task and calendar for task: {}", taskId);
                calendarEvent.setConflictDetected(true);
                calendarEvent.setSyncStatus(SyncStatus.CONFLICT);
            } else {
                // Apply calendar changes to task
                applyCalendarChangesToTask(task, googleEvent);
                calendarEvent.setSyncStatus(SyncStatus.IN_SYNC);
                calendarEvent.setConflictDetected(false);
            }

            // Update sync metadata
            calendarEvent.setCalendarLastModifiedAt(fromGoogleDateTime(googleEvent.getUpdated()));
            calendarEvent.setLastSyncedAt(LocalDateTime.now());
            calendarEvent.setTaskLastModifiedAt(task.getUpdatedAt());

            calendarEventRepository.save(calendarEvent);
            taskRepository.save(task);

            Map<String, Object> changes = Map.of(
                    "taskStatus", task.getStatus(),
                    "taskDueDate", task.getDueDate(),
                    "conflictDetected", hasConflict
            );

            logSyncHistory(calendarEvent, SyncType.AUTOMATIC,
                    SyncDirection.CALENDAR_TO_TASK,
                    hasConflict ? SyncStatus.CONFLICT : SyncStatus.IN_SYNC,
                    changes.toString());

            log.info("Calendar changes synced to task: {}", taskId);

            return CalendarSyncDto.SyncResponse.builder()
                    .taskId(taskId)
                    .eventId(calendarEvent.getEventId())
                    .syncStatus(calendarEvent.getSyncStatus())
                    .conflictResolved(!hasConflict)
                    .changesApplied(changes)
                    .syncedAt(LocalDateTime.now())
                    .message(hasConflict ? "Conflict detected - manual resolution required" : "Calendar synced to task successfully")
                    .build();
        } catch (IOException e){
            log.error("Failed to sync calendar to task: {}", taskId, e);
            calendarEvent.setSyncStatus(SyncStatus.SYNC_FAILED);
            calendarEventRepository.save(calendarEvent);
            throw new InvalidOperationException("Failed to fetch from Google Calendar: " + e.getMessage());
        }
    }

    public void deleteTaskAndEvent(Long taskId){
        log.info("Deleting task and associated calendar event: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(taskId)));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if(calendarEvent != null && task.getCalendarSyncEnabled()) {
            try{
                googleCalendar.events()
                        .delete(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                        .execute();

                log.info("Calendar event deleted: {}", calendarEvent.getEventId());
                logSyncHistory(calendarEvent,SyncType.AUTOMATIC,
                        SyncDirection.TASK_TO_CALENDAR, SyncStatus.IN_SYNC,
                        "Event deleted from calendar");
            } catch (IOException e){
                log.error("Failed to delete calendar event: {}", calendarEvent.getEventId(), e);
            }
        }
        taskRepository.delete(task);
        log.info("Task deleted successfully: {}", taskId);
    }

    @Transactional(readOnly = true)
    public CalendarSyncDto.SyncStatusResponse getSyncStatus(Long taskId){
        log.info("Getting sync status for task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(taskId)));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if (calendarEvent == null) {
            throw new InvalidOperationException("Task is not synced with calendar");
        }

        return CalendarSyncDto.SyncStatusResponse.builder()
                .taskId(taskId)
                .eventId(calendarEvent.getEventId())
                .syncStatus(calendarEvent.getSyncStatus())
                .conflictDetected(calendarEvent.getConflictDetected())
                .conflictResolutionStrategy(calendarEvent.getConflictResolutionStrategy())
                .taskLastModified(calendarEvent.getTaskLastModifiedAt())
                .calendarLastModified(calendarEvent.getCalendarLastModifiedAt())
                .lastSyncedAt(calendarEvent.getLastSyncedAt())
                .message("Sync status retrieved successfully")
                .build();
    }

    public CalendarSyncDto.SyncDisabledResponse disableSync(CalendarSyncDto.DisableSyncRequest request){
        log.info("Disabling calendar sync for task: {}", request.getTaskId());

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(request.getTaskId())));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if (calendarEvent == null) {
            throw new InvalidOperationException("Task is not synced with calendar");
        }

        String eventId = calendarEvent.getEventId();
        Boolean deleted = false;

        if(request.getDeleteCalendarEvent()){
            try{
                googleCalendar.events()
                        .delete(calendarEvent.getCalendarId(), calendarEvent.getEventId())
                        .execute();
                deleted = true;
                log.info("Calendar event deleted: {}", eventId);
            } catch (IOException e){
                log.error("Failed to delete calendar event: {}", eventId, e);
            }
        }
        task.setCalendarEvent(null);
        task.setCalendarSyncEnabled(false);
        task.setCalendarSyncedAt(null);
        taskRepository.save(task);

        calendarEventRepository.delete(calendarEvent);

        log.info("Calendar sync disabled for task: {}", request.getTaskId());

        return CalendarSyncDto.SyncDisabledResponse.builder()
                .taskId(request.getTaskId())
                .eventId(eventId)
                .calendarEventDeleted(deleted)
                .disabledAt(LocalDateTime.now())
                .message("Calendar sync disabled successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public List<CalendarSyncDto.SyncHistoryResponse> getSyncHistory(Long taskId){
        log.info("Getting sync history for task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", String.valueOf(taskId)));

        CalendarEvent calendarEvent = task.getCalendarEvent();
        if (calendarEvent == null) {
            return Collections.emptyList();
        }

        return syncHistoryRepository.findByCalendarEventOrderBySyncedAtDesc(calendarEvent)
                .stream()
                .map(calendarMapper::toSyncHistoryResponse)
                .collect(Collectors.toList());
    }

    private Event createGoogleCalendarEvent(Task task){
        Event event = new Event()
                .setSummary(task.getTitle())
                .setDescription(task.getDescription());

        if (task.getDueDate() != null) {
            com.google.api.client.util.DateTime startDateTime =
                    toGoogleDateTime(task.getDueDate());
            event.setStart(new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("UTC"));

            // Set end time 1 hour after start
            LocalDateTime endTime = task.getDueDate().plusHours(1);
            com.google.api.client.util.DateTime endDateTime =
                    toGoogleDateTime(endTime);
            event.setEnd(new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("UTC"));
        }
        return event;
    }

    private Event updateGoogleCalendarEvent(Event event, Task task){
        event.setSummary(task.getTitle());
        event.setDescription(task.getDescription());

        if(task.getDueDate() != null) {
            com.google.api.client.util.DateTime startDateTime =
                    toGoogleDateTime(task.getDueDate());
            event.setStart(new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("UTC"));

            LocalDateTime endTime = task.getDueDate().plusHours(1);
            com.google.api.client.util.DateTime endDateTime =
                    toGoogleDateTime(endTime);
            event.setEnd(new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("UTC"));
        }
        if(task.getStatus() == TaskStatus.COMPLETED){
            event.setStatus("cancelled");
        } else {
            event.setStatus("confirmed");
        }
        return event;
    }

    private boolean detectConflict(Task task, CalendarEvent calendarEvent, Event googleEvent) {
        boolean taskModified = calendarEvent.isModifiedSinceSync();
        boolean calendarModified = calendarEvent.isCalendarModifiedSinceSync();

        if (!taskModified || !calendarModified) {
            return false;
        }


        String calendarTitle = googleEvent.getSummary();
        LocalDateTime calendarDueDate = fromGoogleDateTime(googleEvent.getStart());

        boolean titleChanged = !task.getTitle().equals(calendarTitle);
        boolean dueDateChanged = !task.getDueDate().equals(calendarDueDate);

        return titleChanged || dueDateChanged;
    }

    private void applyCalendarChangesToTask(Task task, Event googleEvent) {
        task.setTitle(googleEvent.getSummary());
        task.setDescription(googleEvent.getDescription());

        if (googleEvent.getStart() != null) {
            task.setDueDate(fromGoogleDateTime(googleEvent.getStart()));
        }


        if ("cancelled".equals(googleEvent.getStatus())) {
            task.setStatus(TaskStatus.COMPLETED);
        }
    }

    private com.google.api.client.util.DateTime toGoogleDateTime(LocalDateTime localDateTime) {
        return new com.google.api.client.util.DateTime(
                java.util.Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    private LocalDateTime fromGoogleDateTime(EventDateTime eventDateTime) {
        if (eventDateTime == null || eventDateTime.getDateTime() == null) {
            return null;
        }
        return eventDateTime.getDateTime().toCalendar().toZonedDateTime()
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private void logSyncHistory(CalendarEvent calendarEvent, SyncType syncType,
                                SyncDirection syncDirection, SyncStatus syncStatus,
                                String changesApplied) {
        SyncHistory history = SyncHistory.builder()
                .calendarEvent(calendarEvent)
                .syncType(syncType)
                .syncDirection(syncDirection)
                .syncStatus(syncStatus)
                .changesApplied(changesApplied)
                .build();

        syncHistoryRepository.save(history);
    }
}
