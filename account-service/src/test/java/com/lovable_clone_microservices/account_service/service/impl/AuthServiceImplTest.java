package com.lovable_clone_microservices.account_service.service.impl;

import com.lovable_clone_microservices.account_service.dto.auth.AuthResponse;
import com.lovable_clone_microservices.account_service.dto.auth.LoginRequest;
import com.lovable_clone_microservices.account_service.dto.auth.SignUpRequest;
import com.lovable_clone_microservices.account_service.dto.auth.UserProfileResponse;
import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.account_service.mapper.UserMapper;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.account_service.services.implementation.AuthServiceImpl;
import com.lovable_clone_microservices.common_library.error.BadRequestException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.common_library.security.JwtUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthUtil authUtil;
    @Mock
    AuthenticationManager authenticationManager;

    @InjectMocks
    AuthServiceImpl authService;

    @Test
    void signup_shouldPersistEncodedPasswordAndReturnTokenAndProfile() {
        SignUpRequest request = new SignUpRequest("nishanth@example.com", "Nishanth", "plain-pass");
        User user = User.builder()
                .id(17L)
                .username(request.username())
                .name(request.name())
                .build();
        UserProfileResponse profile = new UserProfileResponse(17L, request.username(), request.name());

        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        when(userMapper.toUserEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-pass");
        when(userRepository.save(user)).thenReturn(user);
        when(authUtil.generateAccessToken(any(JwtUserPrincipal.class))).thenReturn("jwt-token");
        when(userMapper.toUserProfileResponse(any(JwtUserPrincipal.class))).thenReturn(profile);

        AuthResponse response = authService.signup(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user()).isEqualTo(profile);
        assertThat(user.getPassword()).isEqualTo("encoded-pass");

        ArgumentCaptor<JwtUserPrincipal> principalCaptor = ArgumentCaptor.forClass(JwtUserPrincipal.class);
        verify(authUtil).generateAccessToken(principalCaptor.capture());
        assertThat(principalCaptor.getValue().userId()).isEqualTo("17");
        assertThat(principalCaptor.getValue().getUsername()).isEqualTo(request.username());
        assertThat(principalCaptor.getValue().name()).isEqualTo(request.name());
        assertThat(principalCaptor.getValue().getPassword()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void signup_shouldRejectDuplicateUsername() {
        SignUpRequest request = new SignUpRequest("nishanth@example.com", "Nishanth", "plain-pass");
        when(userRepository.findByUsername(request.username()))
                .thenReturn(Optional.of(User.builder().id(1L).username(request.username()).build()));

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("user already exists");

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, authUtil);
    }

    @Test
    void login_shouldAuthenticateAndReturnJwtAndProfile() {
        LoginRequest request = new LoginRequest("nishanth@example.com", "plain-pass");
        JwtUserPrincipal principal = new JwtUserPrincipal("17", "nishanth@example.com", "Nishanth", null, java.util.List.of());
        Authentication authentication = mock(Authentication.class);
        UserProfileResponse profile = new UserProfileResponse(17L, request.username(), "Nishanth");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authUtil.generateAccessToken(principal)).thenReturn("jwt-token");
        when(userMapper.toUserProfileResponse(principal)).thenReturn(profile);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user()).isEqualTo(profile);
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo(request.username());
        assertThat(captor.getValue().getCredentials()).isEqualTo(request.password());
    }

    @Test
    void login_shouldPropagateInvalidCredentials() {
        LoginRequest request = new LoginRequest("nishanth@example.com", "wrong-pass");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(authUtil, userMapper);
    }
}
