package org.stephen.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stephen.taskmanagement.dto.request.CreateTaskRequestDto;
import org.stephen.taskmanagement.dto.request.UpdateTaskRequestDto;
import org.stephen.taskmanagement.dto.response.CreateTaskResponseDto;
import org.stephen.taskmanagement.dto.response.TasksListResponseDto;
import org.stephen.taskmanagement.entity.Tag;
import org.stephen.taskmanagement.entity.Task;
import org.stephen.taskmanagement.enums.TaskStatus;
import org.stephen.taskmanagement.exception.ResourceNotFoundException;
import org.stephen.taskmanagement.exception.ValidationException;
import org.stephen.taskmanagement.mappers.TaskMapper;
import org.stephen.taskmanagement.repository.TagRepository;
import org.stephen.taskmanagement.repository.TaskRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final TagService tagService;

    @Transactional
    public CreateTaskResponseDto createTask(CreateTaskRequestDto request){
        log.info("Creating task with title: {}", request.getTitle());
        try{

            Task task = taskMapper.toEntity(request);
            task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            Set<Tag> tags = request.getTagNames().stream()
                    .map(tagService::getOrCreateTag)
                    .collect(Collectors.toSet());

            log.debug("Tags resolved: {}", tags.stream()
                    .map(t -> (t.getId()!=null? t.getId() : "new") + ":" + t.getName())
                    .collect(Collectors.toList()));

            tags.forEach(task::addTag);
            log.debug("Saving task (pre-save) -> title: {}, status: {}, dueDate: {}, createdAt: {}, tagsCount: {}",
                    task.getTitle(), task.getStatus(), task.getDueDate(), task.getCreatedAt(), task.getTags().size());

            Task savedTask = null;
            try {
                savedTask = taskRepository.save(task);
            } catch (Exception e) {
                // log full stacktrace and root cause chain
                log.error("Error while saving task. Task pre-save state: title={}, status={}, tags={}",
                        task.getTitle(),
                        task.getStatus(),
                        task.getTags().stream().map(Tag::getName).collect(Collectors.joining(",")),
                        e);
                throw e;
            }

            log.info("Task created successfully with id: {}", savedTask.getId());
            return taskMapper.toResponse(savedTask);

        } catch (IllegalArgumentException e){
            log.error("Invalid task status provided: {}", request.getStatus(), e);
            throw new ValidationException("Invalid task status: " + request.getStatus());
        }
    }

    @Transactional(readOnly = true)
    public CreateTaskResponseDto getTaskById(Long id){
        log.info("Fetching task with id {}:",id);
        Task task  = taskRepository.findByIdWithTags(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task","id",String.valueOf(id)));
        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TasksListResponseDto> getAllTasks(String status,Long tagId){
        log.info("Fetching all tasks with status: {}, tagId: {}", status, tagId);

        List<Task> tasks;
        if(status != null){
            tasks = taskRepository.findByStatus(TaskStatus.valueOf(status.toUpperCase()));
        } else if(tagId != null){
            tasks = taskRepository.findByTagId(tagId);
        } else {
            tasks = taskRepository.findAllWithTags();
        }

        return tasks.stream()
                .map(taskMapper::toListResponse)
                .collect(Collectors.toList());
    }

    public CreateTaskResponseDto updateTask(Long id, UpdateTaskRequestDto request){
        log.info("Updating task with id: {}",id);
        Task task = taskRepository.findByIdWithTags(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task","id",String.valueOf(id)));
        try{
            taskMapper.updateTaskFromRequest(request,task);
            if(request.getTagNames() != null && !request.getTagNames().isEmpty()){
                task.getTags().clear();
                Set<Tag> updateTags = request.getTagNames().stream()
                        .map(tagName -> tagService.getOrCreateTag(tagName))
                        .collect(Collectors.toSet());
                updateTags.forEach(task::addTag);
            }
            Task updatedTask = taskRepository.save(task);
            log.info("Task updated successfully with id: {}",updatedTask.getId());
            return taskMapper.toResponse(updatedTask);
        } catch (IllegalArgumentException e){
            log.error("Invalid task status provided during update: {}",request.getStatus());
            throw new ValidationException("Invalid task status: " + request.getStatus());
        }
    }

    public void deleteTask(Long id){
        log.info("Deleting task with id: {}",id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task","id",String.valueOf(id)));

        task.getTags().stream()
                .forEach(tag -> task.removeTag(tag));

        taskRepository.delete(task);
        log.info("Task deleted successfully with id: {}",id);
    }

    @Transactional(readOnly = true)
    public List<TasksListResponseDto> searchTasks(String title){
        log.info("Searching for tasks with title containing: {}",title);
        return taskRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(taskMapper::toListResponse)
                .collect(Collectors.toList());
    }

}
