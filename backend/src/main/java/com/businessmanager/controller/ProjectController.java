package com.businessmanager.controller;

import com.businessmanager.dto.project.ProjectDtos.CommentRequest;
import com.businessmanager.dto.project.ProjectDtos.CommentResponse;
import com.businessmanager.dto.project.ProjectDtos.ProjectRequest;
import com.businessmanager.dto.project.ProjectDtos.ProjectResponse;
import com.businessmanager.dto.task.TaskDtos.TaskResponse;
import com.businessmanager.entity.ProjectStatus;
import com.businessmanager.entity.User;
import com.businessmanager.service.ProjectService;
import com.businessmanager.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projets")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    @GetMapping
    public Page<ProjectResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProjectStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.findAll(search, status, pageable);
    }

    @GetMapping("/{id}")
    public ProjectResponse findById(@PathVariable UUID id) {
        return projectService.findById(id);
    }

    @GetMapping("/{id}/tasks")
    public List<TaskResponse> tasks(@PathVariable UUID id) {
        projectService.findById(id); // 404 si le projet n'existe pas
        return taskService.findByProject(id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable UUID id) {
        return projectService.comments(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID id,
                                                      @Valid @RequestBody CommentRequest request,
                                                      @AuthenticationPrincipal User author) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addComment(id, request, author));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
