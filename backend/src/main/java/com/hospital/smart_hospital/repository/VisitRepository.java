package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VisitRepository extends JpaRepository<Visit, Integer> {

    @Query("SELECT COUNT(v) FROM Visit v")
    long countTotalVisits();
}