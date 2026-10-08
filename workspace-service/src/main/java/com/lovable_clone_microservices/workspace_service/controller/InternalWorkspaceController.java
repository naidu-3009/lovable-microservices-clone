package com.lovable_clone_microservices.workspace_service.controller;

import com.lovable_clone_microservices.common_library.enums.ProjectPerimission;
import com.lovable_clone_microservices.workspace_service.dto.projects.FileTreeResponse;
import com.lovable_clone_microservices.workspace_service.service.ProjectFileService;
import com.lovable_clone_microservices.workspace_service.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/internal/v1/")
@RestController
public class InternalWorkspaceController {
    private final ProjectService projectService;
    private final ProjectFileService projectFileService;

    @GetMapping("/projects/{projectId}/files/tree")
    public FileTreeResponse getFileTree(@PathVariable Long projectId) {
        return projectFileService.getFileTree(projectId);
    }

    @GetMapping("/projects/{projectId}/files/content")
    public String getFileContent(@PathVariable Long projectId, @RequestParam String path) {
        return projectFileService.getFileContent(projectId, path).toString();
    }

    @GetMapping("/projects/{projectId}/permissions/check")
    public boolean checkProjectPermission(
            @PathVariable Long projectId,
            @RequestParam ProjectPerimission permission) {
        return projectService.hasPermission(projectId, permission);
    }
}
