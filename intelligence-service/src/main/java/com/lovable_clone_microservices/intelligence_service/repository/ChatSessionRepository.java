package com.lovable_clone_microservices.intelligence_service.repository;

import com.lovable_clone_microservices.intelligence_service.entity.ChatSession;
import com.lovable_clone_microservices.intelligence_service.entity.ChatSessionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {


}
