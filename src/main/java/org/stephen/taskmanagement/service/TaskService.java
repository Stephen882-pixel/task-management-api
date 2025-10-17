package org.stephen.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stephen.taskmanagement.dto.request.CreateTaskRequestDto;
import org.stephen.taskmanagement.dto.response.CreateTaskResponseDto;
import org.stephen.taskmanagement.entity.Tag;
import org.stephen.taskmanagement.entity.Task;
import org.stephen.taskmanagement.enums.TaskStatus;
import org.stephen.taskmanagement.mappers.TaskMapper;
import org.stephen.taskmanagement.repository.TagRepository;
import org.stephen.taskmanagement.repository.TaskRepository;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final TagService tagService;

    public CreateTaskResponseDto createTask(CreateTaskRequestDto request){
        log.info("Creating task with title: {}",request.getTitle());
        try{
            Task task = taskMapper.toEntity(request);
            task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));

            Set<Tag> tags = request.getTagNames().stream()
                    .map(tagName -> tagService.)
        }
    }
}
