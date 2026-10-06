package com.lovable_clone_microservices.workspace_service.repository;


import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import com.lovable_clone_microservices.workspace_service.entity.ProjectMember;
import com.lovable_clone_microservices.workspace_service.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {




    List<ProjectMember> findByProjectId(Long projectId);


    @Query("""
            SELECT pm.projectMemberRole FROM ProjectMember pm
            WHERE pm.projectMemberId.projectId=:projectId AND pm.projectMemberId.userId=:userId
            """)
    Optional<ProjectMemberRole> findRoleByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);



    @Query("""
            SELECT COUNT(pm) FROM ProjectMember pm
    WHERE pm.projectMemberId.userId = :userId
      AND pm.projectMemberRole = 'OWNER'
            """)
    int countProjectOwnedByUser(@Param("userId")Long userId);

}
