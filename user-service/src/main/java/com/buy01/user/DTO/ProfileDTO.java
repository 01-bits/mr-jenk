package com.buy01.user.DTO;

import com.buy01.user.model.Role;
import com.buy01.user.model.User;

import lombok.Builder;

public class ProfileDTO {

    @Builder
    public static record ProfileResponse(
        String id,
        String name,
        String email,
        Role role,
        String avatar
    ) {
        public static ProfileResponse from(User user) {
            return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatar()
            );
        }
    }
}
