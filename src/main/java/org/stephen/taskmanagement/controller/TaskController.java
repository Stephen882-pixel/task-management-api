package org.stephen.taskmanagement.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stephen.taskmanagement.dto.request.CreateTaskRequestDto;
import org.stephen.taskmanagement.dto.request.UpdateTaskRequestDto;
import org.stephen.taskmanagement.dto.response.CreateTaskResponseDto;
import org.stephen.taskmanagement.dto.response.TasksListResponseDto;
import org.stephen.taskmanagement.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task", description = "Create a new task with title, description, due date, and tags")
    @ApiResponse(responseCode = "201", description = "Task created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<CreateTaskResponseDto> createTask(@Valid @RequestBody CreateTaskRequestDto request){
        log.info("POST /api/v1/tasks - Creating new task");
        CreateTaskResponseDto response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Retrieve a specific task with all its details and tags")
    @ApiResponse(responseCode = "200", description = "Task retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<CreateTaskResponseDto> getTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        log.info("GET /api/v1/tasks/{} - Fetching task", id);
        CreateTaskResponseDto response = taskService.getTaskById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Retrieve all tasks with optional filtering by status or tag")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    public ResponseEntity<List<TasksListResponseDto>> getAllTasks(
            @Parameter(description = "Task status filter (PENDING, IN_PROGRESS, COMPLETED, ARCHIVED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Tag ID filter")
            @RequestParam(required = false) Long tagId) {
        log.info("GET /api/v1/tasks - Fetching all tasks with status: {} and tagId: {}", status, tagId);
        List<TasksListResponseDto> responses = taskService.getAllTasks(status, tagId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    @Operation(summary = "Search tasks by title", description = "Search tasks by title containing the provided string")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    public ResponseEntity<List<TasksListResponseDto>> searchTasks(
            @Parameter(description = "Title search string") @RequestParam String title
    ){
        log.info("GET /api/v1/tasks/search - Searching tasks with title: {}", title);
        List<TasksListResponseDto> responses = taskService.searchTasks(title);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Update task details such as title, status, due date, and tags")
    @ApiResponse(responseCode = "200", description = "Task updated successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<CreateTaskResponseDto> updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequestDto request) {
        log.info("PUT /api/v1/tasks/{} - Updating task", id);
        CreateTaskResponseDto response = taskService.updateTask(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task", description = "Delete a task by ID. Associated tags will not be deleted")
    @ApiResponse(responseCode = "204", description = "Task deleted successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        log.info("DELETE /api/v1/tasks/{} - Deleting task", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

}
