package com.lovable_clone_microservices.intelligence_service.llm.tools;

import com.lovable_clone_microservices.common_library.enums.ChatEventStatus;
import com.lovable_clone_microservices.common_library.enums.ChatEventType;
import com.lovable_clone_microservices.intelligence_service.entity.ChatEvent;
import com.lovable_clone_microservices.intelligence_service.entity.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser();

    @Test
    void parseChatEvents_shouldParseMessageFileAndToolEventsInOrder() {
        ChatMessage parent = new ChatMessage();
        String response = """
                <message>Welcome to IdeaStitch.</message>
                <tool args="[src/App.tsx]">Reading project files</tool>
                <file path="src/App.tsx">export default function App() { return <h1>Hello</h1>; }</file>
                """;

        List<ChatEvent> events = parser.parseChatEvents(response, parent);

        assertThat(events).hasSize(3);
        assertThat(events).extracting(ChatEvent::getType)
                .containsExactly(ChatEventType.MESSAGE, ChatEventType.TOOL_LOG, ChatEventType.FILE_EDIT);
        assertThat(events).extracting(ChatEvent::getSequenceOrder).containsExactly(1, 2, 3);
        assertThat(events).allMatch(event -> event.getChatMessage() == parent);
        assertThat(events.get(0).getStatus()).isEqualTo(ChatEventStatus.CONFIRMED);
        assertThat(events.get(1).getStatus()).isEqualTo(ChatEventStatus.CONFIRMED);
        assertThat(events.get(2).getStatus()).isEqualTo(ChatEventStatus.PENDING);
        assertThat(events.get(1).getMetadata()).isEqualTo("[src/App.tsx]");
        assertThat(events.get(2).getFilePath()).isEqualTo("src/App.tsx");
        assertThat(events.get(2).getContent()).contains("Hello");
    }

    @Test
    void parseChatEvents_shouldTrimContentAndHandleCaseInsensitiveTags() {
        List<ChatEvent> events = parser.parseChatEvents(
                "<MESSAGE>  Hello\nWorld  </MESSAGE>", new ChatMessage());

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getType()).isEqualTo(ChatEventType.MESSAGE);
        assertThat(events.getFirst().getContent()).isEqualTo("Hello\nWorld");
    }

    @Test
    void parseChatEvents_shouldReturnEmptyListWhenNoSupportedCompleteTagsExist() {
        assertThat(parser.parseChatEvents("plain text only", new ChatMessage())).isEmpty();
        assertThat(parser.parseChatEvents("<message>unfinished", new ChatMessage())).isEmpty();
        assertThat(parser.parseChatEvents("", new ChatMessage())).isEmpty();
    }

    @Test
    void parseChatEvents_shouldParseMultipleFileEditsSeparately() {
        String response = "<file path=\"src/A.tsx\">A</file><file path=\"src/B.tsx\">B</file>";

        List<ChatEvent> events = parser.parseChatEvents(response, new ChatMessage());

        assertThat(events).hasSize(2);
        assertThat(events).extracting(ChatEvent::getFilePath)
                .containsExactly("src/A.tsx", "src/B.tsx");
        assertThat(events).allMatch(event -> event.getType() == ChatEventType.FILE_EDIT
                && event.getStatus() == ChatEventStatus.PENDING);
    }
}
