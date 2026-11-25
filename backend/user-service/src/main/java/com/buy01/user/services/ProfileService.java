package com.buy01.user.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.user.DTO.MediaResponse;
import com.buy01.user.DTO.ProfileDTO;
import com.buy01.user.client.MediaService;
import com.buy01.user.exception.ResourceNotFoundException;
import com.buy01.user.model.User;
import com.buy01.user.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProfileService implements UserDetailsService {

    private final UserRepository userRepository;
    private final MediaService mediaService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username).orElseThrow(
                () -> new UsernameNotFoundException("Invalid credentials: email or password is incorrect."));
        return user;
    }

    public ResponseEntity<ProfileDTO.ProfileResponse> getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ProfileDTO.ProfileResponse response = ProfileDTO.ProfileResponse.from(user);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ProfileDTO.ProfileResponse> getProfileService(String userId) {
        return getProfile(userId);
    }

    public String updateAvatar(String userId, MultipartFile file) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // FeignException is not caught here: GlobalExceptionHandler translates it
        List<MediaResponse> uploaded = mediaService.upload(List.of(file), null);

        if (uploaded == null || uploaded.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from media service");
        }

        String mediaId = uploaded.getFirst().id();
        user.setAvatar(mediaId);
        userRepository.save(user);
        return mediaId;
    }
}
