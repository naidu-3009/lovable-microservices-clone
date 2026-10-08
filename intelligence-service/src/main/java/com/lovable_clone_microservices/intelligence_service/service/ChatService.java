package com.lovable_clone_microservices.intelligence_service.service;


import com.lovable_clone_microservices.intelligence_service.dto.chat.ChatResponse;

import java.util.List;

public interface ChatService {

    List<ChatResponse> getProjectChatHistory(Long projectId);


}
