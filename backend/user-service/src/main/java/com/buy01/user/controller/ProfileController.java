package com.buy01.user.controller;

import java.security.Principal;
import java.util.List;
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

import com.buy01.user.DTO.MediaResponse;
import com.buy01.user.DTO.ProfileDTO;
import com.buy01.user.client.MediaService;
import com.buy01.user.model.User;
import com.buy01.user.repository.UserRepository;
import com.buy01.user.services.ProfileService;

import feign.FeignException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/profile/me")
@AllArgsConstructor
public class ProfileController {

    private final UserRepository userRepo;
    private final ProfileService profileService;
    private final MediaService mediaFeignClient;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ProfileDTO.ProfileResponse> getUser(Principal principal) {
        return profileService.getProfile(principal.getName());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> updateAvatar(@RequestParam("file") MultipartFile file,
            Authentication auth) {

        List<MultipartFile> body = List.of(file);

        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        try {
            // If your MediaService is configured to return a direct List instead of
            // ResponseEntity
            List<MediaResponse> responseBody = mediaFeignClient.upload(body, null);

            if (responseBody == null || responseBody.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of("error", "Invalid response from media service"));
            }

            User user = userRepo.findById(auth.getName()).get();
            user.setAvatar(responseBody.getFirst().id());
            userRepo.save(user);

            return ResponseEntity.ok(Map.of("MediaId", responseBody.getFirst().id()));

        } catch (FeignException e) {
            System.out.println(
                    "#################################### Caught Feign Exception ####################################");

            // Extract the original status code (e.g., 400) and error message/body from the
            // media service
            int status = e.status();
            String errorBody = e.contentUTF8();

            return ResponseEntity
                    .status(status > 0 ? status : 400)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody);
        }
    }
}