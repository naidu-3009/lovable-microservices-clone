package com.lovable_clone_microservices.workspace_service.service;



import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectRequest;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getUserProjects();
    ProjectSummaryResponse getUserProjectById(Long projectId);
    ProjectResponse createProject(ProjectRequest request);
    ProjectResponse updateProject(Long projectId, ProjectRequest request);
    void softDelete(Long projectId);
}
