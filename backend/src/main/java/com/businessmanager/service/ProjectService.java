package com.businessmanager.service;

import com.businessmanager.dto.project.ProjectDtos.CommentRequest;
import com.businessmanager.dto.project.ProjectDtos.CommentResponse;
import com.businessmanager.dto.project.ProjectDtos.ProjectRequest;
import com.businessmanager.dto.project.ProjectDtos.ProjectResponse;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.NotificationType;
import com.businessmanager.entity.Project;
import com.businessmanager.entity.ProjectComment;
import com.businessmanager.entity.ProjectStatus;
import com.businessmanager.entity.User;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.ProjectMapper;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.ProjectCommentRepository;
import com.businessmanager.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProjectRepository projectRepository;
    private final ProjectCommentRepository commentRepository;
    private final CustomerRepository customerRepository;
    private final ProjectMapper projectMapper;
    private final NotificationService notificationService;

    public Page<ProjectResponse> findAll(String search, ProjectStatus status, Pageable pageable) {
        String q = search == null ? "" : search.trim();
        Page<Project> page = status == null
                ? projectRepository.search(q, pageable)
                : projectRepository.searchByStatus(q, status, pageable);
        return page.map(projectMapper::toResponse);
    }

    public ProjectResponse findById(UUID id) {
        return projectMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = Project.builder()
                .name(request.name().trim())
                .description(request.description())
                .customer(getCustomer(request.customerId()))
                .budget(request.budget())
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .status(request.status() != null ? request.status() : ProjectStatus.PLANNED)
                .build();
        Project saved = projectRepository.save(project);
        notificationService.notify(NotificationType.NEW_PROJECT,
                "Nouveau projet : " + saved.getName(), "/projets/" + saved.getId());
        return projectMapper.toResponse(saved);
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request) {
        Project project = getOrThrow(id);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setCustomer(getCustomer(request.customerId()));
        project.setBudget(request.budget());
        project.setStartDate(request.startDate());
        project.setDueDate(request.dueDate());
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(UUID id) {
        projectRepository.delete(getOrThrow(id));
    }

    public List<CommentResponse> comments(UUID projectId) {
        getOrThrow(projectId);
        return commentRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(projectMapper::toCommentResponse).toList();
    }

    @Transactional
    public CommentResponse addComment(UUID projectId, CommentRequest request, User author) {
        ProjectComment comment = ProjectComment.builder()
                .project(getOrThrow(projectId))
                .authorName(author.getFullName())
                .content(request.content().trim())
                .build();
        return projectMapper.toCommentResponse(commentRepository.save(comment));
    }

    /** Appelé quotidiennement : prévient des projets dont l'échéance tombe dans les 3 jours. */
    @Transactional
    public int notifyUpcomingDeadlines() {
        LocalDate today = LocalDate.now();
        List<Project> upcoming = projectRepository
                .findByDueDateBetweenAndStatusNot(today, today.plusDays(3), ProjectStatus.COMPLETED);
        int created = 0;
        for (Project project : upcoming) {
            boolean sent = notificationService.notifyOnce(NotificationType.DEADLINE_NEAR,
                    "Échéance proche : « " + project.getName() + " » le " + project.getDueDate().format(DATE_FORMAT),
                    "/projets/" + project.getId());
            if (sent) {
                created++;
            }
        }
        return created;
    }

    private Customer getCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }

    private Project getOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + id));
    }
}
