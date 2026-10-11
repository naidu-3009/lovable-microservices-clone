package com.lovable_clone_microservices.api_gtw.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lovable_clone_microservices.api_gtw.config.SecurityProperties;
import com.lovable_clone_microservices.api_gtw.error.ApiError;
import com.lovable_clone_microservices.api_gtw.service.JwtGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.bm.Rule;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
@RequiredArgsConstructor
@Component
public class GatewayJwtAuthFIlter implements GlobalFilter, Ordered {

    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher=new AntPathMatcher();
    private final JwtGatewayService jwtGatewayService;


    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        boolean isPublic = securityProperties.getPublicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isPublic) {
            log.info("Public route, continue: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            jwtGatewayService.validateToken(token);
            log.info("JWT token valid for path: {}", path);
        } catch (Exception e) {
            log.error("JWT Validation failed at Gateway: {}", e.getMessage());
            return sendErrorResponse(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        return chain.filter(exchange);
    }



    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        ApiError apiError = new ApiError(status, message);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiError);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error serializing gateway error response", e);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
