package org.stephen.taskmanagement.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.stephen.taskmanagement.dto.request.CreateTagRequestDto;
import org.stephen.taskmanagement.dto.request.CreateTagRequestDto;
import org.stephen.taskmanagement.dto.request.UpdateTagRequestDto;
import org.stephen.taskmanagement.dto.response.CreateTaskResponseDto;
import org.stephen.taskmanagement.dto.response.TagDetailResponseDto;
import org.stephen.taskmanagement.dto.response.TagListResponseDto;
import org.stephen.taskmanagement.dto.response.TasksListResponseDto;
import org.stephen.taskmanagement.entity.Tag;
import org.stephen.taskmanagement.entity.Task;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Tag toEntity(CreateTagRequestDto request);

    TagListResponseDto toResponse(Tag tag);

    @Mapping(target = "taskCount", expression = "java(tag.getTasks().size())")
    @Mapping(target = "tasks", source = "tasks")
    TagDetailResponseDto toDetailResponse(Tag tag);

    @Mapping(target = "tagCount", expression = "java(task.getTags().size())")
    TasksListResponseDto taskToListResponse(Task task);

    default void updateTagFromRequest(UpdateTagRequestDto request, @MappingTarget Tag tag) {
        if (request.getName() != null) {
            tag.setName(request.getName());
        }
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
    }
}
