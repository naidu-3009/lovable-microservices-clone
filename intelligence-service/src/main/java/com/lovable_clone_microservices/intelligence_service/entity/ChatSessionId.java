package com.lovable_clone_microservices.intelligence_service.entity;

import lombok.*;

import java.io.Serializable;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ChatSessionId implements Serializable {
    Long projectId;
    Long userId;
}
