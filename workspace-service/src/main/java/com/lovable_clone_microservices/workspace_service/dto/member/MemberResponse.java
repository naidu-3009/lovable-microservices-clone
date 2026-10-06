package com.lovable_clone_microservices.workspace_service.dto.member;


import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;

public record MemberResponse(
        Long userId,
        String username,
        String name,
        String avatarUrl,
        ProjectMemberRole role,
        String projectId
) {
}
