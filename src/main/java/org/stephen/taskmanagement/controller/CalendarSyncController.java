package org.stephen.taskmanagement.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stephen.taskmanagement.dto.CalendarSyncDto;
import org.stephen.taskmanagement.service.CalendarSyncService;
import org.stephen.taskmanagement.service.ConflictResolutionService;

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



}
