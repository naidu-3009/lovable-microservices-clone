package com.lovable_clone_microservices.account_service.services.implementation;

import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.common_library.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new JwtUserPrincipal(
                user.getId().toString(),
                user.getUsername(),
                user.getName(),
                user.getPassword(),
                new ArrayList<>()
        );    }
}