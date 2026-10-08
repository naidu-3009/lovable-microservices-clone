package com.lovable_clone_microservices.common_library.dto;

public record FileNode(String path
) {
    @Override
    public String toString() {
        return path;
    }
}
