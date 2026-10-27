package com.buy01.user.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.buy01.user.DTO.ProfileDTO;
import com.buy01.user.exception.ResourceNotFoundException;
import com.buy01.user.model.User;
import com.buy01.user.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service 
@AllArgsConstructor 
public class ProfileService implements UserDetailsService {

    private final UserRepository userRepository;

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
}
