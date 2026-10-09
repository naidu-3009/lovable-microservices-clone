package com.lovable_clone_microservices.workspace_service.service.impl;


import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import com.lovable_clone_microservices.common_library.enums.ProjectPerimission;
import com.lovable_clone_microservices.common_library.error.BadRequestException;
import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.workspace_service.client.AccountClient;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectRequest;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectResponse;
import com.lovable_clone_microservices.workspace_service.dto.projects.ProjectSummaryResponse;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import com.lovable_clone_microservices.workspace_service.entity.ProjectMember;
import com.lovable_clone_microservices.workspace_service.entity.ProjectMemberId;
import com.lovable_clone_microservices.workspace_service.mapper.ProjectMapper;
import com.lovable_clone_microservices.workspace_service.repository.ProjectMemberRepository;
import com.lovable_clone_microservices.workspace_service.repository.ProjectRepository;
import com.lovable_clone_microservices.workspace_service.security.SecurityExpressions;
import com.lovable_clone_microservices.workspace_service.service.ProjectService;
import com.lovable_clone_microservices.workspace_service.service.ProjectTemplateService;
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
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;
    SecurityExpressions securityExpressions;


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
    public boolean hasPermission(Long projectId, ProjectPerimission permission) {
        return securityExpressions.hasPermission(projectId, permission);
    }

    @Override
        public ProjectResponse createProject(ProjectRequest request) {

        if(!canCreateProject()){
            throw new BadRequestException("user cannot create a new project with current plan ,upgrade plan asap");
        }


        Long userId= authUtil.getCurrentUserId();

        Project project=Project.builder().name(request.name()).isPublic(false).build();
        project = projectRepository.save(project);
        ProjectMemberId projectMemberId=new ProjectMemberId(project.getId(),userId);
        ProjectMember projectMember= ProjectMember.builder().projectMemberRole(ProjectMemberRole.OWNER).acceptedAt(Instant.now()).invitedAt(Instant.now()).projectMemberId(projectMemberId).project(project).build();
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

    private boolean canCreateProject() {
        Long userId = authUtil.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();

        int maxAllowed = plan.getMaxProjects();
        int ownedCount = projectMemberRepository.countProjectOwnedByUser(userId);

        return ownedCount < maxAllowed;
    }

}
