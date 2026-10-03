package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffUsers() {

        List<Map<String, Object>> staffUsers =
                userRepository.findAll()
                        .stream()
                        .filter(user ->
                                "STAFF".equalsIgnoreCase(
                                        user.getRole()
                                )
                        )
                        .map(user -> {

                            Map<String, Object> data =
                                    new HashMap<>();

                            data.put(
                                    "user_id",
                                    user.getUser_id()
                            );

                            data.put(
                                    "username",
                                    user.getUsername()
                            );

                            data.put(
                                    "email",
                                    user.getEmail()
                            );

                            data.put(
                                    "role",
                                    user.getRole()
                            );

                            data.put(
                                    "work_area",
                                    user.getWork_area() == null
                                            ? ""
                                            : user.getWork_area()
                            );

                            data.put(
                                    "staff_id",
                                    user.getStaff_id()
                            );

                            return data;
                        })
                        .collect(Collectors.toList());

        return ResponseEntity.ok(staffUsers);
    }

    @PutMapping("/{userId}/work-area")
    public ResponseEntity<?> updateWorkArea(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> request) {

        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "User not found."
                    ));
        }

        if (!"STAFF".equalsIgnoreCase(
                user.getRole())) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Work area can only be assigned to staff."
                    ));
        }

        String workArea =
                request.get("work_area");

        if (workArea == null ||
                workArea.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Please select a work area."
                    ));
        }

        String normalizedArea =
                workArea.trim().toUpperCase();

        if (!normalizedArea.equals("REGISTRATION")
                && !normalizedArea.equals("OP")
                && !normalizedArea.equals("PHARMACY")
                && !normalizedArea.equals("DIAGNOSTICS")) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid work area."
                    ));
        }

        user.setWork_area(normalizedArea);

        User savedUser =
                userRepository.save(user);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Work area updated successfully.",
                        "user_id",
                        savedUser.getUser_id(),
                        "username",
                        savedUser.getUsername(),
                        "work_area",
                        savedUser.getWork_area()
                )
        );
    }

    @PutMapping("/{userId}/staff-id")
    public ResponseEntity<?> updateStaffId(
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> request) {

        User user =
                userRepository
                        .findById(userId)
                        .orElse(null);

        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "User not found."
                    ));
        }

        if (!"STAFF".equalsIgnoreCase(
                user.getRole())) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Staff ID can only be assigned to staff accounts."
                    ));
        }

        Object staffIdValue =
                request.get("staff_id");

        if (staffIdValue == null) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Please select a staff member."
                    ));
        }

        Integer staffId;

        try {
            staffId =
                    Integer.valueOf(
                            staffIdValue.toString()
                    );
        } catch (NumberFormatException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid staff ID."
                    ));
        }

        if (staffId <= 0) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid staff ID."
                    ));
        }

        user.setStaff_id(staffId);

        User savedUser =
                userRepository.save(user);

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                "Staff account linked successfully."
        );

        response.put(
                "user_id",
                savedUser.getUser_id()
        );

        response.put(
                "username",
                savedUser.getUsername()
        );

        response.put(
                "staff_id",
                savedUser.getStaff_id()
        );

        return ResponseEntity.ok(response);
    }
}