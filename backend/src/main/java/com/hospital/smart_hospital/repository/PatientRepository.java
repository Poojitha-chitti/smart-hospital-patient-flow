package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
    @Query("SELECT p FROM Patient p ORDER BY p.patient_id DESC")
    List<Patient> findAllPatientsNewestFirst();
}
