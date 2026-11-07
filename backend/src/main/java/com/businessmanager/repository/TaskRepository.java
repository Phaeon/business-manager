package com.businessmanager.repository;

import com.businessmanager.entity.Task;
import com.businessmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    List<Task> findByAssigneeIdAndStatusNotOrderByCreatedAtAsc(UUID assigneeId, TaskStatus status);

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);
}
