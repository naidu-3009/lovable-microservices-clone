package com.lovable_clone_microservices.workspace_service.controller;


import com.lovable_clone_microservices.workspace_service.dto.projects.FileContentResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.FileTreeResponse;
import com.lovable_clone_microservices.workspace_service.service.ProjectFileService;
import com.lovable_clone_microservices.workspace_service.service.ProjectService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/projects/{projectId}/files")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FileController {
    ProjectFileService fileService;
    ProjectService projectService;

    @GetMapping
    public ResponseEntity<FileTreeResponse> getFileTree(@PathVariable Long projectId) {
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @GetMapping("/content")
    public ResponseEntity<FileContentResponse> getFile(@PathVariable Long projectId, @RequestParam String path) {
        return ResponseEntity.ok(fileService.getFileContent(projectId, path));
    }

    @GetMapping(value = "/download-zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadProjectZip(
            @PathVariable Long projectId) {

        // Verify the current user can access this project.
        var project = projectService.getUserProjectById(projectId);

        byte[] zipBytes = fileService.downloadProjectZip(projectId);

        String safeProjectName = project.name() == null
                ? ""
                : project.name()
                .replaceAll("[\\\\/:*?\"<>|]+", "-")
                .trim();

        String filename = safeProjectName.isBlank()
                ? "project-" + projectId + ".zip"
                : safeProjectName + ".zip";

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString())
                .contentLength(zipBytes.length)
                .body(zipBytes);
    }


}
