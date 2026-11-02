package com.lovable_clone_microservices.intelligence_service.llm.tools;

import com.lovable_clone_microservices.intelligence_service.client.WorkspaceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeGenerationToolsTest {

    @Mock WorkspaceClient workspaceClient;

    @Test
    void readFiles_shouldFetchRequestedFilesAndFormatTheirContents() {
        CodeGenerationTools tools = new CodeGenerationTools(9L, workspaceClient, "Bearer test-token");
        when(workspaceClient.getFileContent(9L, "src/App.tsx", "Bearer test-token"))
                .thenReturn("export default 1;");
        when(workspaceClient.getFileContent(9L, "package.json", "Bearer test-token"))
                .thenReturn("{\"name\":\"demo\"}");

        List<String> result = tools.readFiles(List.of("/src/App.tsx", "package.json"));

        assertThat(result).containsExactly(
                "--- START OF FILE: src/App.tsx ---\nexport default 1;\n--- END OF FILE ---",
                "--- START OF FILE: package.json ---\n{\"name\":\"demo\"}\n--- END OF FILE ---"
        );
        verify(workspaceClient).getFileContent(9L, "src/App.tsx", "Bearer test-token");
        verify(workspaceClient).getFileContent(9L, "package.json", "Bearer test-token");
    }

    @Test
    void readFiles_shouldReturnEmptyListForNoPaths() {
        CodeGenerationTools tools = new CodeGenerationTools(9L, workspaceClient, "Bearer test-token");

        assertThat(tools.readFiles(List.of())).isEmpty();
        verifyNoInteractions(workspaceClient);
    }

    @Test
    void readFiles_shouldPropagateWorkspaceClientFailure() {
        CodeGenerationTools tools = new CodeGenerationTools(9L, workspaceClient, "Bearer test-token");
        when(workspaceClient.getFileContent(9L, "src/Missing.tsx", "Bearer test-token"))
                .thenThrow(new IllegalStateException("Workspace unavailable"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tools.readFiles(List.of("src/Missing.tsx")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Workspace unavailable");
    }
}
