package llm.advisors;

import client.WorkspaceClient;
import com.lovable_clone_microservices.common_library.dto.FileNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class FileTreeContextAdvisor implements StreamAdvisor {

    private final WorkspaceClient workspaceClient;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamAdvisorChain) {
         Map<String,Object> context =request.context();
         Long projectId=Long.parseLong(context.getOrDefault("projectId",0).toString());
        ChatClientRequest augmentedChatClientRequest=augmentRequestWithFileTree(request,projectId);
        return streamAdvisorChain.nextStream(augmentedChatClientRequest);
    }

    private ChatClientRequest augmentRequestWithFileTree(ChatClientRequest request,Long projectId){

        List<Message> unAugmentedPrompt=request.prompt().getInstructions();


        Message systemPrompt=unAugmentedPrompt.stream()
                .filter(m-> m.getMessageType()== MessageType.SYSTEM)
                .findFirst()
                .orElse(null);


        List<Message> userPrompt = new ArrayList<>(unAugmentedPrompt.stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .toList());



        List<Message> finalAugmentedPrompt=new ArrayList<>();

        if(systemPrompt != null){
            finalAugmentedPrompt.add(systemPrompt);
        }


        List<FileNode> fileTree=workspaceClient.getFileTree(projectId).files();
        String fileTreeContext="\n\n ===========File Tree========\n"+fileTree.toString();
        finalAugmentedPrompt.add(new SystemMessage(fileTreeContext));

        finalAugmentedPrompt.addAll(userPrompt);

        return request.mutate().prompt(new Prompt(finalAugmentedPrompt,request.prompt().getOptions())).build();
    }

    @Override
    public String getName() {
        return "File Context Path";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}




//LLms has this caching thing where if the consecutive prompts have same "prefix"  then
//the response would be kinda cached response and this is  cache-hit
//suppose the prefixes were not same then thats a cache miss and it would genenrally cost more than cache hit ones
//so sometimes if we use advisors they can jumble the user ans system prompt
//its better to we ourselves keep them in order and send to llm -> for each message
