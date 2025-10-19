package org.stephen.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stephen.taskmanagement.dto.request.CreateTagRequestDto;
import org.stephen.taskmanagement.dto.request.UpdateTagRequestDto;
import org.stephen.taskmanagement.dto.response.TagDetailResponseDto;
import org.stephen.taskmanagement.dto.response.TagListResponseDto;
import org.stephen.taskmanagement.entity.Tag;
import org.stephen.taskmanagement.exception.DuplicateResourceException;
import org.stephen.taskmanagement.exception.ResourceNotFoundException;
import org.stephen.taskmanagement.mappers.TagMapper;
import org.stephen.taskmanagement.mappers.TaskMapper;
import org.stephen.taskmanagement.repository.TagRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final TagMapper tagMapper;

    public TagListResponseDto createTag(CreateTagRequestDto request){
        log.info("Creating tag with name: {}",request.getName());
        tagRepository.findByNameIgnoreCase(request.getName())
                .ifPresent(tag -> {
                    throw new DuplicateResourceException("Tag","name", request.getName());
                });

        Tag tag = tagMapper.toEntity(request);
        Tag savedTag = tagRepository.save(tag);
        log.info("Tag created successfully wih id: {}",savedTag.getId());
        return tagMapper.toResponse(savedTag);
    }

    @Transactional(readOnly = true)
    public TagListResponseDto getTagById(Long id) {
        log.info("Fetching tag with id: {}",id);
        Tag tag = tagRepository.findByIdWithTasks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag","id",String.valueOf(id)));
        return tagMapper.toResponse(tag);
    }

    @Transactional(readOnly = true)
    public TagDetailResponseDto getTagDetails(Long id){
        log.info("Fetching tag details with id: {}",id);
        Tag tag = tagRepository.findByIdWithTasks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag","id",String.valueOf(id)));
        return tagMapper.toDetailResponse(tag);
    }

    @Transactional(readOnly = true)
    public List<TagListResponseDto> getAllTags(){
        log.info("Fetching all tags");
        return tagRepository.findAll().stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TagListResponseDto updateTag(Long id, UpdateTagRequestDto request) {
        log.info("Updating tag with id: {}",id);
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag","id",String.valueOf(id)));

        if(request.getName() != null && !request.getName().equals(tag.getName())){
            tagRepository.findByNameIgnoreCase(request.getName())
                    .ifPresent(existingTag -> {
                        throw new DuplicateResourceException("Tag","name", request.getName());
                    });
        }

        tagMapper.updateTagFromRequest(request,tag);
        Tag updatedTag = tagRepository.save(tag);
        log.info("Tag updated successfully with id: {}", id);
        return tagMapper.toResponse(updatedTag);
    }

    public void deleteTag(Long id){
        log.info("Deleting tag with id: {}",id);
        Tag tag = tagRepository.findByIdWithTasks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag","id",String.valueOf(id)));

        tag.getTasks().stream()
                .forEach(task -> {
                    task.removeTag(tag);
                });

        tagRepository.delete(tag);
        log.info("Tag deleted successfully with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<TagListResponseDto> searchTags(String name){
        log.info("Searching for tags with name containing: {}",name);
        return tagRepository.findByNameContainingIgnoreCase(name).stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Tag getOrCreateTag(String tagName){
        return tagRepository.findByNameIgnoreCase(tagName)
                .orElseGet(() -> {
                   log.info("Auto-creating tag with name: {}",tagName);
                   Tag newTag = Tag.builder()
                           .name(tagName)
                           .tasks(new HashSet<>())
                           .createdAt(LocalDateTime.now())
                           .updatedAt(LocalDateTime.now())
                           .build();
                   return tagRepository.save(newTag);
                });
    }

}
