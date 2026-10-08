package com.lovable_clone_microservices.intelligence_service.service;

import com.lovable_clone_microservices.intelligence_service.dto.chat.StreamResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service

public interface AiGenerationService {
    Flux<StreamResponse> streamResponse(String message, Long aLong);
}
