package com.buy01.user.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${AUTH_USER_ID_HEADER}") String userIdHeader,
                        @Value("${AUTH_USER_ROLE_HEADER}") String roleHeader) {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests((requests) -> requests
                                                .requestMatchers("/actuator/health/**").permitAll()
                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.PUT, "/profile/me").hasRole("SELLER")
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(e -> e
                                                .authenticationEntryPoint(
                                                                (req, res, ex) -> json(res, 401, "Unauthorized",
                                                                                "Authentication required"))
                                                .accessDeniedHandler((req, res, ex) -> json(res, 403, "Forbidden",
                                                                "Access denied")))
                                .addFilterBefore(new HeadersFilter(userIdHeader, roleHeader), UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        private static void json(HttpServletResponse res, int status, String error, String msg) throws IOException {
                res.setStatus(status);
                res.setContentType("application/json");
                res.getWriter().write(
                                "{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + msg + "\"}");
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }
}
