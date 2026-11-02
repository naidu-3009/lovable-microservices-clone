package com.lovable_clone_microservices.workspace_service.service.impl;

import com.lovable_clone_microservices.common_library.dto.UserDto;
import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.workspace_service.client.AccountClient;
import com.lovable_clone_microservices.workspace_service.dto.member.InviteMemberRequest;
import com.lovable_clone_microservices.workspace_service.dto.member.MemberResponse;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import com.lovable_clone_microservices.workspace_service.entity.ProjectMember;
import com.lovable_clone_microservices.workspace_service.mapper.ProjectMemberMapper;
import com.lovable_clone_microservices.workspace_service.repository.ProjectMemberRepository;
import com.lovable_clone_microservices.workspace_service.repository.ProjectRepository;
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
class ProjectMemberServiceImplTest {

    @Mock ProjectMemberRepository projectMemberRepository;
    @Mock ProjectRepository projectRepository;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock AuthUtil authUtil;
    @Mock AccountClient accountClient;

    @InjectMocks ProjectMemberServiceImpl projectMemberService;

    @Test
    void inviteMember_shouldCreateMembershipAndReturnMemberDetails() {
        Long projectId = 7L;
        Long ownerId = 10L;
        Long inviteeId = 20L;
        Project project = Project.builder().id(projectId).name("Demo").build();
        UserDto invitee = new UserDto(inviteeId, "invitee@example.com", "Invitee");
        MemberResponse mapped = new MemberResponse(inviteeId, null, null, null, ProjectMemberRole.EDITOR, "7");
        when(authUtil.getCurrentUserId()).thenReturn(ownerId);
        when(projectRepository.findAllAccessibleByUserId(projectId, ownerId)).thenReturn(Optional.of(project));
        when(accountClient.getUserByEmail("invitee@example.com")).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsById(any())).thenReturn(false);
        when(projectMemberMapper.toMemberResponseFromMember(any(ProjectMember.class))).thenReturn(mapped);
        when(accountClient.getUserById(inviteeId)).thenReturn(invitee);

        MemberResponse response = projectMemberService.inviteMember(
                projectId, new InviteMemberRequest("invitee@example.com", ProjectMemberRole.EDITOR));

        assertThat(response.userId()).isEqualTo(inviteeId);
        assertThat(response.username()).isEqualTo("invitee@example.com");
        assertThat(response.name()).isEqualTo("Invitee");
        assertThat(response.role()).isEqualTo(ProjectMemberRole.EDITOR);

        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getProjectMemberId().getProjectId()).isEqualTo(projectId);
        assertThat(captor.getValue().getProjectMemberId().getUserId()).isEqualTo(inviteeId);
        assertThat(captor.getValue().getProjectMemberRole()).isEqualTo(ProjectMemberRole.EDITOR);
        assertThat(captor.getValue().getInvitedAt()).isNotNull();
    }

    @Test
    void inviteMember_shouldRejectInvitingCurrentUser() {
        Long projectId = 7L;
        Long currentUserId = 10L;
        Project project = Project.builder().id(projectId).build();
        UserDto currentUser = new UserDto(currentUserId, "owner@example.com", "Owner");
        when(authUtil.getCurrentUserId()).thenReturn(currentUserId);
        when(projectRepository.findAllAccessibleByUserId(projectId, currentUserId)).thenReturn(Optional.of(project));
        when(accountClient.getUserByEmail("owner@example.com")).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> projectMemberService.inviteMember(
                projectId, new InviteMemberRequest("owner@example.com", ProjectMemberRole.EDITOR)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invite yourself");

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void inviteMember_shouldRejectDuplicateMembership() {
        Long projectId = 7L;
        Long ownerId = 10L;
        Long inviteeId = 20L;
        Project project = Project.builder().id(projectId).build();
        UserDto invitee = new UserDto(inviteeId, "invitee@example.com", "Invitee");
        when(authUtil.getCurrentUserId()).thenReturn(ownerId);
        when(projectRepository.findAllAccessibleByUserId(projectId, ownerId)).thenReturn(Optional.of(project));
        when(accountClient.getUserByEmail("invitee@example.com")).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsById(any())).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.inviteMember(
                projectId, new InviteMemberRequest("invitee@example.com", ProjectMemberRole.VIEWER)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("multiple times");

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void inviteMember_shouldFailWhenAccountDoesNotExist() {
        Long projectId = 7L;
        Long ownerId = 10L;
        Project project = Project.builder().id(projectId).build();
        when(authUtil.getCurrentUserId()).thenReturn(ownerId);
        when(projectRepository.findAllAccessibleByUserId(projectId, ownerId)).thenReturn(Optional.of(project));
        when(accountClient.getUserByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.inviteMember(
                projectId, new InviteMemberRequest("missing@example.com", ProjectMemberRole.VIEWER)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
}
