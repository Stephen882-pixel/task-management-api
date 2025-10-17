package org.stephen.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.stephen.taskmanagement.entity.Tag;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByNameIgnoreCase(String name);

    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Tag> findByNameContainingIgnoreCase(String name);

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.tasks WHERE t.id = :id")
    Optional<Tag> findByIdWithTasks(Long id);

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.tasks")
    List<Tag> findAllWithTasks();
}
