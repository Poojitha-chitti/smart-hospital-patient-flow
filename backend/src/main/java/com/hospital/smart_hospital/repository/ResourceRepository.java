package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    @Query("""
        SELECT r
        FROM Resource r
        WHERE r.department = :department
        ORDER BY r.resource_id ASC
    """)
    Optional<Resource> findFirstByDepartment(String department);
}