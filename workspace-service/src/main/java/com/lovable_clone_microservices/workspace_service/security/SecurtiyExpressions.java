package com.lovable_clone_microservices.workspace_service.security;

import com.lovable_clone_microservices.common_library.enums.ProjectPerimission;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.workspace_service.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor
public class SecurtiyExpressions {

    private final ProjectMemberRepository projectMemberRepository;
    private final AuthUtil authUtil;


    public boolean hasPermission(Long projectId, ProjectPerimission projectPerimission){
        Long userId=authUtil.getCurrentUserId();
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId,userId).map(role -> role.getPermissions().contains(projectPerimission)).orElse(false);
    }

    public boolean canViewProject(Long projectId){
        return hasPermission(projectId, ProjectPerimission.VIEW);
    }


    public boolean canEditProject(Long projectId){
        return hasPermission(projectId, ProjectPerimission.EDIT);
    }


    public boolean canDeleteProject(Long projectId){
        return hasPermission(projectId, ProjectPerimission.DELETE);
    }

    public boolean canViewMembers(Long projectId){
        return hasPermission(projectId, ProjectPerimission.VIEW_MEMBERS);
    }

    public boolean canManageMembers(Long projectId){
        return hasPermission(projectId, ProjectPerimission.MANAGE_MEMBERS);
    }





}
