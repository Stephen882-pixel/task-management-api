package org.stephen.taskmanagement.service;


import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stephen.taskmanagement.config.CalendarSyncProperties;
import org.stephen.taskmanagement.dto.CalendarSyncDto;
import org.stephen.taskmanagement.entity.CalendarEvent;
import org.stephen.taskmanagement.entity.Task;
import org.stephen.taskmanagement.enums.ConflictResolutionStrategy;
import org.stephen.taskmanagement.enums.SyncStatus;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Calendar Sync Service - Critical Paths")
class CalendarSyncServiceTest {

    @Mock
    private Calendar googleCalendar;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private SyncHistoryRepository syncHistoryRepository;

    @Mock
    private CalendarMapper calendarMapper;

    @Mock
    private CalendarSyncProperties syncProperties;

    @Mock
    private ConflictResolutionService conflictResolutionService;

    @InjectMocks
    private CalendarSyncService calendarSyncService;

    private Task task;
    private CalendarEvent calendarEvent;
    private Event googleEvent;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .dueDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .calendarSyncEnabled(false)
                .build();

        calendarEvent = CalendarEvent.builder()
                .id(1L)
                .task(task)
                .eventId("event123")
                .calendarId("primary")
                .eventTitle("Test Task")
                .syncStatus(SyncStatus.IN_SYNC)
                .conflictDetected(false)
                .conflictResolutionStrategy(ConflictResolutionStrategy.TASK_WINS)
                .lastSyncedAt(LocalDateTime.now())
                .taskLastModifiedAt(LocalDateTime.now())
                .calendarLastModifiedAt(LocalDateTime.now())
                .build();

