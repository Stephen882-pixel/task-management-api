package org.stephen.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.stephen.taskmanagement.entity.Task;
import org.stephen.taskmanagement.enums.TaskStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.status = :status")
    List<Task> findByStatus(@Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t JOIN t.tags tag WHERE tag.id = :tagId")
    List<Task> findByTagId(@Param("tagId") Long tagId);

    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Task> findByTitleContainingIgnoreCase(@Param("title") String title);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.tags WHERE t.id = :id")
    Optional<Task> findByIdWithTags(@Param("id") Long id);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.tags")
    List<Task> findAllWithTags();


}
