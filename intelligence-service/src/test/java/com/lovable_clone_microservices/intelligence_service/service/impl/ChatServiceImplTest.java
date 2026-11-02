package com.lovable_clone_microservices.intelligence_service.service.impl;

import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.intelligence_service.dto.chat.ChatResponse;
import com.lovable_clone_microservices.intelligence_service.entity.ChatMessage;
import com.lovable_clone_microservices.intelligence_service.entity.ChatSession;
import com.lovable_clone_microservices.intelligence_service.entity.ChatSessionId;
import com.lovable_clone_microservices.intelligence_service.mapper.ChatMapper;
import com.lovable_clone_microservices.intelligence_service.repository.ChatMessageRepository;
import com.lovable_clone_microservices.intelligence_service.repository.ChatSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock AuthUtil authUtil;
    @Mock ChatSessionRepository chatSessionRepository;
    @Mock ChatMapper chatMapper;

    @InjectMocks ChatServiceImpl chatService;

    @Test
    void getProjectChatHistory_shouldLoadSessionForAuthenticatedUserAndMapMessages() {
        Long projectId = 7L;
        Long userId = 11L;
        ChatSession session = ChatSession.builder()
                .chatSessionId(new ChatSessionId(projectId, userId)).build();
        List<ChatMessage> messages = List.of(new ChatMessage(), new ChatMessage());
        ChatResponse mappedOne = new ChatResponse(1L, "hello", 10, null, null, List.of());
        ChatResponse mappedTwo = new ChatResponse(2L, "hi", 12, null, null, List.of());
        when(authUtil.getCurrentUserId()).thenReturn(userId);
        when(chatSessionRepository.getReferenceById(any(ChatSessionId.class))).thenReturn(session);
        when(chatMessageRepository.findByChatSession(session)).thenReturn(messages);
        when(chatMapper.toChatResponse(messages)).thenReturn(List.of(mappedOne, mappedTwo));

        List<ChatResponse> result = chatService.getProjectChatHistory(projectId);

        assertThat(result).containsExactly(mappedOne, mappedTwo);
        ArgumentCaptor<ChatSessionId> idCaptor = ArgumentCaptor.forClass(ChatSessionId.class);
        verify(chatSessionRepository).getReferenceById(idCaptor.capture());
        assertThat(idCaptor.getValue().getProjectId()).isEqualTo(projectId);
        assertThat(idCaptor.getValue().getUserId()).isEqualTo(userId);
        verify(chatMessageRepository).findByChatSession(session);
        verify(chatMapper).toChatResponse(messages);
    }

    @Test
    void getProjectChatHistory_shouldReturnEmptyMappedHistoryWhenNoMessagesExist() {
        Long projectId = 7L;
        Long userId = 11L;
        ChatSession session = ChatSession.builder()
                .chatSessionId(new ChatSessionId(projectId, userId)).build();
        when(authUtil.getCurrentUserId()).thenReturn(userId);
        when(chatSessionRepository.getReferenceById(any(ChatSessionId.class))).thenReturn(session);
        when(chatMessageRepository.findByChatSession(session)).thenReturn(List.of());
        when(chatMapper.toChatResponse(List.of())).thenReturn(List.of());

        assertThat(chatService.getProjectChatHistory(projectId)).isEmpty();
    }
}
