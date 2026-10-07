package com.lovable_clone_microservices.workspace_service.controller;

import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.workspace_service.dto.deploy.DeployResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectRequest;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectSummaryResponse;
import com.lovable_clone_microservices.workspace_service.service.DeploymentService;
import com.lovable_clone_microservices.workspace_service.service.ProjectService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class ProjectController {
     ProjectService projectService;
     DeploymentService deploymentService;
     AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects(){

        return ResponseEntity.ok(projectService.getUserProjects());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectSummaryResponse> getProjectById(@PathVariable Long projectId){
        return ResponseEntity.ok(projectService.getUserProjectById(projectId));
    }


    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }


    @PatchMapping("/{projectId}")
    public  ResponseEntity<ProjectResponse> updateProject(@PathVariable Long projectId,@RequestBody @Valid ProjectRequest request){
        return ResponseEntity.ok(projectService.updateProject(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId){
        projectService.softDelete(projectId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/deploy")
    public ResponseEntity<DeployResponse> deployProject(@PathVariable Long id){
        return ResponseEntity.ok(deploymentService.deploy(id));
    }

}
