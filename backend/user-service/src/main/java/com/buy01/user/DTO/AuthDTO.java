package com.buy01.user.DTO;

import com.buy01.user.model.Role;
import com.buy01.user.model.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class AuthDTO {
    public static record RegisterRequest(
        @NotBlank (message = "Name is required")
        @Size(min = 2, message = "Name must be at least 2 characters long")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email, 

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password,

        @NotNull(message = "Role is required")
        Role role, 

        String avatar
    ) {}

    public static record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    @Builder
    public static record AuthResponse(
        String id,
        String name,
        String email,
        Role role,
        String avatar,
        String token
    ) {
        public static AuthResponse from(User user, String token) {
            return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatar(),
                token
            );
        }
    }
}