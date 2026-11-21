package com.buy01.user.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.user.DTO.MediaResponse;
import com.buy01.user.DTO.ProfileDTO.ProfileResponse;
import com.buy01.user.client.MediaService;
import com.buy01.user.exception.ResourceNotFoundException;
import com.buy01.user.model.Role;
import com.buy01.user.model.User;
import com.buy01.user.repository.UserRepository;

import feign.FeignException;

/**
 * ProfileServiceTest
 */


@ExtendWith (MockitoExtension.class)
public class ProfileServiceTest {
 
    @Mock 
    private UserRepository userRepository;
 
    @Mock
    private MediaService mediaService;
 
    @InjectMocks 
    private ProfileService profileService;
 
    private User user;
    private MultipartFile file;
 
    @BeforeEach 
    void setUp() {
        user = User.builder()
                .id("user-1")
                .name("User")
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.SELLER)
                .avatar("old-media")
                .build();
 
        file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2, 3 });
    }
 
    private static MediaResponse uploaded(String id) {
        return new MediaResponse(id, "/media/images/" + id, null, "image/png", 3, Instant.now());
    }
 
    // ---------- loadUserByUsername (used by login) ----------
 
    @Test 
    void loadUserByUsername_shouldReturnUserByEmail() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
 
        assertSame(user, profileService.loadUserByUsername("user@example.com"));
    }
 
    @Test
    void loadUserByUsername_shouldThrowWhenEmailUnknown() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
 
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> profileService.loadUserByUsername("unknown@example.com"));
 
        assertEquals("Invalid credentials: email or password is incorrect.", ex.getMessage());
    }
 
    // ---------- getProfile ----------
 
    @Test
    void getProfile_shouldReturnProfile() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
 
        ProfileResponse profile = profileService.getProfile("user-1").getBody();
 
        assertEquals("user-1", profile.id());
        assertEquals("User", profile.name());
        assertEquals("user@example.com", profile.email());
        assertEquals(Role.SELLER, profile.role());
        assertEquals("old-media", profile.avatar());
    }
 
    @Test
    void getProfile_shouldThrowWhenUserMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
 
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> profileService.getProfile("missing"));
 
        assertEquals("User not found with id: missing", ex.getMessage());
    }
 
    // ---------- updateAvatar ----------
 
    @Test
    void updateAvatar_shouldSaveMediaIdAsAvatar() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(mediaService.upload(List.of(file), null)).thenReturn(List.of(uploaded("media-1")));
 
        String mediaId = profileService.updateAvatar("user-1", file);
 
        assertEquals("media-1", mediaId);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("media-1", saved.getValue().getAvatar());
    }
 
    @Test
    void updateAvatar_shouldNotUploadWhenUserMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
 
        assertThrows(ResourceNotFoundException.class,
                () -> profileService.updateAvatar("missing", file));
 
        // no orphan image in media-service
        verifyNoInteractions(mediaService);
    }
 
    @Test
    void updateAvatar_shouldFailWhenMediaResponseEmpty() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(mediaService.upload(anyList(), isNull())).thenReturn(List.of());
 
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> profileService.updateAvatar("user-1", file));
 
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }
 
    @Test
    void updateAvatar_shouldNotSaveUserWhenUploadFails() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(mediaService.upload(anyList(), isNull())).thenThrow(FeignException.class);
 
        // propagated untouched: GlobalExceptionHandler decides the HTTP status
        assertThrows(FeignException.class, () -> profileService.updateAvatar("user-1", file));
 
        verify(userRepository, never()).save(any(User.class));
        assertEquals("old-media", user.getAvatar());
    }
}