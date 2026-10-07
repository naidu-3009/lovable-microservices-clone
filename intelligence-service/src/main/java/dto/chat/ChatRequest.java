package dto.chat;

public record ChatRequest(
        String message,Long projectId
) {
}
