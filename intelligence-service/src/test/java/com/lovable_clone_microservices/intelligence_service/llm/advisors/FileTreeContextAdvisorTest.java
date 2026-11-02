package com.lovable_clone_microservices.intelligence_service.llm.advisors;

import com.lovable_clone_microservices.intelligence_service.client.WorkspaceClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FileTreeContextAdvisorTest {

    @Test
    void advisor_shouldExposeStableNameAndOrder() {
        FileTreeContextAdvisor advisor = new FileTreeContextAdvisor(mock(WorkspaceClient.class));

        assertThat(advisor.getName()).isEqualTo("File Context Path");
        assertThat(advisor.getOrder()).isZero();
    }
}
