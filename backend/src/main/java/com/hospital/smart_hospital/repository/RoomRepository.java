package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Integer> {
}