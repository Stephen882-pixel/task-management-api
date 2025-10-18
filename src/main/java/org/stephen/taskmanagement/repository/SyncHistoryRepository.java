package org.stephen.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.stephen.taskmanagement.entity.CalendarEvent;
import org.stephen.taskmanagement.entity.SyncHistory;
import org.stephen.taskmanagement.enums.SyncDirection;
import org.stephen.taskmanagement.enums.SyncStatus;

import java.util.List;

@Repository
public interface SyncHistoryRepository extends JpaRepository<SyncHistory, Long> {
    @Query("SELECT sh FROM SyncHistory sh WHERE sh.calendarEvent = :calendarEvent ORDER BY sh.syncedAt DESC")
    List<SyncHistory> findByCalendarEventOrderBySyncedAtDesc(@Param("calendarEvent") CalendarEvent calendarEvent);

    @Query("SELECT sh FROM SyncHistory sh WHERE sh.calendarEvent.id = :calendarEventId ORDER BY sh.syncedAt DESC")
    List<SyncHistory> findByCalendarEventIdOrderBySyncedAtDesc(@Param("calendarEventId") Long calendarEventId);

    @Query("SELECT sh FROM SyncHistory sh WHERE sh.syncStatus = :syncStatus")
    List<SyncHistory> findBySyncStatus(@Param("syncStatus") SyncStatus syncStatus);

    @Query("SELECT sh FROM SyncHistory sh WHERE sh.syncDirection = :syncDirection")
    List<SyncHistory> findBySyncDirection(@Param("syncDirection") SyncDirection syncDirection);

    @Query("SELECT COUNT(sh) FROM SyncHistory sh WHERE sh.syncStatus = 'SYNC_FAILED'")
    Long countFailedSyncs();
}
