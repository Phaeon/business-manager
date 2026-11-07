package com.businessmanager.repository;

import com.businessmanager.entity.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectCommentRepository extends JpaRepository<ProjectComment, UUID> {

    List<ProjectComment> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
