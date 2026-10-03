package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public RoomController(
            RoomRepository roomRepository,
            UserRepository userRepository) {

        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }


    /*
     * ============================================================
     * GET ALL ROOMS
     * ============================================================
     *
     * Existing functionality.
     */
    @GetMapping
    public List<Room> getRooms() {

        return roomRepository.findAll();
    }


    /*
     * ============================================================
     * UPDATE ROOM STATUS
     * ============================================================
     *
     * Only an actual ADMIN account can update
     * room availability.
     */
    @PutMapping("/{roomId}/status")
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable Integer roomId,
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
         * Verify that the account is actually ADMIN.
         */
        if (user.getRole() == null ||
                !"ADMIN".equalsIgnoreCase(
                        user.getRole().trim())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Only administrators can update room status."
                    ));
        }


        /*
         * Find room.
         */
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElse(null);


        if (room == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Room not found."
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
                            "Room status is required."
                    ));
        }


        String normalizedStatus =
                status.trim().toUpperCase();


        /*
         * Only these room statuses are allowed.
         */
        if (!normalizedStatus.equals("AVAILABLE") &&
                !normalizedStatus.equals("OCCUPIED") &&
                !normalizedStatus.equals("MAINTENANCE")) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid room status."
                    ));
        }


        /*
         * Update existing room status.
         */
        room.setStatus(
                normalizedStatus
        );


        Room savedRoom =
                roomRepository.save(
                        room
                );


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Room status updated successfully.",

                        "room_id",
                        savedRoom.getRoom_id(),

                        "room_no",
                        savedRoom.getRoom_no(),

                        "status",
                        savedRoom.getStatus()
                )
        );
    }
}