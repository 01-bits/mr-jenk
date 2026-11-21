package com.buy01.user.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.buy01.user.model.Role;

import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor 
public class HeadersFilter extends OncePerRequestFilter {

    @Value("${AUTH_USER_ID_HEADER}")
    private String AUTH_USER_ID_HEADER;

    @Value("${AUTH_USER_ROLE_HEADER}")
    private String AUTH_USER_ROLE_HEADER;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(AUTH_USER_ID_HEADER);
        String userRole = request.getHeader(AUTH_USER_ROLE_HEADER);

        if (userId == null || userId.isBlank() || userRole == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Role role = Role.valueOf(userRole.trim().toUpperCase());
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException e) {
            // unknown role: continue unauthenticated, protected routes will answer 401
        }

        filterChain.doFilter(request, response);

    }
}