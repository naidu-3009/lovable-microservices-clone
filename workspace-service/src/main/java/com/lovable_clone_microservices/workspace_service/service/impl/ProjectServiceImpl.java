package com.lovable_clone_microservices.workspace_service.service.impl;

import com.projectlove.lovable_clone.Services.ProjectService;
import com.projectlove.lovable_clone.Services.ProjectTemplateService;
import com.projectlove.lovable_clone.Services.SubscriptionService;
import com.projectlove.lovable_clone.dto.projects.ProjectRequest;
import com.projectlove.lovable_clone.dto.projects.ProjectResponse;
import com.projectlove.lovable_clone.dto.projects.ProjectSummaryResponse;
import com.projectlove.lovable_clone.entity.Project;
import com.projectlove.lovable_clone.entity.ProjectMember;
import com.projectlove.lovable_clone.entity.ProjectMemberId;
import com.projectlove.lovable_clone.entity.User;
import com.projectlove.lovable_clone.enums.ProjectMemberRole;
import com.projectlove.lovable_clone.error.BadRequestException;
import com.projectlove.lovable_clone.error.ResourceNotFoundException;
import com.projectlove.lovable_clone.mapper.ProjectMapper;
import com.projectlove.lovable_clone.repository.ProjectMemberRepository;
import com.projectlove.lovable_clone.repository.ProjectRepository;
import com.projectlove.lovable_clone.repository.UserRepository;
import com.projectlove.lovable_clone.security.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {
    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    SubscriptionService subscriptionService;
    ProjectTemplateService projectTemplateService;

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId= authUtil.getCurrentUserId();
        var projectWithRoles=projectRepository.findAllAccessibleByUser(userId);
        return projectWithRoles.stream().map(p->projectMapper.toProjectSummaryResponse(p.getProject(),p.getRole())).toList();
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {
        Long userId= authUtil.getCurrentUserId();
//        getUserProjectWithRoleIdInternal
        ProjectRepository.ProjectWithRole  projectWithRole = getUserProjectWithRoleIdInternal(projectId);
       return projectMapper.toProjectSummaryResponse(projectWithRole.getProject(),projectWithRole.getRole());
    }

    @Override
        public ProjectResponse createProject(ProjectRequest request) {

        if(!subscriptionService.canCreateNewProject()){
            throw new BadRequestException("user cannot create a new project with current plan ,upgrade plan asap");
        }


        Long userId= authUtil.getCurrentUserId();
        User owner=userRepository.findById(userId).orElseThrow(
                ()->new ResourceNotFoundException("user id not found",userId.toString())
        );
        Project project=Project.builder().name(request.name()).isPublic(false).build();
        project = projectRepository.save(project);
        ProjectMemberId projectMemberId=new ProjectMemberId(project.getId(),userId);
        ProjectMember projectMember=ProjectMember.builder().projectMemberRole(ProjectMemberRole.OWNER).user(owner).acceptedAt(Instant.now()).invitedAt(Instant.now()).projectMemberId(projectMemberId).project(project).build();
         projectMemberRepository.save(projectMember);
         projectTemplateService.initializeProjectFromTemplate(project.getId());

        return projectMapper.toProjectResponse(project);

    }

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Long userId= authUtil.getCurrentUserId();
        Project project=getUserProjectByIdInternal(projectId);

        project.setName(request.name());
//        projectRepository.save(project);(we can ignore this line cuz if we add @transactional tag it dirtychecks and updates the db) but no harm in writing
        projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#projectId)")
    public void softDelete(Long projectId) {
        Long userId= authUtil.getCurrentUserId();
        Project project=getUserProjectByIdInternal(projectId);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }


    //internal use
    private Project getUserProjectByIdInternal(Long projectId){
        Long userId= authUtil.getCurrentUserId();
        return projectRepository.findAllAccessibleByUserId(projectId,userId).orElseThrow(()->{return new ResourceNotFoundException("Project",projectId.toString());
        });
    }

    private ProjectRepository.ProjectWithRole getUserProjectWithRoleIdInternal(Long projectId){
        Long userId= authUtil.getCurrentUserId();
        return projectRepository.findAllAccessibleByUserIdWithRole(projectId,userId).orElseThrow(()-> new BadRequestException("Project Not found"));
    }

}
