package org.stephen.taskmanagement.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stephen.taskmanagement.dto.request.CreateTagRequestDto;
import org.stephen.taskmanagement.dto.request.UpdateTagRequestDto;
import org.stephen.taskmanagement.dto.response.TagDetailResponseDto;
import org.stephen.taskmanagement.dto.response.TagListResponseDto;
import org.stephen.taskmanagement.service.TagService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tags", description = "Tag management endpoints")
public class TagController {

    private final TagService tagService;

    @PostMapping
    @Operation(summary = "Create a new tag", description = "Create a new tag with name and optional description")
    @ApiResponse(responseCode = "201", description = "Tag created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "Tag with this name already exists")
    public ResponseEntity<TagListResponseDto> createTag(@Valid @RequestBody CreateTagRequestDto request){
        log.info("POST /api/v1/tags - Creating new tag");
        TagListResponseDto response = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID", description = "Retrieve a specific tag by its ID")
    @ApiResponse(responseCode = "200", description = "Tag retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public ResponseEntity<TagListResponseDto> getTag(
            @Parameter(description = "Tag ID") @PathVariable Long id){
        log.info("GET /api/v1/tags/{} - Fetching tag", id);
        TagListResponseDto response = tagService.getTagById(id);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}/details")
    @Operation(summary = "Get tag details with tasks", description = "Retrieve detailed information about a tag including all associated tasks")
    @ApiResponse(responseCode = "200", description = "Tag details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public ResponseEntity<TagDetailResponseDto> getTagDetails(
            @Parameter(description = "Tag ID") @PathVariable Long id){
        log.info("GET /api/v1/tags/{}/details - Fetching tag details", id);
        TagDetailResponseDto response = tagService.getTagDetails(id);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    @Operation(summary = "Get all tags", description = "Retrieve all tags in the system")
    @ApiResponse(responseCode = "200", description = "Tags retrieved successfully")
    public ResponseEntity<List<TagListResponseDto>> getAllTags(){
        log.info("GET /api/v1/tags - Fetching all tags");
        List<TagListResponseDto> responses = tagService.getAllTags();
        return ResponseEntity.ok(responses);
    }


    @GetMapping("/search")
    @Operation(summary = "Search tags by name", description = "Search tags by name containing the provided string")
    @ApiResponse(responseCode = "200", description = "Tags retrieved successfully")
    public ResponseEntity<List<TagListResponseDto>> searchTags(
            @Parameter(description = "Name search string") @RequestParam String name){
        log.info("GET /api/v1/tags/search - Searching tags with name: {}", name);
        List<TagListResponseDto> responses = tagService.searchTags(name);
        return ResponseEntity.ok(responses);
    }



    @PutMapping("/{id}")
    @Operation(summary = "Update tag", description = "Update tag name or description")
    @ApiResponse(responseCode = "200", description = "Tag updated successfully")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "Tag with this name already exists")

    public ResponseEntity<TagListResponseDto> updateTag(
            @Parameter(description = "Tag ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTagRequestDto request){
        log.info("PUT /api/v1/tags/{} - Updating tag", id);
        TagListResponseDto response = tagService.updateTag(id, request);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tag", description = "Delete a tag by ID. Associated tasks will not be deleted")
    @ApiResponse(responseCode = "204", description = "Tag deleted successfully")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public ResponseEntity<Void> deleteTag(
            @Parameter(description = "Tag ID") @PathVariable Long id){
        log.info("DELETE /api/v1/tags/{} - Deleting tag", id);
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
