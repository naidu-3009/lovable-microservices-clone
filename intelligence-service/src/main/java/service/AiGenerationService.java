package service;

import com.openai.core.http.StreamResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service

public interface AiGenerationService {
    Flux<StreamResponse> streamResponse(String message, Long aLong);
}
