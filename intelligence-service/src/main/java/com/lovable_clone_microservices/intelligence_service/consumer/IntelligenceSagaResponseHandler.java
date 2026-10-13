package com.lovable_clone_microservices.intelligence_service.consumer;


import com.lovable_clone_microservices.common_library.enums.ChatEventStatus;
import com.lovable_clone_microservices.common_library.event.FileStoreResponseEvent;
import com.lovable_clone_microservices.intelligence_service.repository.ChatEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntelligenceSagaResponseHandler {

    private final ChatEventRepository chatEventRepository;

    @Transactional
    @KafkaListener(topics = "file-store-responses", groupId = "intelligence-group")
    public void handleSagaResponse(FileStoreResponseEvent response) {

        chatEventRepository.findBySagaId(response.sagaId()).ifPresent(event -> {


            //*****we are using commonly shared enumns as kinda idempotency key **********
            //theres no rule that we gotta use idempotency key always here we are using commongly shared ChatEventStatus via common-lib
            //new way to handle idempotency
            if (!ChatEventStatus.PENDING.equals(event.getStatus())) { //Idempotency
                log.info("Response for Saga {} already handled. Skipping.", response.sagaId());
                return;
            }

            if (response.success()) {
                event.setStatus(ChatEventStatus.CONFIRMED);
                log.info("Saga {} CONFIRMED", response.sagaId());
            } else {
                log.warn("Saga {} FAILED. Deleting event.", response.sagaId());
                event.setStatus(ChatEventStatus.FAILED);
            }
        });
    }
}
