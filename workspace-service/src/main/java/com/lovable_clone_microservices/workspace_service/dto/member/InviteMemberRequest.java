package com.lovable_clone_microservices.workspace_service.dto.member;

import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest (
        @NotNull String username,
        @NotNull ProjectMemberRole role){}
