package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Integer> {

    @Query("""
        SELECT s
        FROM Staff s
        WHERE s.department = :department
        AND s.available = true
        ORDER BY s.staff_id ASC
    """)
    List<Staff> findAvailableStaff(String department);
}