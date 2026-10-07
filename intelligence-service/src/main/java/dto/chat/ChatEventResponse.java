package dto.chat;

import com.projectlove.lovable_clone.enums.ChatEventType;
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
