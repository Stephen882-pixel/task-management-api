package org.stephen.taskmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stephen.taskmanagement.dto.request.CreateTagRequestDto;
import org.stephen.taskmanagement.dto.response.TagDetailResponseDto;
import org.stephen.taskmanagement.entity.Tag;
import org.stephen.taskmanagement.mappers.TagMapper;
import org.stephen.taskmanagement.mappers.TaskMapper;
import org.stephen.taskmanagement.repository.TagRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final TagMapper tagMapper;

    public TagDetailResponseDto createTag(CreateTagRequestDto request){
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
    public TagDetailResponseDto getTagById(Long id) {
        log.info("Fetching tag with id: {}",id);
        Tag tag = tagRepository.findByIdWithTasks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag","id",String.valueOf(id)));
        return tagMapper.toResponse(tag);
    }
}
