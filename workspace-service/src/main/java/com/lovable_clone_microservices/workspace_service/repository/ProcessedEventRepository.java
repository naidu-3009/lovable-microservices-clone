package com.lovable_clone_microservices.workspace_service.repository;

import com.lovable_clone_microservices.workspace_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
