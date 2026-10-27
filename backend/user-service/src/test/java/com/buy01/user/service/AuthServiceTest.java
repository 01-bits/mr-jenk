package com.buy01.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.buy01.user.DTO.AuthDTO.LoginRequest;
import com.buy01.user.DTO.AuthDTO.RegisterRequest;
import com.buy01.user.exception.DuplicateResourceException;
import com.buy01.user.model.Role;
import com.buy01.user.model.User;
import com.buy01.user.repository.UserRepository;
import com.buy01.user.services.AuthService;
import com.buy01.user.services.JwtService;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private BCryptPasswordEncoder passwordEncoder;

        @Mock
        private JwtService jwtService;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private Authentication authentication;

        @InjectMocks
        private AuthService authService;

        private User user;

        @BeforeEach
        void setUp() {
                user = User.builder()
                                .name("Ahmed")
                                .email("ahmed@example.com")
                                .password("encoded-password")
                                .role(Role.CLIENT)
                                .build();
        }

        @Test
        void registerService_shouldRegisterUserSuccessfully() {
                RegisterRequest request = new RegisterRequest(
                                "Ahmed",
                                "ahmed@example.com",
                                "plain-password",
                                Role.CLIENT,
                                "");

                when(userRepository.existsByEmail("ahmed@example.com"))
                                .thenReturn(false);

                when(passwordEncoder.encode("plain-password"))
                                .thenReturn("encoded-password");

                when(userRepository.save(any(User.class)))
                                .thenReturn(user);

                when(jwtService.generateToken(user))
                                .thenReturn("fake-jwt-token");

                var result = authService.registerService(request);

                assertEquals(HttpStatus.OK, result.getStatusCode());
                assertNotNull(result.getBody());

                verify(userRepository).existsByEmail("ahmed@example.com");
                verify(passwordEncoder).encode("plain-password");
                verify(userRepository).save(any(User.class));
                verify(jwtService).generateToken(user);
        }

        @Test
        void registerService_shouldThrowExceptionWhenEmailExists() {
                RegisterRequest request = new RegisterRequest(
                                "Ahmed",
                                "ahmed@example.com",
                                "plain-password",
                                Role.CLIENT, "");

                when(userRepository.existsByEmail("ahmed@example.com"))
                                .thenReturn(true);

                assertThrows(
                                DuplicateResourceException.class,
                                () -> authService.registerService(request));

                verify(userRepository).existsByEmail("ahmed@example.com");

                verify(userRepository, never()).save(any(User.class));

                verifyNoInteractions(passwordEncoder, jwtService);
        }

        @Test
        void loginService_shouldLoginSuccessfully() {
                LoginRequest request = new LoginRequest(
                                "ahmed@example.com",
                                "plain-password");

                when(authenticationManager.authenticate(
                                any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

                when(authentication.getPrincipal()).thenReturn(user);

                when(jwtService.generateToken(user))
                                .thenReturn("fake-jwt-token");

                var result = authService.loginService(request);

                assertEquals(HttpStatus.OK, result.getStatusCode());
                assertNotNull(result.getBody());

                verify(authenticationManager).authenticate(
                                any(UsernamePasswordAuthenticationToken.class));

                verify(jwtService).generateToken(user);
        }

        @Test
        void loginService_shouldPropagateAuthenticationFailure() {
                LoginRequest request = new LoginRequest(
                                "ahmed@example.com",
                                "wrong-password");

                when(authenticationManager.authenticate(
                                any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad Credentials"));

                assertThrows(
                                BadCredentialsException.class,
                                () -> authService.loginService(request));

                verify(jwtService, never()).generateToken(any(User.class));
        }
}
