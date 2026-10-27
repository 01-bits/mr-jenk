package com.buy01.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.buy01.api_gateway.services.JwtService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtFilter implements GlobalFilter {

    @Value("${AUTH_USER_ID_HEADER}")
    private String AUTH_USER_ID_HEADER;

    @Value("${AUTH_USER_ROLE_HEADER}")
    private String AUTH_USER_ROLE_HEADER;

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        String authHeader = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.length() <= 7) {
            return chain.filter(exchange);
        }

        if (!authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenExpired(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String id = jwtService.extractId(token);
        String role = jwtService.extractRole(token);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header(AUTH_USER_ID_HEADER, id)
                .header(AUTH_USER_ROLE_HEADER, role)
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        return chain.filter(modifiedExchange);
    }
}
