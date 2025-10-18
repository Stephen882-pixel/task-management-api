package org.stephen.taskmanagement.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Calendar Sync", description = "Google Calendar synchronization endpoints")
public class CalendarSyncController {

    private final CalendarSyncService calendarSyncService;
    private final ConflictResolutionService conflictResolutionService;


}