        googleEvent = new Event()
                .setId("event123")
                .setSummary("Test Task")
                .setDescription("Test Description")
                .setStart(new EventDateTime().setDateTime(
                        new com.google.api.client.util.DateTime(
                                java.util.Date.from(task.getDueDate()
                                        .atZone(ZoneId.systemDefault()).toInstant()))))
                .setEnd(new EventDateTime().setDateTime(
                        new com.google.api.client.util.DateTime(
                                java.util.Date.from(task.getDueDate().plusHours(1)
                                        .atZone(ZoneId.systemDefault()).toInstant()))));
    }

    @Test
    @DisplayName("Enable sync: Should create Google Calendar event and link to task")
    void testEnableSync_Success() throws Exception {
        CalendarSyncDto.EnableSyncRequest request = CalendarSyncDto.EnableSyncRequest.builder()
                .taskId(1L)
                .calendarId("primary")
                .conflictResolutionStrategy(ConflictResolutionStrategy.TASK_WINS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Insert insertAPI = mock(Calendar.Events.Insert.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.insert(eq("primary"), any(Event.class))).thenReturn(insertAPI);
        when(insertAPI.execute()).thenReturn(googleEvent);
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        CalendarSyncDto.SyncEnabledResponse response = calendarSyncService.enableSync(request);

        assertNotNull(response);
        assertEquals(1L, response.getTaskId());
        assertEquals("event123", response.getEventId());
        assertEquals(SyncStatus.IN_SYNC, response.getSyncStatus());
        assertFalse(response.getConflictDetected());

        verify(eventsAPI).insert(eq("primary"), any(Event.class));
        verify(calendarEventRepository).save(any(CalendarEvent.class));
        verify(syncHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Enable sync: Should fail when task already synced")
    void testEnableSync_AlreadySynced() {
        task.setCalendarEvent(calendarEvent);

        CalendarSyncDto.EnableSyncRequest request = CalendarSyncDto.EnableSyncRequest.builder()
                .taskId(1L)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(InvalidOperationException.class, () -> calendarSyncService.enableSync(request));
        verify(googleCalendar, never()).events();
    }

    @Test
    @DisplayName("Enable sync: Should fail when task not found")
    void testEnableSync_TaskNotFound() {
        CalendarSyncDto.EnableSyncRequest request = CalendarSyncDto.EnableSyncRequest.builder()
                .taskId(999L)
                .build();

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> calendarSyncService.enableSync(request));
    }

    @Test
    @DisplayName("Enable sync: Should handle Google Calendar API failure")
    void testEnableSync_GoogleCalendarFailure() throws Exception {
        CalendarSyncDto.EnableSyncRequest request = CalendarSyncDto.EnableSyncRequest.builder()
                .taskId(1L)
                .calendarId("primary")
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Insert insertAPI = mock(Calendar.Events.Insert.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.insert(eq("primary"), any(Event.class))).thenReturn(insertAPI);
        when(insertAPI.execute()).thenThrow(new IOException("API error"));

        assertThrows(InvalidOperationException.class, () -> calendarSyncService.enableSync(request));
        verify(syncHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sync to calendar: Should update Google Calendar when task changes")
    void testSyncTaskToCalendar_Success() throws Exception {
        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Get getAPI = mock(Calendar.Events.Get.class);
        Calendar.Events.Update updateAPI = mock(Calendar.Events.Update.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.get(eq("primary"), eq("event123"))).thenReturn(getAPI);
        when(getAPI.execute()).thenReturn(googleEvent);
        when(eventsAPI.update(eq("primary"), eq("event123"), any(Event.class))).thenReturn(updateAPI);
        when(updateAPI.execute()).thenReturn(googleEvent);

        CalendarSyncDto.SyncResponse response = calendarSyncService.syncTaskToCalendar(1L);

        assertNotNull(response);
        assertEquals(1L, response.getTaskId());
        assertEquals(SyncStatus.IN_SYNC, response.getSyncStatus());

        verify(eventsAPI).get(eq("primary"), eq("event123"));
        verify(eventsAPI).update(eq("primary"), eq("event123"), any(Event.class));
    }

    @Test
    @DisplayName("Sync to calendar: Should fail when task not synced")
    void testSyncTaskToCalendar_NotSynced() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(InvalidOperationException.class, () -> calendarSyncService.syncTaskToCalendar(1L));
        verify(googleCalendar, never()).events();
    }

    @Test
    @DisplayName("Sync to calendar: Should mark as SYNC_FAILED on IOException")
    void testSyncTaskToCalendar_GoogleCalendarFailure() throws Exception {
        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Get getAPI = mock(Calendar.Events.Get.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.get(eq("primary"), eq("event123"))).thenReturn(getAPI);
        when(getAPI.execute()).thenThrow(new IOException("Network error"));

        assertThrows(InvalidOperationException.class, () -> calendarSyncService.syncTaskToCalendar(1L));
        assertEquals(SyncStatus.SYNC_FAILED, calendarEvent.getSyncStatus());
        verify(calendarEventRepository).save(calendarEvent);
    }


    @Test
    @DisplayName("Sync from calendar: Should apply calendar changes when no conflict")
    void testSyncCalendarToTask_NoConflict() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusHours(2);
        LocalDateTime recentTime = now.minusMinutes(30);

        calendarEvent.setLastSyncedAt(pastTime);
        calendarEvent.setTaskLastModifiedAt(pastTime.minusMinutes(5));
        calendarEvent.setCalendarLastModifiedAt(recentTime);

        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);

        Event updatedGoogleEvent = new Event()
                .setId("event123")
                .setSummary("Updated Title")
                .setDescription("Updated Description")
                .setUpdated(new com.google.api.client.util.DateTime(recentTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .setStart(new EventDateTime().setDateTime(
                        new com.google.api.client.util.DateTime(
                                java.util.Date.from(now.plusDays(2).atZone(ZoneId.systemDefault()).toInstant()))))
                .setEnd(new EventDateTime().setDateTime(
                        new com.google.api.client.util.DateTime(
                                java.util.Date.from(now.plusDays(2).plusHours(1).atZone(ZoneId.systemDefault()).toInstant()))));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Get getAPI = mock(Calendar.Events.Get.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.get(eq("primary"), eq("event123"))).thenReturn(getAPI);
        when(getAPI.execute()).thenReturn(updatedGoogleEvent);

        CalendarSyncDto.SyncResponse response = calendarSyncService.syncCalendarToTask(1L);

        assertEquals(SyncStatus.IN_SYNC, response.getSyncStatus());
        assertTrue(response.getConflictResolved());
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("Sync from calendar: Should detect conflict when both modified")
    void testSyncCalendarToTask_ConflictDetected() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusHours(2);
        LocalDateTime recentTime = now.minusMinutes(5);


        calendarEvent.setLastSyncedAt(pastTime);
        calendarEvent.setTaskLastModifiedAt(recentTime);
        calendarEvent.setCalendarLastModifiedAt(recentTime);

        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);
        task.setTitle("Task Title");

        Event conflictingGoogleEvent = new Event()
                .setId("event123")
                .setSummary("Different Calendar Title")
                .setUpdated(new com.google.api.client.util.DateTime(recentTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .setStart(new EventDateTime().setDateTime(
                        new com.google.api.client.util.DateTime(
                                java.util.Date.from(now.plusDays(2).atZone(ZoneId.systemDefault()).toInstant()))));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Get getAPI = mock(Calendar.Events.Get.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.get(eq("primary"), eq("event123"))).thenReturn(getAPI);
        when(getAPI.execute()).thenReturn(conflictingGoogleEvent);

        CalendarSyncDto.SyncResponse response = calendarSyncService.syncCalendarToTask(1L);

        assertEquals(SyncStatus.CONFLICT, response.getSyncStatus());
        assertFalse(response.getConflictResolved());
        verify(calendarEventRepository).save(calendarEvent);
    }

    @Test
    @DisplayName("Sync from calendar: Should fail when task not synced")
    void testSyncCalendarToTask_NotSynced() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(InvalidOperationException.class, () -> calendarSyncService.syncCalendarToTask(1L));
    }


    @Test
    @DisplayName("Disable sync: Should delete calendar event when requested")
    void testDisableSync_WithEventDeletion() throws Exception {
        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);

        CalendarSyncDto.DisableSyncRequest request = CalendarSyncDto.DisableSyncRequest.builder()
                .taskId(1L)
                .deleteCalendarEvent(true)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Delete deleteAPI = mock(Calendar.Events.Delete.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.delete(eq("primary"), eq("event123"))).thenReturn(deleteAPI);

        CalendarSyncDto.SyncDisabledResponse response = calendarSyncService.disableSync(request);

        assertEquals(1L, response.getTaskId());
        assertTrue(response.getCalendarEventDeleted());
        verify(eventsAPI).delete(eq("primary"), eq("event123"));
        verify(calendarEventRepository).delete(calendarEvent);
    }

    @Test
    @DisplayName("Disable sync: Should keep calendar event when not requested")
    void testDisableSync_WithoutEventDeletion() {
        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);

        CalendarSyncDto.DisableSyncRequest request = CalendarSyncDto.DisableSyncRequest.builder()
                .taskId(1L)
                .deleteCalendarEvent(false)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        CalendarSyncDto.SyncDisabledResponse response = calendarSyncService.disableSync(request);

        assertFalse(response.getCalendarEventDeleted());
        verify(googleCalendar, never()).events();
        verify(calendarEventRepository).delete(calendarEvent);
    }


    @Test
    @DisplayName("Delete task: Should delete calendar event and task")
    void testDeleteTaskAndEvent_Success() throws Exception {
        // Given
        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Delete deleteAPI = mock(Calendar.Events.Delete.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.delete(eq("primary"), eq("event123"))).thenReturn(deleteAPI);

        calendarSyncService.deleteTaskAndEvent(1L);

        verify(eventsAPI).delete(eq("primary"), eq("event123"));
        verify(taskRepository).delete(task);
        verify(syncHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Delete task: Should handle Google Calendar deletion failure gracefully")
    void testDeleteTaskAndEvent_GoogleCalendarFailure() throws Exception {
        task.setCalendarEvent(calendarEvent);
        task.setCalendarSyncEnabled(true);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Calendar.Events eventsAPI = mock(Calendar.Events.class);
        Calendar.Events.Delete deleteAPI = mock(Calendar.Events.Delete.class);
        when(googleCalendar.events()).thenReturn(eventsAPI);
        when(eventsAPI.delete(eq("primary"), eq("event123"))).thenReturn(deleteAPI);
        when(deleteAPI.execute()).thenThrow(new IOException("Deletion failed"));

        calendarSyncService.deleteTaskAndEvent(1L);

        verify(taskRepository).delete(task);
    }
}