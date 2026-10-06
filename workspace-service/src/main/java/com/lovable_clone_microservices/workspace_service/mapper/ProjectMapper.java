package com.lovable_clone_microservices.workspace_service.mapper;


import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectSummaryResponse;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    ProjectResponse toProjectResponse(Project project);

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "name")
    @Mapping(source = "project.createdAt", target = "createdAt")
    @Mapping(source = "project.updatedAt", target = "updatedAt")
    @Mapping(source = "role", target = "role")
    ProjectSummaryResponse toProjectSummaryResponse(Project project, ProjectMemberRole role);

    List<ProjectSummaryResponse> toListOfProjectSummaryResponse(List<Project> projects);


}