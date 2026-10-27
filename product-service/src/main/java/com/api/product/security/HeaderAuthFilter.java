package com.api.product.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.api.product.model.Role;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

/**
 * HeaderAuthFilter
 */
@AllArgsConstructor
public class HeaderAuthFilter extends OncePerRequestFilter {

    private final String userIdHeader;
    private final String roleHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String userId = req.getHeader(userIdHeader);
        String role = req.getHeader(roleHeader);

        if (userId != null && !userId.isBlank() && role != null && Role.valueOf(role.toUpperCase()) != null) {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority(roleEnum.toAuthority())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }

}