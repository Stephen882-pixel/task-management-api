package org.stephen.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.stephen.taskmanagement.config.CalendarSyncProperties;
import org.stephen.taskmanagement.repository.CalendarEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledCalendarSyncService {
    private final CalendarSyncService calendarSyncService;
    private final CalendarEventRepository calendarEventRepository;
    private final CalendarSyncProperties syncProperties;
}
