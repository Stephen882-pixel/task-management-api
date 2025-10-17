package org.stephen.taskmanagement.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.stephen.taskmanagement.dto.request.CreateTaskRequestDto;
import org.stephen.taskmanagement.dto.request.UpdateTaskRequestDto;
import org.stephen.taskmanagement.dto.response.CreateTaskResponseDto;
import org.stephen.taskmanagement.dto.response.TagDetailResponseDto;
import org.stephen.taskmanagement.dto.response.TasksListResponseDto;
import org.stephen.taskmanagement.entity.Tag;
import org.stephen.taskmanagement.entity.Task;
import org.stephen.taskmanagement.enums.TaskStatus;

import java.util.List;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    @Mapping(target = "id",ignore = true)
    @Mapping(target = "createdAt",ignore = true)
    @Mapping(target = "updatedAt",ignore = true)
    @Mapping(target = "tags",ignore = true)
    Task toEntity(CreateTaskRequestDto request);

    CreateTaskResponseDto toResponse(Task task);

    TasksListResponseDto toListResponse(Task task);

    TagDetailResponseDto tagToResponse(Tag tag);

    default void updateTaskFromRequest(UpdateTaskRequestDto request, @MappingTarget Task task) {
        if(request.getTitle() != null){
            task.setTitle(request.getTitle());
        }
        if(request.getDescription() != null){
            task.setDescription(request.getDescription());
        }
        if(request.getDueDate() != null){
            task.setDueDate(request.getDueDate());
        }
        if(request.getStatus() != null){
            task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
        }
    }
}

