package com.lovable_clone_microservices.workspace_service.service.impl;


import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.workspace_service.dto.member.InviteMemberRequest;
import com.lovable_clone_microservices.workspace_service.dto.member.MemberResponse;
import com.lovable_clone_microservices.workspace_service.dto.member.updateRoleRequest;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import com.lovable_clone_microservices.workspace_service.mapper.ProjectMemberMapper;
import com.lovable_clone_microservices.workspace_service.repository.ProjectMemberRepository;
import com.lovable_clone_microservices.workspace_service.repository.ProjectRepository;
import com.lovable_clone_microservices.workspace_service.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;


@Service
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {
    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    UserRepository userRepository;
    AuthUtil authUtil;

    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
        Long userId= authUtil.getCurrentUserId();
        Project project=getUserProjectByIdInternal(projectId);
        return projectMemberRepository.findByProjectId(projectId).stream().map(projectMemberMapper::toMemberResponseFromMember).toList();
    }


    @Override
    @PreAuthorize(("@security.canManageMembers(#projectId)"))
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) throws RuntimeException{
        Long userId= authUtil.getCurrentUserId();
        Project project = getUserProjectByIdInternal(projectId);

        User invitee=userRepository.findByUsername(request.username()).orElseThrow();

        if(invitee.getId().equals(userId))
            throw new RuntimeException("you are not allowed to invite yourself");

        ProjectMemberId projectMemberId=new ProjectMemberId(projectId,invitee.getId());
        if(projectMemberRepository.existsById(projectMemberId))
            throw new RuntimeException("you are not allowed to invite multiple times");

        ProjectMember member=ProjectMember.builder().projectMemberId(projectMemberId).projectMemberRole(request.role()) .invitedAt(Instant.now()).project(project).build();

        projectMemberRepository.save(member);
        return projectMemberMapper.toMemberResponseFromMember(member);

    }

    @Override
    @PreAuthorize(("@security.canManageMembers(#projectId)"))
    public MemberResponse updateMemberRole(Long projectId, Long memberId, updateRoleRequest request) {
        Long userId= authUtil.getCurrentUserId();
        Project project=getUserProjectByIdInternal(projectId);

        ProjectMemberId projectMemberId=new ProjectMemberId(projectId,memberId);
        ProjectMember projectMember=projectMemberRepository.findById(projectMemberId).orElseThrow();

        projectMember.setProjectMemberRole(request.role());
        projectMemberRepository.save(projectMember);
       return projectMemberMapper.toMemberResponseFromMember(projectMember);
    }

    @Override
    @PreAuthorize(("@security.canManageMembers(#projectId)"))
    public void removeProjectMember(Long projectId, Long memberId) {
        Long userId= authUtil.getCurrentUserId();
        Project project=getUserProjectByIdInternal(projectId);


        ProjectMemberId projectMemberId=new ProjectMemberId(projectId,memberId);

        if(!(projectMemberRepository.existsById(projectMemberId))){
            throw new RuntimeException("the member doesnt even exits,hence cant delete");
        }
        projectMemberRepository.deleteById(projectMemberId);
        return ;
    }

    //internal use
    private Project getUserProjectByIdInternal(Long projectId){
        Long userId= authUtil.getCurrentUserId();
        Project project= projectRepository.findAllAccessibleByUserId(projectId,userId).orElseThrow();
        return project;
    }

}
