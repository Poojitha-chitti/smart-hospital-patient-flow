package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Visit;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.VisitRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;

    public PatientController(PatientRepository patientRepository,
                             VisitRepository visitRepository) {
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registerPatient(@RequestBody Patient patientRequest) {

        if (patientRequest.getPatient_name() == null ||
                patientRequest.getPatient_name().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Patient name is required."));
        }

        if (patientRequest.getAge() == null || patientRequest.getAge() < 0 || patientRequest.getAge() > 120) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please enter a valid age."));
        }

        if (patientRequest.getDepartment() == null ||
                patientRequest.getDepartment().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Department is required."));
        }

        LocalDateTime now = LocalDateTime.now();
        Integer nextVisitId = visitRepository.findMaxVisitId() + 1;

        Visit visit = new Visit();
visit.setVisit_id(nextVisitId);
visit.setVisit_date(LocalDate.now());
visit.setArrival_time(now);
visit.setPatient_type(patientRequest.getDepartment());
visit.setStatus("WAITING");
        visitRepository.save(visit);

        Patient patient = new Patient();
        patient.setVisit_id(nextVisitId);
        patient.setPatient_name(patientRequest.getPatient_name().trim());
        patient.setAge(patientRequest.getAge());
        patient.setGender(patientRequest.getGender());
        patient.setPhone(patientRequest.getPhone());
        patient.setDepartment(patientRequest.getDepartment());
        patient.setRegistration_time(now);
        patient.setStatus("WAITING");

        Patient saved = patientRepository.save(patient);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Patient registered successfully.");
        response.put("patient", saved);
        response.put("visit_id", nextVisitId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<Patient> getPatients() {
        return patientRepository.findAllPatientsNewestFirst();
    }
}
