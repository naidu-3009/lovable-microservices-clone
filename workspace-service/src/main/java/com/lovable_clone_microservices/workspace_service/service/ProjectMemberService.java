package com.lovable_clone_microservices.workspace_service.service;



import com.lovable_clone_microservices.workspace_service.dto.member.InviteMemberRequest;
import com.lovable_clone_microservices.workspace_service.dto.member.MemberResponse;
import com.lovable_clone_microservices.workspace_service.dto.member.updateRoleRequest;

import java.util.List;

public interface ProjectMemberService {
       List<MemberResponse> getProjectMembers(Long projectId) ;

         MemberResponse inviteMember(Long projectId, InviteMemberRequest request);

    MemberResponse updateMemberRole(Long projectId, Long memberId, updateRoleRequest request);

    void removeProjectMember(Long projectId, Long memberId);
}
