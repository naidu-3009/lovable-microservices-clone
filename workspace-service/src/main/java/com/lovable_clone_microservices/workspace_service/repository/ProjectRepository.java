package com.lovable_clone_microservices.workspace_service.repository;


import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import com.lovable_clone_microservices.workspace_service.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {

    @Query("""
            SELECT p as project, pm.projectMemberRole as role
            FROM Project p
            JOIN ProjectMember pm ON pm.project.id = p.id
            WHERE pm.id.userId = :userId
              AND p.deletedAt IS NULL
            ORDER BY p.updatedAt DESC
""")
    List<ProjectWithRole> findAllAccessibleByUser(@Param("userId")Long userId);

@Query("""
   SELECT p from Project p 
   WHERE p.id=:id
     AND p.deletedAt is NULL
     AND EXISTS(
            SELECT 1 FROM ProjectMember pm
            where pm.id.userId=:userId
            AND pm.id.projectId=p.id
        )
""")
Optional<Project> findAllAccessibleByUserId(@Param("id") Long id, @Param("userId") Long userId);


    @Query("""
    SELECT p AS project, pm.projectMemberRole AS role
    FROM Project p
    JOIN ProjectMember pm ON pm.project.id = p.id
    WHERE p.id = :projectId
      AND pm.id.userId = :userId
      AND p.deletedAt IS NULL
""")
    Optional<ProjectWithRole> findAllAccessibleByUserIdWithRole(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );
interface ProjectWithRole{
    Project getProject();
    ProjectMemberRole getRole();
}


}
