package com.lovable_clone_microservices.workspace_service.dto.member;


import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;

public record updateRoleRequest(ProjectMemberRole role) {
}
