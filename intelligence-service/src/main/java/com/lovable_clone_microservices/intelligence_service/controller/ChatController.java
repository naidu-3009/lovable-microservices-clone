package com.lovable_clone_microservices.intelligence_service.controller;



import com.lovable_clone_microservices.intelligence_service.dto.chat.ChatRequest;
import com.lovable_clone_microservices.intelligence_service.dto.chat.ChatResponse;
import com.lovable_clone_microservices.intelligence_service.dto.chat.StreamResponse;
import com.lovable_clone_microservices.intelligence_service.service.AiGenerationService;
import com.lovable_clone_microservices.intelligence_service.service.ChatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intelligence")
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class ChatController {
    AiGenerationService aiGenerationService;
    ChatService chatService;

    @PostMapping(value = "/chat/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamResponse>> streamChat(
            @RequestBody ChatRequest request
    ){
        return aiGenerationService.streamResponse(request.message(),request.projectId())
                .map(data-> ServerSentEvent.<StreamResponse>builder()
                        .data(data)
                        .build());

    }


    @GetMapping("/chat/projects/{projectId}")
    public ResponseEntity<List<ChatResponse>> getChatHistory(
            @PathVariable Long projectId
    ){
        return ResponseEntity.ok(chatService.getProjectChatHistory(projectId));

    }
}

