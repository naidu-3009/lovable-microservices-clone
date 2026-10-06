package com.lovable_clone_microservices.workspace_service.service;


import com.lovable_clone_microservices.workspace_service.dto.projects.FileContentResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.FileTreeResponse;

public interface ProjectFileService {
         FileTreeResponse getFileTree(Long projectId);

    FileContentResponse getFileContent(Long projectId, String path);

    void saveFile(Long projectId, String filePath, String fileContent);
}
