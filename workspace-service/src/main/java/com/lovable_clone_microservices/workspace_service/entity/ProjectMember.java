package com.lovable_clone_microservices.workspace_service.entity;

import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "project_member")
public class ProjectMember {
    @EmbeddedId
     ProjectMemberId projectMemberId;

    @Enumerated(EnumType.STRING)
     @Column(nullable = false)
    ProjectMemberRole projectMemberRole;



     @ManyToOne
     @MapsId("projectId")
     Project project;

     Instant invitedAt;
     Instant acceptedAt;

}
