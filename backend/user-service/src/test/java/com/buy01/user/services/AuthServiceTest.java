package com.buy01.user.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.buy01.user.DTO.AuthDTO.AuthResponse;
import com.buy01.user.DTO.AuthDTO.LoginRequest;
import com.buy01.user.DTO.AuthDTO.RegisterRequest;
import com.buy01.user.exception.DuplicateResourceException;
import com.buy01.user.model.Role;
import com.buy01.user.model.User;
import com.buy01.user.repository.UserRepository;
import com.buy01.user.services.AuthService;
import com.buy01.user.services.JwtService;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

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

        @InjectMocks
        private AuthService authService;

        // ---------- register ----------

        @Test
        void register_shouldSaveEncodedPasswordAndReturnToken() {
                RegisterRequest request = new RegisterRequest("User", "user@example.com", "plain-password", Role.SELLER,
                                null);
                when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
                when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
                // like MongoDB: save returns the user with a generated id
                when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                        User u = inv.getArgument(0);
                        u.setId("user-1");
                        return u;
                });
                when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

                ResponseEntity<AuthResponse> result = authService.registerService(request);

                // what was stored
                ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(saved.capture());
                assertEquals("User", saved.getValue().getName());
                assertEquals("user@example.com", saved.getValue().getEmail());
                assertEquals("encoded-password", saved.getValue().getPassword()); // never the plain password
                assertEquals(Role.SELLER, saved.getValue().getRole());

                // what the client receives
                assertEquals(HttpStatus.OK, result.getStatusCode());
                AuthResponse body = result.getBody();
                assertEquals("user-1", body.id());
                assertEquals("user@example.com", body.email());
                assertEquals(Role.SELLER, body.role());
                assertEquals("jwt-token", body.token());
        }

        @Test
        void register_shouldRejectDuplicateEmail() {
                RegisterRequest request = new RegisterRequest("User", "user@example.com", "plain-password", Role.CLIENT,
                                null);
                when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

                DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                                () -> authService.registerService(request));

                assertEquals("Email already exists.", ex.getMessage());
                verify(userRepository, never()).save(any(User.class));
                verifyNoInteractions(passwordEncoder, jwtService);
        }

        // ---------- login ----------

        @Test
        void login_shouldReturnTokenForAuthenticatedUser() {
                User user = User.builder()
                                .id("user-1").name("User").email("user@example.com")
                                .password("encoded-password").role(Role.CLIENT).build();
                // a real authenticated token, as the AuthenticationManager would return it
                Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
                when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
                when(jwtService.generateToken(user)).thenReturn("jwt-token");

                ResponseEntity<AuthResponse> result = authService.loginService(
                                new LoginRequest("user@example.com", "plain-password"));

                // the credentials typed by the user are what gets checked
                ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
                verify(authenticationManager).authenticate(attempt.capture());
                assertEquals("user@example.com", attempt.getValue().getPrincipal());
                assertEquals("plain-password", attempt.getValue().getCredentials());

                assertEquals(HttpStatus.OK, result.getStatusCode());
                assertEquals("user-1", result.getBody().id());
                assertEquals("jwt-token", result.getBody().token());
        }

        @Test
        void login_shouldNotIssueTokenWhenCredentialsWrong() {
                when(authenticationManager.authenticate(any(Authentication.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                assertThrows(BadCredentialsException.class,
                                () -> authService.loginService(new LoginRequest("user@example.com", "wrong-password")));

                verifyNoInteractions(jwtService);
        }
}
