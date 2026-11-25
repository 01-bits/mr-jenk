package com.buy01.user.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buy01.user.DTO.ProfileDTO;
import com.buy01.user.services.ProfileService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/profile/me")
@AllArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ProfileDTO.ProfileResponse> getUser(Principal principal) {
        return profileService.getProfile(principal.getName());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> updateAvatar(@RequestParam("file") MultipartFile file,
            Authentication auth) {
        String mediaId = profileService.updateAvatar(auth.getName(), file);
        return ResponseEntity.ok(Map.of("MediaId", mediaId));
    }
}