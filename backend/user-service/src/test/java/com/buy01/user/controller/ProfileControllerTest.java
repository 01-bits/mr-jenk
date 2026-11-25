package com.buy01.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;

import com.buy01.user.DTO.ProfileDTO.ProfileResponse;
import com.buy01.user.config.SecurityConfig;
import com.buy01.user.model.Role;
import com.buy01.user.services.ProfileService;

import feign.FeignException;

/**
 * ProfileControllerTest
 */
@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "AUTH_USER_ID_HEADER=X-User-Id",
        "AUTH_USER_ROLE_HEADER=X-User-Role"
})
public class ProfileControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-User-Role";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    // ---------- helpers ----------

    private static <B extends AbstractMockHttpServletRequestBuilder<?>> B as(String userId, String role, B builder) {
        builder.header(USER_ID_HEADER, userId).header(ROLE_HEADER, role);
        return builder;
    }

    /** The avatar endpoint is a multipart PUT, not the default multipart POST. */
    private static AbstractMockHttpServletRequestBuilder<?> avatarUpload() {
        return multipart(HttpMethod.PUT, "/profile/me")
                .file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2, 3 }));
    }

    // ---------- GET /profile/me ----------

    @Test
    void getProfile_shouldReturnProfileOfCaller() throws Exception {
        when(profileService.getProfile("user-1")).thenReturn(ResponseEntity.ok(
                new ProfileResponse("user-1", "User", "user@example.com", Role.CLIENT, null)));

        mockMvc.perform(as("user-1", "SELLER", get("/profile/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.email").value("user@example.com"));

        // the id comes from the identity header, never from the request body or URL
        verify(profileService).getProfile("user-1");
    }

    @Test
    void getProfile_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/profile/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(profileService);
    }

    // ---------- PUT /profile/me ----------

    @Test
    void updateAvatar_shouldReturnMediaIdForSeller() throws Exception {
        when(profileService.updateAvatar(eq("user-1"), any(MultipartFile.class))).thenReturn("media-1");

        mockMvc.perform(as("user-1", "SELLER", avatarUpload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MediaId").value("media-1"));
    }

    @Test
    void updateAvatar_shouldReturn403ForClient() throws Exception {
        mockMvc.perform(as("user-1", "CLIENT", avatarUpload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        verifyNoInteractions(profileService);
    }

    @Test
    void updateAvatar_shouldForwardMediaServiceRejection() throws Exception {
        // a 400 answered by media-service, e.g. a file that is not an image
        FeignException rejected = mock(FeignException.class);
        when(rejected.status()).thenReturn(400);
        when(rejected.contentUTF8()).thenReturn("{\"status\":400,\"message\":\"Only image files are allowed\"}");
        when(profileService.updateAvatar(eq("user-1"), any(MultipartFile.class))).thenThrow(rejected);

        mockMvc.perform(as("user-1", "SELLER", avatarUpload()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only image files are allowed"));
    }

    @Test
    void updateAvatar_shouldReturn503WhenMediaServiceDown() throws Exception {
        // created without a response: status() is 0, like a refused connection
        when(profileService.updateAvatar(eq("user-1"), any(MultipartFile.class)))
                .thenThrow(FeignException.class);

        mockMvc.perform(as("user-1", "SELLER", avatarUpload()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Media service unavailable"));
    }
}
