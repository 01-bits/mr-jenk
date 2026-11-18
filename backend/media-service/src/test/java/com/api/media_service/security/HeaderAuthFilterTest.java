package com.api.media_service.security;

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

import com.api.media_service.model.Role;
import com.api.media_service.security.HeaderAuthFilter;

/**
 * HeaderAuthFilterTest
 */
public class HeaderAuthFilterTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    private HeaderAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        filter = new HeaderAuthFilter(USER_ID_HEADER, ROLE_HEADER);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private Authentication runFilter() throws Exception {
        filter.doFilter(request, response, chain);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static List<String> authoritiesOf(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    private void assertRequestContinued() {
        assertNotNull(chain.getRequest(), "filter must always pass the request on");
    }

    // ---------- valid headers ----------

    @Test
    void shouldAuthenticateSeller() throws Exception {
        request.addHeader(USER_ID_HEADER, "seller-1");
        request.addHeader(ROLE_HEADER, Role.SELLER.name());

        Authentication auth = runFilter();

        assertNotNull(auth);
        assertEquals("seller-1", auth.getName());
        assertEquals(List.of(Role.SELLER.toAuthority()), authoritiesOf(auth));
        assertRequestContinued();
    }

    @Test
    void shouldAuthenticateClient() throws Exception {
        request.addHeader(USER_ID_HEADER, "client-1");
        request.addHeader(ROLE_HEADER, Role.CLIENT.name());

        Authentication auth = runFilter();

        assertNotNull(auth);
        assertEquals("client-1", auth.getName());
        assertEquals(List.of(Role.CLIENT.toAuthority()), authoritiesOf(auth));
        assertRequestContinued();
    }

    @Test
    void shouldAcceptRoleInAnyCase() throws Exception {
        request.addHeader(USER_ID_HEADER, "seller-1");
        request.addHeader(ROLE_HEADER, "seller");

        Authentication auth = runFilter();

        assertNotNull(auth);
        assertEquals(List.of("ROLE_SELLER"), authoritiesOf(auth));
    }

    // ---------- invalid role ----------

    @Test
    void shouldNotAuthenticateAndNotThrowWhenRoleIsUnknown() throws Exception {
        request.addHeader(USER_ID_HEADER, "user-1");
        request.addHeader(ROLE_HEADER, "ADMIN");

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertRequestContinued();
    }

    @Test
    void shouldNotAuthenticateAndNotThrowWhenRoleIsBlank() {
        request.addHeader(USER_ID_HEADER, "user-1");
        request.addHeader(ROLE_HEADER, "   ");

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertRequestContinued();
    }

    // ---------- missing headers ----------

    @Test
    void shouldNotAuthenticateWhenNoHeaders() throws Exception {
        assertNull(runFilter());
        assertRequestContinued();
    }

    @Test
    void shouldNotAuthenticateWhenUserIdIsMissing() throws Exception {
        request.addHeader(ROLE_HEADER, Role.SELLER.name());

        assertNull(runFilter());
        assertRequestContinued();
    }

    @Test
    void shouldNotAuthenticateWhenUserIdIsBlank() throws Exception {
        request.addHeader(USER_ID_HEADER, "   ");
        request.addHeader(ROLE_HEADER, Role.SELLER.name());

        assertNull(runFilter());
        assertRequestContinued();
    }

    @Test
    void shouldNotAuthenticateWhenRoleIsMissing() throws Exception {
        request.addHeader(USER_ID_HEADER, "seller-1");

        assertNull(runFilter());
        assertRequestContinued();
    }

    // ---------- configuration ----------

    @Test
    void shouldReadTheHeaderNamesItWasConfiguredWith() throws Exception {
        filter = new HeaderAuthFilter("X-Custom-Id", "X-Custom-Role");
        request.addHeader("X-Custom-Id", "seller-1");
        request.addHeader("X-Custom-Role", "SELLER");

        Authentication auth = runFilter();

        assertNotNull(auth);
        assertEquals("seller-1", auth.getName());
    }

    @Test
    void shouldIgnoreDefaultHeadersWhenConfiguredWithOtherNames() throws Exception {
        filter = new HeaderAuthFilter("X-Custom-Id", "X-Custom-Role");
        request.addHeader(USER_ID_HEADER, "seller-1");
        request.addHeader(ROLE_HEADER, "SELLER");

        assertNull(runFilter());
    }
}