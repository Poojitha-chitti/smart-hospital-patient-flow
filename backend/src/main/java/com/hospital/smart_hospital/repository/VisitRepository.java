package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Integer> {
}