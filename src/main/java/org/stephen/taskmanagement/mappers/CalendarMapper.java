package org.stephen.taskmanagement.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.stephen.taskmanagement.dto.CalendarSyncDto;
import org.stephen.taskmanagement.entity.CalendarEvent;
import org.stephen.taskmanagement.entity.SyncHistory;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CalendarMapper {
    CalendarSyncDto.GoogleEventDetails toGoogleEventDetails(CalendarEvent calendarEvent);

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "eventId", source = "eventId")
    CalendarSyncDto.SyncStatusResponse toSyncStatusResponse(CalendarEvent calendarEvent);

    @Mapping(target = "taskId", source = "calendarEvent.task.id")
    @Mapping(target = "eventId", source = "calendarEvent.eventId")
    @Mapping(target = "syncType", source = "syncType")
    @Mapping(target = "syncDirection", source = "syncDirection")
    @Mapping(target = "syncStatus", source = "syncStatus")
    CalendarSyncDto.SyncHistoryResponse toSyncHistoryResponse(SyncHistory syncHistory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CalendarEvent toEntity(CalendarSyncDto.GoogleEventDetails dto);

    default void updateCalendarEventFromSync(CalendarSyncDto.GoogleEventDetails dto,
                                             @MappingTarget CalendarEvent calendarEvent) {
        if (dto.getSummary() != null) {
            calendarEvent.setEventTitle(dto.getSummary());
        }
        if (dto.getDescription() != null) {
            calendarEvent.setEventDescription(dto.getDescription());
        }
        if (dto.getStartTime() != null) {
            calendarEvent.setEventStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            calendarEvent.setEventEndTime(dto.getEndTime());
        }
        if (dto.getUpdatedAt() != null) {
            calendarEvent.setCalendarLastModifiedAt(dto.getUpdatedAt());
        }
    }
}
