package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.PasswordResetService;
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
    private final PasswordResetService passwordResetService;

    public AuthController(UserRepository userRepository,
                          PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (user.getUsername() == null || user.getUsername().isBlank() ||
                user.getPassword() == null || user.getPassword().isBlank() ||
                user.getEmail() == null || user.getEmail().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Username, email and password are required"));
        }

        Optional<User> existingUser =
                userRepository.findByUsername(user.getUsername());

        if (existingUser.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }

        Optional<User> existingEmail =
                userRepository.findByEmail(user.getEmail());

        if (existingEmail.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        user.setRole("STAFF");

        User savedUser = userRepository.save(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registration successful",
                        "username", savedUser.getUsername(),
                        "email", savedUser.getEmail(),
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
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request) {

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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Email is required"));
        }

        String normalizedEmail = email.trim().toLowerCase();

        var user = userRepository.findByEmail(normalizedEmail);

        if (user.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "No account found with this email"));
        }

        passwordResetService.sendOtp(normalizedEmail);

        return ResponseEntity.ok(
                Map.of("message", "OTP sent successfully to your registered email")
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");

        if (email == null || email.trim().isEmpty() ||
                otp == null || otp.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Email and OTP are required"));
        }

        String normalizedEmail = email.trim().toLowerCase();
        String enteredOtp = otp.trim();

        boolean verified =
                passwordResetService.verifyOtp(normalizedEmail, enteredOtp);

        if (!verified) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid or expired OTP"));
        }

        return ResponseEntity.ok(
                Map.of("message", "OTP verified successfully")
        );
    }
    @PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(
        @RequestBody Map<String, String> request) {

    String email = request.get("email");
    String newPassword = request.get("newPassword");

    if (email == null || email.trim().isEmpty() ||
            newPassword == null || newPassword.isEmpty()) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "Email and new password are required"));
    }

    if (newPassword.length() < 6) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "New password must be at least 6 characters"));
    }

    String normalizedEmail = email.trim().toLowerCase();

    Optional<User> existingUser =
            userRepository.findByEmail(normalizedEmail);

    if (existingUser.isEmpty()) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "No account found with this email"));
    }

    User databaseUser = existingUser.get();

    databaseUser.setPassword(newPassword);
    userRepository.save(databaseUser);

    return ResponseEntity.ok(
            Map.of("message", "Password reset successfully")
    );
}
}