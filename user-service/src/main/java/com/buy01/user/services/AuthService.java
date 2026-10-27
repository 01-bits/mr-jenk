package com.buy01.user.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.buy01.user.DTO.AuthDTO;
import com.buy01.user.DTO.AuthDTO.LoginRequest;
import com.buy01.user.DTO.AuthDTO.RegisterRequest;
import com.buy01.user.exception.DuplicateResourceException;
import com.buy01.user.model.User;
import com.buy01.user.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService  {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    

    public ResponseEntity<AuthDTO.AuthResponse> registerService(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists.");
        }

        User newUser = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        User savedUser = userRepository.save(newUser);

        String token = jwtService.generateToken(savedUser);

        AuthDTO.AuthResponse response = AuthDTO.AuthResponse.from(savedUser, token);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<AuthDTO.AuthResponse> loginService(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = (User) authentication.getPrincipal();

        String token = jwtService.generateToken(user);

        AuthDTO.AuthResponse response = AuthDTO.AuthResponse.from(user, token);

        return ResponseEntity.ok(response);
    }
}
