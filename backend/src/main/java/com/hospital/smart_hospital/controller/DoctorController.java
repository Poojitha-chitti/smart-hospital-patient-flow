package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.Doctor;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.DoctorRepository;
import com.hospital.smart_hospital.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public DoctorController(
            DoctorRepository doctorRepository,
            UserRepository userRepository) {

        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }


    /*
     * ============================================================
     * GET ALL DOCTORS
     * ============================================================
     *
     * Existing functionality.
     */
    @GetMapping
    public List<Doctor> getDoctors() {

        return doctorRepository.findAll();
    }


    /*
     * ============================================================
     * UPDATE DOCTOR STATUS
     * ============================================================
     *
     * Only an actual ADMIN account can update
     * doctor availability.
     */
    @PutMapping("/{doctorId}/status")
    public ResponseEntity<?> updateDoctorStatus(
            @PathVariable Integer doctorId,
            @RequestBody Map<String, String> request) {

        /*
         * Get logged-in username.
         */
        String username =
                request.get("username");

        if (username == null ||
                username.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Logged-in user is required."
                    ));
        }


        /*
         * Find the actual user from the database.
         */
        User user =
                userRepository
                        .findByUsername(
                                username.trim()
                        )
                        .orElse(null);


        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Logged-in user not found."
                    ));
        }


        /*
         * Verify that the database account
         * is actually an ADMIN.
         */
        if (user.getRole() == null ||
                !"ADMIN".equalsIgnoreCase(
                        user.getRole().trim())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Only administrators can update doctor status."
                    ));
        }


        /*
         * Find doctor.
         */
        Doctor doctor =
                doctorRepository
                        .findById(doctorId)
                        .orElse(null);


        if (doctor == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Doctor not found."
                    ));
        }


        /*
         * Get requested status.
         */
        String status =
                request.get("status");


        if (status == null ||
                status.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Doctor status is required."
                    ));
        }


        String normalizedStatus =
                status.trim().toUpperCase();


        /*
         * Only these three statuses
         * are allowed.
         */
        if (!normalizedStatus.equals("AVAILABLE") &&
                !normalizedStatus.equals("BUSY") &&
                !normalizedStatus.equals("UNAVAILABLE")) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid doctor status."
                    ));
        }


        /*
         * Update existing status.
         */
        doctor.setStatus(
                normalizedStatus
        );


        Doctor savedDoctor =
                doctorRepository.save(
                        doctor
                );


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Doctor status updated successfully.",

                        "doctor_id",
                        savedDoctor.getDoctor_id(),

                        "doctor_name",
                        savedDoctor.getDoctor_name(),

                        "status",
                        savedDoctor.getStatus()
                )
        );
    }
}