package com.lovable_clone_microservices.intelligence_service.dto.chat;

public record ChatRequest(
        String message,Long projectId
) {
}
