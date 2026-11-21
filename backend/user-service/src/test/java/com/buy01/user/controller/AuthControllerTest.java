package com.buy01.user.controller;
 
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
 
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
 
import com.buy01.user.DTO.AuthDTO.AuthResponse;
import com.buy01.user.DTO.AuthDTO.LoginRequest;
import com.buy01.user.DTO.AuthDTO.RegisterRequest;
import com.buy01.user.config.SecurityConfig;
import com.buy01.user.exception.DuplicateResourceException;
import com.buy01.user.model.Role;
import com.buy01.user.services.AuthService;
 
/**
 * AuthControllerTest
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "AUTH_USER_ID_HEADER=X-User-Id",
        "AUTH_USER_ROLE_HEADER=X-User-Role"
})
public class AuthControllerTest {
 
    private static final AuthResponse RESPONSE =
            new AuthResponse("user-1", "User", "user@example.com", Role.SELLER, null, "jwt-token");
 
    @Autowired
    private MockMvc mockMvc;
 
    @MockitoBean
    private AuthService authService;
 
    // ---------- POST /auth/register ----------
 
    @Test
    void register_shouldReturn200() throws Exception {
        RegisterRequest expected = new RegisterRequest("User", "user@example.com", "secret123", Role.SELLER, null);
        when(authService.registerService(expected)).thenReturn(ResponseEntity.ok(RESPONSE));
 
        // public route: no identity headers
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "User",
                          "email": "user@example.com",
                          "password": "secret123",
                          "role": "SELLER"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
 
    @Test
    void register_shouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "User",
                          "email": "not-an-email",
                          "password": "123",
                          "role": "SELLER"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"))
                .andExpect(jsonPath("$.password").value("Password must be at least 6 characters long"));
 
        verifyNoInteractions(authService);
    }
 
    @Test
    void register_shouldReturn409WhenEmailExists() throws Exception {
        when(authService.registerService(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists."));
 
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "User",
                          "email": "user@example.com",
                          "password": "secret123",
                          "role": "CLIENT"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email already exists."));
    }
 
    // ---------- POST /auth/login ----------
 
    @Test
    void login_shouldReturn200() throws Exception {
        LoginRequest expected = new LoginRequest("user@example.com", "secret123");
        when(authService.loginService(expected)).thenReturn(ResponseEntity.ok(RESPONSE));
 
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "user@example.com", "password": "secret123" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
 
    @Test
    void login_shouldReturn401WhenCredentialsWrong() throws Exception {
        when(authService.loginService(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
 
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "user@example.com", "password": "wrong-password" }
                        """))
                .andExpect(status().isUnauthorized());
    }
}
 