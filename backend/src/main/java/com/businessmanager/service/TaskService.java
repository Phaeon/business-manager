package com.businessmanager.service;

import com.businessmanager.dto.task.TaskDtos.TaskRequest;
import com.businessmanager.dto.task.TaskDtos.TaskResponse;
import com.businessmanager.entity.Project;
import com.businessmanager.entity.Task;
import com.businessmanager.entity.TaskPriority;
import com.businessmanager.entity.TaskStatus;
import com.businessmanager.entity.User;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.TaskMapper;
import com.businessmanager.repository.ProjectRepository;
import com.businessmanager.repository.TaskRepository;
import com.businessmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public List<TaskResponse> findByProject(UUID projectId) {
        return taskRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
                .stream().map(taskMapper::toResponse).toList();
    }

    public List<TaskResponse> findMine(UUID userId) {
        return taskRepository.findByAssigneeIdAndStatusNotOrderByCreatedAtAsc(userId, TaskStatus.DONE)
                .stream().map(taskMapper::toResponse).toList();
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + request.projectId()));

        Task task = Task.builder()
                .project(project)
                .title(request.title().trim())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .estimateHours(request.estimateHours())
                .spentHours(request.spentHours() != null ? request.spentHours() : BigDecimal.ZERO)
                .assignee(findAssignee(request.assigneeId()))
                .build();
        Task saved = taskRepository.save(task);
        refreshProgress(project);
        return taskMapper.toResponse(saved);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = getOrThrow(id);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        task.setEstimateHours(request.estimateHours());
        if (request.spentHours() != null) {
            task.setSpentHours(request.spentHours());
        }
        task.setAssignee(findAssignee(request.assigneeId()));
        Task saved = taskRepository.save(task);
        refreshProgress(task.getProject());
        return taskMapper.toResponse(saved);
    }

    /** Utilisé par le tableau Kanban (glisser-déposer). */
    @Transactional
    public TaskResponse updateStatus(UUID id, TaskStatus status) {
        Task task = getOrThrow(id);
        task.setStatus(status);
        Task saved = taskRepository.save(task);
        refreshProgress(task.getProject());
        return taskMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Task task = getOrThrow(id);
        Project project = task.getProject();
        taskRepository.delete(task);
        refreshProgress(project);
    }

    /** La progression du projet = pourcentage de tâches terminées. */
    private void refreshProgress(Project project) {
        long total = taskRepository.countByProjectId(project.getId());
        long done = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.DONE);
        project.setProgress(total == 0 ? 0 : (int) Math.round(done * 100.0 / total));
        projectRepository.save(project);
    }

    private User findAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + assigneeId));
    }

    private Task getOrThrow(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable : " + id));
    }
}
