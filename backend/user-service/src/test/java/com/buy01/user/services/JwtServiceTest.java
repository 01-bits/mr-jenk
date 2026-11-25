package com.buy01.user.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.buy01.user.model.Role;
import com.buy01.user.model.User;

import io.jsonwebtoken.JwtException;

/**
 * JwtServiceTest
 */
public class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLTAxMjM0NTY3ODk=";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LW5vdC10aGUtc2FtZS05ODc2NTQzMjEw";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = withSecret(SECRET);
    }

    private static JwtService withSecret(String secret) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        return service;
    }

    @Test
    void generateToken_shouldContainUserIdAndRole() {
        User user = User.builder().id("user-1").role(Role.SELLER).build();

        String token = jwtService.generateToken(user);

        assertEquals("user-1", jwtService.extractId(token));
        assertEquals("SELLER", jwtService.extractRole(token));
    }

    @Test
    void generateToken_shouldUseEmptyRoleWhenRoleIsNull() {
        String token = jwtService.generateToken("user-1", (Role) null);

        assertEquals("user-1", jwtService.extractId(token));
        assertEquals("", jwtService.extractRole(token));
    }

    @Test
    void extract_shouldRejectTokenSignedWithAnotherKey() {
        String foreignToken = withSecret(OTHER_SECRET).generateToken("user-1", Role.SELLER);

        assertThrows(JwtException.class, () -> jwtService.extractId(foreignToken));
    }

    @Test
    void extract_shouldRejectTamperedToken() {
        // a client tries to promote itself: keep its own signature,
        // but swap in the payload of a seller's token
        String[] client = jwtService.generateToken("user-1", Role.CLIENT).split("\\.");
        String[] seller = jwtService.generateToken("user-2", Role.SELLER).split("\\.");
        String forged = client[0] + "." + seller[1] + "." + client[2];

        assertThrows(JwtException.class, () -> jwtService.extractRole(forged));
    }

}