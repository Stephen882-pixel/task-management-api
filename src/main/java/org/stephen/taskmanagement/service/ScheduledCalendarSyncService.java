package org.stephen.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stephen.taskmanagement.config.CalendarSyncProperties;
import org.stephen.taskmanagement.entity.CalendarEvent;
import org.stephen.taskmanagement.enums.SyncStatus;
import org.stephen.taskmanagement.repository.CalendarEventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledCalendarSyncService {
    private final CalendarSyncService calendarSyncService;
    private final CalendarEventRepository calendarEventRepository;
    private final CalendarSyncProperties syncProperties;

    @Scheduled(cron = "${scheduling.sync-cron:0 */5 * * * ?}")
    @Transactional
    public void performScheduledSync() {
        if (!syncProperties.getAutoSyncEnabled()) {
            log.debug("Automatic sync is disabled");
            return;
        }

        log.info("Starting scheduled calendar synchronization");

        try {
            // Find all events that need syncing
            List<CalendarEvent> eventsToSync = calendarEventRepository.findBySyncStatus(SyncStatus.SYNC_PENDING)
                    .stream()
                    .filter(event -> event.getTask() != null && event.getTask().getCalendarSyncEnabled())
                    .collect(Collectors.toList());

            log.info("Found {} events pending sync", eventsToSync.size());

            eventsToSync.forEach(event -> {
                try {
                    log.debug("Syncing event for task: {}", event.getTask().getId());
                    calendarSyncService.syncTaskToCalendar(event.getTask().getId());
                } catch (Exception e) {
                    log.error("Failed to sync task during scheduled sync: {}", event.getTask().getId(), e);
                }
            });

            log.info("Scheduled synchronization completed");

        } catch (Exception e) {
            log.error("Error during scheduled calendar synchronization", e);
        }
    }

    @Scheduled(cron = "${scheduling.conflict-check-cron:0 0 * * * ?}")
    @Transactional
    public void performConflictCheck() {
        log.info("Starting scheduled conflict check");

        try {
            // Find all potentially conflicted events
            List<CalendarEvent> conflictedEvents = calendarEventRepository.findConflictedEvents()
                    .stream()
                    .filter(event -> event.getTask() != null && event.getTask().getCalendarSyncEnabled())
                    .collect(Collectors.toList());

            log.info("Found {} conflicted events", conflictedEvents.size());

            conflictedEvents.forEach(event -> {
                try {
                    log.debug("Checking conflict for task: {}", event.getTask().getId());
                    // Perform sync from calendar to detect current conflicts
                    calendarSyncService.syncCalendarToTask(event.getTask().getId());
                } catch (Exception e) {
                    log.error("Failed to check conflict for task: {}", event.getTask().getId(), e);
                }
            });

            log.info("Scheduled conflict check completed");

        } catch (Exception e) {
            log.error("Error during scheduled conflict check", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void performRetryFailedSyncs() {
        log.info("Starting retry of failed syncs");

        try {
            List<CalendarEvent> failedEvents = calendarEventRepository.findBySyncStatus(SyncStatus.SYNC_FAILED)
                    .stream()
                    .filter(event -> event.getTask() != null && event.getTask().getCalendarSyncEnabled())
                    .collect(Collectors.toList());

            log.info("Found {} failed sync events to retry", failedEvents.size());

            failedEvents.forEach(event -> {
                try {
                    log.debug("Retrying sync for task: {}", event.getTask().getId());
                    calendarSyncService.syncTaskToCalendar(event.getTask().getId());
                } catch (Exception e) {
                    log.error("Failed to retry sync for task: {}", event.getTask().getId(), e);
                }
            });

            log.info("Retry of failed syncs completed");

        } catch (Exception e) {
            log.error("Error during retry of failed syncs", e);
        }
    }

    public void triggerFullSync() {
        log.info("Triggering full synchronization of all tasks");

        try {
            List<CalendarEvent> allEvents = calendarEventRepository.findAll()
                    .stream()
                    .filter(event -> event.getTask() != null && event.getTask().getCalendarSyncEnabled())
                    .collect(Collectors.toList());

            log.info("Full sync initiated for {} calendar events", allEvents.size());

            int syncedCount = 0;
            int failedCount = 0;

            for (CalendarEvent event : allEvents) {
                try {
                    calendarSyncService.syncTaskToCalendar(event.getTask().getId());
                    syncedCount++;
                } catch (Exception e) {
                    log.error("Failed to sync task during full sync: {}", event.getTask().getId(), e);
                    failedCount++;
                }
            }

            log.info("Full sync completed: {} succeeded, {} failed", syncedCount, failedCount);

        } catch (Exception e) {
            log.error("Error during full synchronization", e);
        }
    }

    @Transactional(readOnly = true)
    public SyncStatistics getSyncStatistics() {
        long totalEvents = calendarEventRepository.count();
        long inSyncCount = calendarEventRepository.findBySyncStatus(SyncStatus.IN_SYNC).size();
        long conflictedCount = calendarEventRepository.countConflictedEvents();
        long failedCount = calendarEventRepository.findBySyncStatus(SyncStatus.SYNC_FAILED).size();

        return SyncStatistics.builder()
                .totalCalendarEvents(totalEvents)
                .inSyncCount(inSyncCount)
                .conflictedCount(conflictedCount)
                .failedCount(failedCount)
                .successRate(totalEvents > 0 ? (double) inSyncCount / totalEvents * 100 : 0)
                .build();
    }

    public static class SyncStatistics {
        private final Long totalCalendarEvents;
        private final Long inSyncCount;
        private final Long conflictedCount;
        private final Long failedCount;
        private final Double successRate;

        public SyncStatistics(Long totalCalendarEvents, Long inSyncCount,
                              Long conflictedCount, Long failedCount, Double successRate) {
            this.totalCalendarEvents = totalCalendarEvents;
            this.inSyncCount = inSyncCount;
            this.conflictedCount = conflictedCount;
            this.failedCount = failedCount;
            this.successRate = successRate;
        }
        public static SyncStatisticsBuilder builder() {
            return new SyncStatisticsBuilder();
        }

        public Long getTotalCalendarEvents() { return totalCalendarEvents; }
        public Long getInSyncCount() { return inSyncCount; }
        public Long getConflictedCount() { return conflictedCount; }
        public Long getFailedCount() { return failedCount; }
        public Double getSuccessRate() { return successRate; }

        public static class SyncStatisticsBuilder {
            private Long totalCalendarEvents;
            private Long inSyncCount;
            private Long conflictedCount;
            private Long failedCount;
            private Double successRate;

            public SyncStatisticsBuilder totalCalendarEvents(Long totalCalendarEvents) {
                this.totalCalendarEvents = totalCalendarEvents;
                return this;
            }

            public SyncStatisticsBuilder inSyncCount(Long inSyncCount) {
                this.inSyncCount = inSyncCount;
                return this;
            }

            public SyncStatisticsBuilder conflictedCount(Long conflictedCount) {
                this.conflictedCount = conflictedCount;
                return this;
            }

            public SyncStatisticsBuilder failedCount(Long failedCount) {
                this.failedCount = failedCount;
                return this;
            }

            public SyncStatisticsBuilder successRate(Double successRate) {
                this.successRate = successRate;
                return this;
            }

            public SyncStatistics build() {
                return new SyncStatistics(totalCalendarEvents, inSyncCount, conflictedCount, failedCount, successRate);
            }
        }
    }
}
