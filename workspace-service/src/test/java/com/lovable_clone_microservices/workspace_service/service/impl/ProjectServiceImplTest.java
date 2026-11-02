package com.lovable_clone_microservices.workspace_service.service.impl;

import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import com.lovable_clone_microservices.common_library.error.BadRequestException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.workspace_service.client.AccountClient;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectRequest;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectResponse;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import com.lovable_clone_microservices.workspace_service.entity.ProjectMember;
import com.lovable_clone_microservices.workspace_service.mapper.ProjectMapper;
import com.lovable_clone_microservices.workspace_service.repository.ProjectMemberRepository;
import com.lovable_clone_microservices.workspace_service.repository.ProjectRepository;
import com.lovable_clone_microservices.workspace_service.security.SecurityExpressions;
import com.lovable_clone_microservices.workspace_service.service.ProjectTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock ProjectRepository projectRepository;
    @Mock ProjectMapper projectMapper;
    @Mock ProjectMemberRepository projectMemberRepository;
    @Mock AuthUtil authUtil;
    @Mock ProjectTemplateService projectTemplateService;
    @Mock AccountClient accountClient;
    @Mock SecurityExpressions securityExpressions;

    @InjectMocks ProjectServiceImpl projectService;

    @Test
    void createProject_shouldCreateProjectAndAssignOwner() {
        Long userId = 42L;
        ProjectRequest request = new ProjectRequest("IdeaStitch Demo");
        PlanDto plan = new PlanDto();
        plan.setMaxProjects(5);
        when(authUtil.getCurrentUserId()).thenReturn(userId);
        when(accountClient.getCurrentSubscribedPlanByUser()).thenReturn(plan);
        when(projectMemberRepository.countProjectOwnedByUser(userId)).thenReturn(1);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        ProjectResponse expected = new ProjectResponse(100L, request.name(), null, null);
        when(projectMapper.toProjectResponse(any(Project.class))).thenReturn(expected);

        ProjectResponse response = projectService.createProject(request);

        assertThat(response).isEqualTo(expected);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getName()).isEqualTo("IdeaStitch Demo");
        assertThat(projectCaptor.getValue().getIsPublic()).isFalse();

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(memberCaptor.capture());
        ProjectMember owner = memberCaptor.getValue();
        assertThat(owner.getProjectMemberId().getProjectId()).isEqualTo(100L);
        assertThat(owner.getProjectMemberId().getUserId()).isEqualTo(userId);
        assertThat(owner.getProjectMemberRole()).isEqualTo(ProjectMemberRole.OWNER);
        assertThat(owner.getAcceptedAt()).isNotNull();
        verify(projectTemplateService).initializeProjectFromTemplate(100L);
    }

    @Test
    void createProject_shouldRejectWhenUserReachedProjectLimit() {
        PlanDto plan = new PlanDto();
        plan.setMaxProjects(2);
        when(authUtil.getCurrentUserId()).thenReturn(42L);
        when(accountClient.getCurrentSubscribedPlanByUser()).thenReturn(plan);
        when(projectMemberRepository.countProjectOwnedByUser(42L)).thenReturn(2);

        assertThatThrownBy(() -> projectService.createProject(new ProjectRequest("One too many")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot create");

        verify(projectRepository, never()).save(any(Project.class));
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        verifyNoInteractions(projectTemplateService);
    }

    @Test
    void updateProject_shouldUpdateAccessibleProjectName() {
        Project project = Project.builder().id(100L).name("Old name").isPublic(false).build();
        when(authUtil.getCurrentUserId()).thenReturn(42L);
        when(projectRepository.findAllAccessibleByUserId(100L, 42L)).thenReturn(Optional.of(project));
        ProjectResponse expected = new ProjectResponse(100L, "New name", null, null);
        when(projectMapper.toProjectResponse(project)).thenReturn(expected);

        ProjectResponse response = projectService.updateProject(100L, new ProjectRequest("New name"));

        assertThat(response).isEqualTo(expected);
        assertThat(project.getName()).isEqualTo("New name");
        verify(projectRepository).save(project);
    }

    @Test
    void softDelete_shouldSetDeletedAtForAccessibleProject() {
        Project project = Project.builder().id(100L).name("To delete").build();
        when(authUtil.getCurrentUserId()).thenReturn(42L);
        when(projectRepository.findAllAccessibleByUserId(100L, 42L)).thenReturn(Optional.of(project));

        projectService.softDelete(100L);

        assertThat(project.getDeletedAt()).isNotNull();
        verify(projectRepository).save(project);
    }
}
