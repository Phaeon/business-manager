package com.businessmanager.mapper;

import com.businessmanager.dto.project.ProjectDtos.CommentResponse;
import com.businessmanager.dto.project.ProjectDtos.ProjectResponse;
import com.businessmanager.entity.Project;
import com.businessmanager.entity.ProjectComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    ProjectResponse toResponse(Project project);

    CommentResponse toCommentResponse(ProjectComment comment);
}
