package com.lovable_clone_microservices.workspace_service.mapper;



import com.lovable_clone_microservices.workspace_service.dto.projects.FileNode;
import com.lovable_clone_microservices.workspace_service.entity.ProjectFile;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectFileMapper {

    public List<FileNode> toFileNode(List<ProjectFile> projectFileList);


}
