package com.lovable_clone_microservices.intelligence_service.repository;

import com.lovable_clone_microservices.intelligence_service.entity.ChatEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatEventRepository extends JpaRepository<ChatEvent,Long> {
}
