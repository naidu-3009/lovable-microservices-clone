package com.lovable_clone_microservices.workspace_service.service.impl;


import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.workspace_service.dto.projects.FileContentResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.FileNode;
import com.lovable_clone_microservices.workspace_service.dto.projects.FileTreeResponse;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import com.lovable_clone_microservices.workspace_service.entity.ProjectFile;
import com.lovable_clone_microservices.workspace_service.mapper.ProjectFileMapper;
import com.lovable_clone_microservices.workspace_service.repository.ProjectFileRepository;
import com.lovable_clone_microservices.workspace_service.repository.ProjectRepository;
import com.lovable_clone_microservices.workspace_service.service.ProjectFileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final MinioClient minioClient;
    private final ProjectFileMapper projectFileMapper;

    @Value("${minio.project-bucket}")
    private String projectBucket;

    @Override
    public FileTreeResponse getFileTree(Long projectId) {

        List<ProjectFile> projectFileList = projectFileRepository.findByProjectId(projectId);
        List<FileNode> fileTreeList = projectFileMapper.toFileNode(projectFileList);

        return new FileTreeResponse(fileTreeList);
    }

    @Override
    public FileContentResponse getFileContent(Long projectId, String path) {
        String objectName = projectId + "/" + path;
        try (
                InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket("projectslovable")
                                .object(objectName)
                                .build())) {

            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new FileContentResponse(path, content);
        } catch (Exception e) {
            log.error("Failed to read file: {}/{}", projectId, path, e);
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    @Override
    @Transactional
    public void saveFile(Long projectId, String filePath, String fileContent) {
        log.info("saving the filepath{}", filePath);
        //we are saving our file metadata in our postgresdb and upload our file content to min io
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", projectId.toString())
        );


        String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        String objectKey = projectId + "/" + cleanPath;


        try {
            byte[] contentBytes = fileContent.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(contentBytes);
            // saving the file content to minio
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(projectBucket)
                            .object(objectKey)
                            .stream(inputStream, contentBytes.length, -1)
                            .contentType(determineContentType(filePath))
                            .build());

            // Saving the metaData
            ProjectFile file = projectFileRepository.findByProjectIdAndPath(projectId, cleanPath)
                    .orElseGet(() -> ProjectFile.builder()
                            .project(project)
                            .path(cleanPath)
                            .minioObjectKey(objectKey) // Use the key we generated
                            .createdAt(Instant.now())
                            .build());

            file.setUpdatedAt(Instant.now());
            projectFileRepository.save(file);
            log.info("Saved file: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to save file {}/{}", projectId, cleanPath, e);
            throw new RuntimeException("File save failed", e);
        }


    }


    @Override
    public byte[] downloadProjectZip(Long projectId) {
        List<ProjectFile> files =
                projectFileRepository.findByProjectId(projectId)
                        .stream()
                        .filter(file -> file.getDeletedAt() == null)
                        .toList();

        if (files.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Project files", projectId.toString());
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Set<String> addedEntries = new HashSet<>();

        try (ZipOutputStream zip =
                     new ZipOutputStream(output, StandardCharsets.UTF_8)) {

            for (ProjectFile file : files) {
                String entryName = toSafeZipEntryName(file.getPath());

                if (!addedEntries.add(entryName)) {
                    continue;
                }

                String objectKey =
                        file.getMinioObjectKey() != null
                                && !file.getMinioObjectKey().isBlank()
                                ? file.getMinioObjectKey()
                                : projectId + "/" + file.getPath();

                try (InputStream input = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(projectBucket)
                                .object(objectKey)
                                .build())) {

                    zip.putNextEntry(new ZipEntry(entryName));
                    input.transferTo(zip);
                    zip.closeEntry();
                }
            }

        } catch (Exception e) {
            log.error(
                    "Failed to create ZIP archive for project {}",
                    projectId, e);

            throw new RuntimeException(
                    "Failed to create project ZIP archive", e);
        }

        return output.toByteArray();
    }

    private String toSafeZipEntryName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Project contains a file with an empty path");
        }

        String normalizedInput = filePath
                .replace('\\', '/')
                .replaceAll("^/+", "");

        if (normalizedInput.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException(
                    "Absolute file paths cannot be exported");
        }

        Path normalizedPath = Path.of(normalizedInput).normalize();

        if (normalizedPath.isAbsolute()
                || normalizedPath.startsWith("..")
                || normalizedPath.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "Unsafe project file path: " + filePath);
        }

        return normalizedPath.toString().replace('\\', '/');
    }


    private String determineContentType(String filePath) {
        String type = URLConnection.guessContentTypeFromName(filePath);//we are asking java do you this file type if it fails we will find it manually from seeing the path
        if (type != null) return type;
        if (filePath.endsWith(".jsx") || filePath.endsWith(".ts") || filePath.endsWith(".tsx"))
            return "text/javascript";
        if (filePath.endsWith(".json")) return "application/json";
        if (filePath.endsWith(".css")) return "text/css";

        return "text/plain";
    }


}
