package com.lovable_clone_microservices.intelligence_service.security;

import com.lovable_clone_microservices.common_library.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.DispatcherType;







@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
@EnableWebSecurity
public class IntelligenceSecurityConfig{
        private final JwtAuthFilter jwtAuthFilter;
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){
            httpSecurity
                    .csrf( csrfConfig -> csrfConfig.disable())
                    .cors(Customizer.withDefaults())
                    .sessionManagement(sessionConfig->sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                    .authorizeHttpRequests(auth ->auth
//                            .anyRequest().authenticated()
//                    )
                    .authorizeHttpRequests(auth -> auth
                            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            return httpSecurity.build();
        }

}
