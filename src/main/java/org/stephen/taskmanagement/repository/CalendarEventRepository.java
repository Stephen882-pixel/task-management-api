package org.stephen.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.stephen.taskmanagement.entity.CalendarEvent;
import org.stephen.taskmanagement.enums.SyncStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    Optional<CalendarEvent> findByEventId(String eventId);

    Optional<CalendarEvent> findByTaskId(Long taskId);

    @Query("SELECT ce FROM CalendarEvent ce WHERE ce.syncStatus = :syncStatus")
    List<CalendarEvent> findBySyncStatus(@Param("syncStatus") SyncStatus syncStatus);

    @Query("SELECT ce FROM CalendarEvent ce WHERE ce.conflictDetected = true")
    List<CalendarEvent> findConflictedEvents();

    @Query("SELECT ce FROM CalendarEvent ce WHERE ce.calendarId = :calendarId")
    List<CalendarEvent> findByCalendarId(@Param("calendarId") String calendarId);

    @Query("SELECT COUNT(ce) FROM CalendarEvent ce WHERE ce.conflictDetected = true")
    Long countConflictedEvents();

    @Query("SELECT ce FROM CalendarEvent ce LEFT JOIN FETCH ce.task WHERE ce.id = :id")
    Optional<CalendarEvent> findByIdWithTask(@Param("id") Long id);
}
