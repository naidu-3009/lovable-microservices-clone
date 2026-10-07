package com.lovable_clone_microservices.account_service.mapper;


import com.lovable_clone_microservices.account_service.dto.auth.SignUpRequest;
import com.lovable_clone_microservices.account_service.dto.auth.UserProfileResponse;
import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.common_library.dto.UserDto;
import com.lovable_clone_microservices.common_library.security.JwtUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUserEntity(SignUpRequest signUpRequest);

    @Mapping(target = "userId", source = "userId")
    UserProfileResponse toUserProfileResponse(JwtUserPrincipal user);

    UserDto toUserDto(User user);
}
