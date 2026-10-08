package com.lovable_clone_microservices.intelligence_service.service.impl;


import com.lovable_clone_microservices.intelligence_service.client.WorkspaceClient;
import com.lovable_clone_microservices.common_library.enums.ChatEventType;
import com.lovable_clone_microservices.common_library.enums.MessageRole;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.intelligence_service.dto.chat.StreamResponse;
import com.lovable_clone_microservices.intelligence_service.entity.ChatEvent;
import com.lovable_clone_microservices.intelligence_service.entity.ChatMessage;
import com.lovable_clone_microservices.intelligence_service.entity.ChatSession;
import com.lovable_clone_microservices.intelligence_service.entity.ChatSessionId;
import com.lovable_clone_microservices.intelligence_service.llm.PromptUtils;
import com.lovable_clone_microservices.intelligence_service.llm.advisors.FileTreeContextAdvisor;
import com.lovable_clone_microservices.intelligence_service.llm.tools.CodeGenerationTools;
import com.lovable_clone_microservices.intelligence_service.llm.tools.LlmResponseParser;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import com.lovable_clone_microservices.intelligence_service.repository.ChatEventRepository;
import com.lovable_clone_microservices.intelligence_service.repository.ChatMessageRepository;
import com.lovable_clone_microservices.intelligence_service.repository.ChatSessionRepository;
import com.lovable_clone_microservices.intelligence_service.service.AiGenerationService;
import com.lovable_clone_microservices.intelligence_service.service.UsageService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class AiGenerationServiceImpl implements AiGenerationService {

   ChatClient chatClient;
   AuthUtil authUtil;
   FileTreeContextAdvisor fileTreeContextAdvisor;
   ChatSessionRepository chatSessionRepository;
   LlmResponseParser llmResponseParser;
   ChatMessageRepository chatMessageRepository;
   ChatEventRepository chatEventRepository;
   UsageService usageService;
   WorkspaceClient workspaceClient;
    //We could just append our system prompt with our filetreecontext like just append my systemprompt
    //with the file tree context with just like projectfileservice.getfiletree(projectid) and append like normal string
    //but the spring ai convention is that to add what ever you want to add in advisors only
    //if that thing is other than user or system prompt

     static  Pattern FILE_TAG_PATTERN=  Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>",Pattern.DOTALL);

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")//here if we want to pass the arguments from the below method call we use # telling use the parameters passed in below function
    //as parameters
    public Flux<StreamResponse> streamResponse(String userPrompt, Long projectId) {
//           usageService.checkDailyTokensUsage();
            Long userId=authUtil.getCurrentUserId();
           ChatSession chatSession= createChatSessionIfNotExists(projectId,userId);
        Map<String,Object> advisorParams =Map.of(
                "userId" ,userId,
                "projectId",projectId
        );

        StringBuilder fullResponseBuffer=new StringBuilder();
        CodeGenerationTools codeGenerationTools=new CodeGenerationTools(projectId,workspaceClient);
        AtomicReference<Long> startTime=new AtomicReference<Long>(System.currentTimeMillis());
        AtomicReference<Long> endTime=new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<Usage>();


        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userPrompt)
                .tools(codeGenerationTools)
                .advisors(
                        advisorSpec -> {
                            advisorSpec.params(advisorParams);
                            advisorSpec.advisors(fileTreeContextAdvisor);
                        }
                ).stream()
                .chatResponse()
                .doOnNext(response -> {
                    String content = response.getResult()
                            .getOutput()
                            .getText();
                    if (content != null && !content.isEmpty() && endTime.get() == 0) {
                        endTime.set(System.currentTimeMillis());
                    }
                    if (response.getMetadata() != null &&
                            response.getMetadata().getUsage() != null) {
                        usageRef.set(response.getMetadata().getUsage());
                    }
                    if (content != null) {
                        fullResponseBuffer.append(content);
                    }
                })
                .doOnComplete(() -> {
                    long end = endTime.get();
                    if (end == 0) {
                        end = System.currentTimeMillis();
                    }
                    long duration = (end - startTime.get()) / 1000;
                    finalizeChats(
                            userPrompt,
                            chatSession,
                            fullResponseBuffer.toString(),
                            duration,
                            usageRef.get()
                    );
                })
                .doOnError(error ->
                        log.error(
                                "Error occurred during streaming of projectId: {}",
                                projectId,
                                error
                        )
                )
                .map(response -> {
                    String text = response.getResult()
                            .getOutput()
                            .getText();

                    return new StreamResponse(
                            text != null ? text : ""
                    );
                });
    }

//    Don't think:
//    LLM → XML
//    Think:
//    LLM → TEXT
//    The text can be:
//    Hello
//    or:
//<message>Hello</message>
//    or:
//<file path="App.tsx">...</file>
//    or:
//    {"name":"Nishanth"}
//    or:
//    I am a banana.
//    The model fundamentally generates tokens that become text.
//    Your system prompt influences what text it generates.
//    JSON is the API/transport structure; <file>, <message>, <tool> are merely text generated inside the model's content because your system prompt instructed the model to use those tags.
//{
//  "choices": [
//    {
//      "message": {
//        "content": "<file path="App.tsx">...</file>" -> text but in xml format==>we asked the llm in system prompt to give in this format
//      }
//    }
//  ]
//}


private void finalizeChats(String userMessage, ChatSession chatSession, String fullText, Long duration, Usage usage){

            Long projectId=chatSession.getChatSessionId().getProjectId();
    int promptTokens = 0;
    int completionTokens = 0;
    int totalTokens = 0;


    if(usage != null) {
        promptTokens = usage.getPromptTokens();
        completionTokens = usage.getCompletionTokens();
        totalTokens = usage.getTotalTokens();

        usageService.recordTokenUsage(
                chatSession.getChatSessionId().getUserId(),
                totalTokens
        );
    }
   //saving the user message
    ChatMessage userChatMessage = ChatMessage.builder()
            .chatSession(chatSession)
            .role(MessageRole.USER)
            .content(userMessage)
            .tokensUsed(promptTokens)
            .build();

    chatMessageRepository.save(userChatMessage);

    // Save assistant message
    ChatMessage assistantChatMessage = ChatMessage.builder()
            .chatSession(chatSession)
            .role(MessageRole.ASSISTANT)
            .content(fullText)
            .tokensUsed(completionTokens)
            .build();
    chatMessageRepository.save(assistantChatMessage);

    // Parse AI response into events
    List<ChatEvent> chatEventList =
            llmResponseParser.parseChatEvents(
                    fullText,
                    assistantChatMessage
            );

    chatEventList.addFirst(
            ChatEvent.builder()
                    .type(ChatEventType.THOUGHT)
                    .chatMessage(assistantChatMessage)
                    .content("Thought for " + duration + "s")
                    .sequenceOrder(0)
                    .build()
    );

    // Save generated files
    chatEventList.stream()
            .filter(e -> e.getType() == ChatEventType.FILE_EDIT)
            .forEach(e ->{}
//                    projectFileService.saveFile(
//                            projectId,
//                            e.getFilePath(),
//                            e.getContent()
//                    )
            );

    chatEventRepository.saveAll(chatEventList);
}


    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId=new ChatSessionId(projectId,userId);
        ChatSession chatSession=chatSessionRepository.findById(chatSessionId).orElse(null);
        if(chatSession==null){
            ChatSession freshChatSession=ChatSession.builder().chatSessionId(chatSessionId).build();
            chatSessionRepository.save(freshChatSession);
            return freshChatSession;
        }
        return chatSession;
    }
}
