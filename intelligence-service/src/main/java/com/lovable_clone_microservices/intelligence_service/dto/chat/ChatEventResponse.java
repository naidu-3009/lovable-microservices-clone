package com.lovable_clone_microservices.intelligence_service.dto.chat;

import com.lovable_clone_microservices.common_library.enums.ChatEventType;
import jakarta.persistence.Id;

public record ChatEventResponse(@Id
                                Long id,
                                ChatEventType type,
                                Integer sequenceOrder,
                                String content,
                                String metadata,
                                String filePath//null unless file edit
) {
}
