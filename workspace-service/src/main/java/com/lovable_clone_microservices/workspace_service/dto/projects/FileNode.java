package com.lovable_clone_microservices.workspace_service.dto.projects;

public record FileNode(String path
) {
    @Override
    public String toString() {
        return path;
    }
}
