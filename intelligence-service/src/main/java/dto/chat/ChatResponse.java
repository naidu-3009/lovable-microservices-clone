package dto.chat;

import com.projectlove.lovable_clone.enums.MessageRole;

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
