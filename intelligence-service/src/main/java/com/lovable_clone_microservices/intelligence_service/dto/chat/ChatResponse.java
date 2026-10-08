package com.lovable_clone_microservices.intelligence_service.dto.chat;


import com.lovable_clone_microservices.common_library.enums.MessageRole;

import java.time.Instant;
import java.util.List;




public record ChatResponse (
    Long id,
    String content,
    Integer tokensUsed,
    Instant createdAt,
    MessageRole role,
    List<ChatEventResponse> events
    ){}
