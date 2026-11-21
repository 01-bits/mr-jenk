package com.buy01.user.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * HeadersFilterTest
 */
public class HeadersFilterTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    private HeadersFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        filter = new HeadersFilter(USER_ID_HEADER, ROLE_HEADER);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    /**
     * Runs the filter, checks the request always continues, returns the identity it
     * set.
     */
    private Authentication runFilter() {
        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        assertNotNull(chain.getRequest(), "filter must always pass the request on");
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // ---------- tests ----------

    @Test
    void shouldAuthenticateValidRole() {
        request.addHeader(USER_ID_HEADER, "seller-1");
        request.addHeader(ROLE_HEADER, "SELLER");

        Authentication auth = runFilter();

        assertNotNull(auth);
        assertEquals("seller-1", auth.getName());
        assertEquals(List.of("ROLE_SELLER"),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void shouldNotAuthenticateUnknownRole() {
        request.addHeader(USER_ID_HEADER, "user-1");
        request.addHeader(ROLE_HEADER, "ADMIN");

        assertNull(runFilter());
    }

    @Test
    void shouldNotAuthenticateWhenUserIdMissing() {
        request.addHeader(ROLE_HEADER, "SELLER");

        assertNull(runFilter());
    }

    @Test
    void shouldNotAuthenticateWhenUserIdBlank() {
        request.addHeader(USER_ID_HEADER, "   ");
        request.addHeader(ROLE_HEADER, "SELLER");

        assertNull(runFilter());
    }

    @Test
    void shouldNotAuthenticateWhenRoleMissing() {
        request.addHeader(USER_ID_HEADER, "seller-1");

        assertNull(runFilter());
    }
}