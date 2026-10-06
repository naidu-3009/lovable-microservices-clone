package com.lovable_clone_microservices.workspace_service.mapper;


import com.lovable_clone_microservices.workspace_service.dto.member.MemberResponse;
import com.lovable_clone_microservices.workspace_service.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {



    @Mapping(source = "user.id",target = "userId")
    @Mapping(source = "user.username",target = "username")
    @Mapping(source = "user.name",target = "name")
    @Mapping(source = "project.id",target = "projectId")
    @Mapping(source = "projectMemberRole",target = "role")
    MemberResponse toMemberResponseFromMember(ProjectMember member);



}
