package com.lovable_clone_microservices.intelligence_service.mapper;

import com.lovable_clone_microservices.intelligence_service.dto.chat.ChatResponse;
import com.lovable_clone_microservices.intelligence_service.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {


     List<ChatResponse> toChatResponse(List<ChatMessage> chatMessages);
}
