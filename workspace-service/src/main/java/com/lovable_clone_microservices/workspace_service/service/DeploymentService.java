package com.lovable_clone_microservices.workspace_service.service;


import com.lovable_clone_microservices.workspace_service.dto.deploy.DeployResponse;

public interface DeploymentService {
    DeployResponse deploy(Long projectId);

}
