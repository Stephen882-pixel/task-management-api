package org.stephen.taskmanagement.controller;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stephen.taskmanagement.dto.request.CreateTagRequestDto;
import org.stephen.taskmanagement.dto.response.TagDetailResponseDto;
import org.stephen.taskmanagement.dto.response.TagListResponseDto;
import org.stephen.taskmanagement.service.TagService;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tags", description = "Tag management endpoints")
public class TagController {

    private final TagService tagService;

    public ResponseEntity<TagListResponseDto> createTag(@Valid @RequestBody CreateTagRequestDto request){
        log.info("POST /api/v1/tags - Creating new tag");
        TagListResponseDto response = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public ResponseEntity<TagListResponseDto> getTag(
            @Parameter(description = "Tag ID") @PathVariable Long id){
        log.info("GET /api/v1/tags/{} - Fetching tag", id);
        TagListResponseDto response = tagService.getTagById(id);
        return ResponseEntity.ok(response);
    }


}
