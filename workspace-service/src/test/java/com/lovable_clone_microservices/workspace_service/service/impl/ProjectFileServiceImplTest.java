package com.lovable_clone_microservices.workspace_service.service.impl;

import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.workspace_service.dto.projects.FileContentResponse;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import com.lovable_clone_microservices.workspace_service.entity.ProjectFile;
import com.lovable_clone_microservices.workspace_service.mapper.ProjectFileMapper;
import com.lovable_clone_microservices.workspace_service.repository.ProjectFileRepository;
import com.lovable_clone_microservices.workspace_service.repository.ProjectRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectFileServiceImplTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    ProjectFileRepository projectFileRepository;
    @Mock
    MinioClient minioClient;
    @Mock
    ProjectFileMapper projectFileMapper;

    @InjectMocks
    ProjectFileServiceImpl projectFileService;

    @Test
    void saveFile_shouldUploadContentAndPersistFileMetadata() throws Exception {
        ReflectionTestUtils.setField(projectFileService, "projectBucket", "ideastitch-projects");
        Project project = Project.builder().id(7L).name("Demo").build();
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(projectFileRepository.findByProjectIdAndPath(7L, "src/App.tsx"))
                .thenReturn(Optional.empty());

        projectFileService.saveFile(7L, "/src/App.tsx", "export default 1;");

        ArgumentCaptor<PutObjectArgs> putCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(putCaptor.capture());
        assertThat(putCaptor.getValue().bucket()).isEqualTo("ideastitch-projects");
        assertThat(putCaptor.getValue().object()).isEqualTo("7/src/App.tsx");

        ArgumentCaptor<ProjectFile> fileCaptor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileRepository).save(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getProject()).isSameAs(project);
        assertThat(fileCaptor.getValue().getPath()).isEqualTo("src/App.tsx");
        assertThat(fileCaptor.getValue().getMinioObjectKey()).isEqualTo("7/src/App.tsx");
        assertThat(fileCaptor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void saveFile_shouldUpdateExistingMetadataInsteadOfCreatingAnotherRecord() throws Exception {
        ReflectionTestUtils.setField(projectFileService, "projectBucket", "ideastitch-projects");
        Project project = Project.builder().id(7L).name("Demo").build();
        ProjectFile existing = ProjectFile.builder()
                .id(88L).project(project).path("src/App.tsx").minioObjectKey("7/src/App.tsx")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(projectFileRepository.findByProjectIdAndPath(7L, "src/App.tsx"))
                .thenReturn(Optional.of(existing));

        projectFileService.saveFile(7L, "src/App.tsx", "updated content");

        verify(projectFileRepository).save(existing);
        assertThat(existing.getId()).isEqualTo(88L);
        assertThat(existing.getUpdatedAt()).isNotNull();
    }

    @Test
    void saveFile_shouldFailWhenProjectDoesNotExist() {
        when(projectRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectFileService.saveFile(7L, "src/App.tsx", "content"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(minioClient, projectFileRepository);
    }

    @Test
    void getFileContent_shouldReturnUtf8FileContent() throws Exception {
        byte[] bytes = "hello IdeaStitch".getBytes(StandardCharsets.UTF_8);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(minioResponse(bytes));

        FileContentResponse response = projectFileService.getFileContent(7L, "src/App.tsx");

        assertThat(response.path()).isEqualTo("src/App.tsx");
        assertThat(response.content()).isEqualTo("hello IdeaStitch");
        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void downloadProjectZip_shouldIncludeActiveFilesAndTheirContents() throws Exception {
        ReflectionTestUtils.setField(
                projectFileService,
                "projectBucket",
                "projectslovable"
        );
        ProjectFile active = ProjectFile.builder()
                .id(1L).path("src/App.tsx").minioObjectKey("7/src/App.tsx").build();
        ProjectFile deleted = ProjectFile.builder()
                .id(2L).path("old/Old.tsx").minioObjectKey("7/old/Old.tsx")
                .deletedAt(Instant.now()).build();
        when(projectFileRepository.findByProjectId(7L)).thenReturn(List.of(active, deleted));
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(minioResponse(
                        "export default 1;".getBytes(StandardCharsets.UTF_8)
                ));
        byte[] zipBytes = projectFileService.downloadProjectZip(7L);

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            var entry = zip.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("src/App.tsx");
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            zip.transferTo(content);
            assertThat(content.toString(StandardCharsets.UTF_8)).isEqualTo("export default 1;");
            assertThat(zip.getNextEntry()).isNull();
        }
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }

    @Test
    void downloadProjectZip_shouldRejectEmptyProject() {
        when(projectFileRepository.findByProjectId(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> projectFileService.downloadProjectZip(7L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(minioClient);
    }

    @Test
    void downloadProjectZip_shouldRejectDirectoryTraversalPath() {
        ProjectFile unsafe = ProjectFile.builder()
                .id(1L).path("../../etc/passwd").minioObjectKey("7/../../etc/passwd").build();
        when(projectFileRepository.findByProjectId(7L)).thenReturn(List.of(unsafe));

        assertThatThrownBy(() -> projectFileService.downloadProjectZip(7L))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(minioClient);
    }

    private GetObjectResponse minioResponse(byte[] content) {
        return new GetObjectResponse(
                new Headers.Builder().build(),
                "projectslovable",
                "us-east-1",
                "7/src/App.tsx",
                new ByteArrayInputStream(content)
        );
    }
}
