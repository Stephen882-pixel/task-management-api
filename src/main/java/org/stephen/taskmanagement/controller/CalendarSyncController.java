package org.stephen.taskmanagement.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stephen.taskmanagement.dto.CalendarSyncDto;
import org.stephen.taskmanagement.service.CalendarSyncService;
import org.stephen.taskmanagement.service.ConflictResolutionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Calendar Sync", description = "Google Calendar synchronization endpoints")
public class CalendarSyncController {

    private final CalendarSyncService calendarSyncService;
    private final ConflictResolutionService conflictResolutionService;

    @PostMapping("/enable")
    @Operation(summary = "Enable calendar sync for a task",
            description = "Link a task with Google Calendar and start synchronization")
    @ApiResponse(responseCode = "201", description = "Calendar sync enabled successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "409", description = "Task already synced")
    public ResponseEntity<CalendarSyncDto.SyncEnabledResponse> enableSync(
            @Valid @RequestBody CalendarSyncDto.EnableSyncRequest request) {
        log.info("POST /api/v1/calendar/enable - Task ID: {}", request.getTaskId());
        CalendarSyncDto.SyncEnabledResponse response = calendarSyncService.enableSync(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable calendar sync for a task",
            description = "Stop synchronization and optionally delete calendar event")
    @ApiResponse(responseCode = "200", description = "Calendar sync disabled successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<CalendarSyncDto.SyncDisabledResponse> disableSync(
            @Valid @RequestBody CalendarSyncDto.DisableSyncRequest request) {
        log.info("POST /api/v1/calendar/disable - Task ID: {}", request.getTaskId());
        CalendarSyncDto.SyncDisabledResponse response = calendarSyncService.disableSync(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync-to-calendar/{taskId}")
    @Operation(summary = "Sync task changes to Google Calendar",
            description = "Push task updates to the linked Google Calendar event")
    @ApiResponse(responseCode = "200", description = "Task synced to calendar successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "400", description = "Task not synced with calendar")
    public ResponseEntity<CalendarSyncDto.SyncResponse> syncTaskToCalendar(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        log.info("POST /api/v1/calendar/sync-to-calendar/{} - Syncing task to calendar", taskId);
        CalendarSyncDto.SyncResponse response = calendarSyncService.syncTaskToCalendar(taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{taskId}")
    @Operation(summary = "Get calendar sync status",
            description = "Get current synchronization status and conflict information for a task")
    @ApiResponse(responseCode = "200", description = "Sync status retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Task not found or not synced")
    public ResponseEntity<CalendarSyncDto.SyncStatusResponse> getSyncStatus(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        log.info("GET /api/v1/calendar/status/{} - Getting sync status", taskId);
        CalendarSyncDto.SyncStatusResponse response = calendarSyncService.getSyncStatus(taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{taskId}")
    @Operation(summary = "Get sync history",
            description = "Retrieve synchronization history for a task")
    @ApiResponse(responseCode = "200", description = "Sync history retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<List<CalendarSyncDto.SyncHistoryResponse>> getSyncHistory(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        log.info("GET /api/v1/calendar/history/{} - Getting sync history", taskId);
        List<CalendarSyncDto.SyncHistoryResponse> response = calendarSyncService.getSyncHistory(taskId);
        return ResponseEntity.ok(response);
    }


}
