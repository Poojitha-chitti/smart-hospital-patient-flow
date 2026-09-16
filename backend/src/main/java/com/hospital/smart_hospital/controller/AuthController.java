package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        Optional<User> existingUser =
                userRepository.findByUsername(user.getUsername());

        if (existingUser.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }

        user.setRole("STAFF");

        User savedUser = userRepository.save(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registration successful",
                        "username", savedUser.getUsername(),
                        "role", savedUser.getRole()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {

        Optional<User> existingUser =
                userRepository.findByUsername(user.getUsername());

        if (existingUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        User databaseUser = existingUser.get();

        if (!databaseUser.getPassword().equals(user.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", "Login successful",
                        "username", databaseUser.getUsername(),
                        "role", databaseUser.getRole()
                )
        );
    }
    @PostMapping("/change-password")
public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {

    String username = request.get("username");
    String currentPassword = request.get("currentPassword");
    String newPassword = request.get("newPassword");

    if (username == null || currentPassword == null || newPassword == null) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "All fields are required"));
    }

    Optional<User> existingUser =
            userRepository.findByUsername(username);

    if (existingUser.isEmpty()) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "User not found"));
    }

    User databaseUser = existingUser.get();

    // Only ADMIN can change password through this endpoint
    if (!"ADMIN".equalsIgnoreCase(databaseUser.getRole())) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Only admin can change password"));
    }

    // Check current password
    if (!databaseUser.getPassword().equals(currentPassword)) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Current password is incorrect"));
    }

    if (newPassword.length() < 6) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "New password must be at least 6 characters"));
    }

    databaseUser.setPassword(newPassword);
    userRepository.save(databaseUser);

    return ResponseEntity.ok(
            Map.of("message", "Password changed successfully")
    );
}
}