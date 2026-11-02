package com.lovable_clone_microservices.account_service.service.impl;

import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.account_service.services.implementation.UserService;
import com.lovable_clone_microservices.common_library.security.JwtUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @InjectMocks
    UserService userService;

    @Test
    void loadUserByUsername_shouldReturnJwtPrincipal() {
        User user = User.builder()
                .id(21L)
                .username("nishanth@example.com")
                .name("Nishanth")
                .password("encoded-password")
                .build();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername(user.getUsername());

        assertThat(result).isInstanceOf(JwtUserPrincipal.class);
        JwtUserPrincipal principal = (JwtUserPrincipal) result;
        assertThat(principal.userId()).isEqualTo("21");
        assertThat(principal.getUsername()).isEqualTo("nishanth@example.com");
        assertThat(principal.name()).isEqualTo("Nishanth");
        assertThat(principal.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@example.com");
    }
}
