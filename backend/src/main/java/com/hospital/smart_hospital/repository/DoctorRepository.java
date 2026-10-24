package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {
}