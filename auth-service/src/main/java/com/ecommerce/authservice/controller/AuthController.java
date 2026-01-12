package com.ecommerce.authservice.controller;

import com.ecommerce.authservice.config.JwtUtil;
import com.ecommerce.authservice.dto.LoginRequest;
import com.ecommerce.authservice.dto.RegisterRequest;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication API", description = "User registration and login")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        // Map DTO to entity
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setEmail(req.getEmail());

        // Set role from request if provided and valid, else default to ROLE_USER
        String requestedRole = req.getRole();
        if (requestedRole != null) {
            String normalized = requestedRole.trim().toUpperCase();
            if (normalized.equals("ROLE_ADMIN") || normalized.equals("ADMIN")) {
                user.setRole("ROLE_ADMIN");
            } else {
                user.setRole("ROLE_USER");
            }
        } else {
            user.setRole("ROLE_USER");
        }

        User saved = authService.register(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("username", saved.getUsername());
        resp.put("email", saved.getEmail());
        resp.put("role", saved.getRole() != null ? saved.getRole().name() : null);

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @Operation(summary = "Login and generate JWT token")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req.getUsername(), req.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @Operation(summary = "Validate JWT token")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestParam String token) {
        boolean isValid = jwtUtil.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @Operation(summary = "Get role from token")
    @GetMapping("/role")
    public ResponseEntity<Map<String, String>> getRole(@RequestParam String token) {
        String role = jwtUtil.extractRole(token);
        return ResponseEntity.ok(Map.of("role", role));
    }

}
