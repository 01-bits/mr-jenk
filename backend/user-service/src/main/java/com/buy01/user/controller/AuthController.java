package com.buy01.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.user.DTO.AuthDTO;
import com.buy01.user.services.AuthService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping ("/auth")
@AllArgsConstructor 
public class AuthController {

    private final AuthService authService;

    @PostMapping ("/register")
    public ResponseEntity<AuthDTO.AuthResponse> registerController(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        return authService.registerService(request);
    }

    @PostMapping ("/login")
    public ResponseEntity<AuthDTO.AuthResponse> loginController(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return authService.loginService(request);
    }

}
