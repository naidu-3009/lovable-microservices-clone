package com.lovable_clone_microservices.workspace_service.controller;



import com.lovable_clone_microservices.workspace_service.dto.member.InviteMemberRequest;
import com.lovable_clone_microservices.workspace_service.dto.member.MemberResponse;
import com.lovable_clone_microservices.workspace_service.dto.member.updateRoleRequest;
import com.lovable_clone_microservices.workspace_service.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class ProjectMemberController {

      ProjectMemberService projectMemberService;

   @GetMapping
    public ResponseEntity<List<MemberResponse>> getProjectMembers(@PathVariable Long projectId){
       return ResponseEntity.ok(projectMemberService.getProjectMembers(projectId));
   }

   @PostMapping
    public ResponseEntity<MemberResponse> inviteMember(
            @PathVariable Long projectId,
            @RequestBody @Valid InviteMemberRequest request
   ){
       return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.inviteMember(projectId,request));
   }

   @PatchMapping("/{memberId}") //pass userid here in place of memberid
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestBody  @Valid updateRoleRequest request
   ){
       return ResponseEntity.ok(projectMemberService.updateMemberRole(projectId,memberId,request));
   }

   @DeleteMapping("/{memberId}")//pass userid here in place of memberid
    public ResponseEntity<Void> deleteMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId
   ){
       projectMemberService.removeProjectMember(projectId,memberId);
       return ResponseEntity.noContent().build();
   }

}
