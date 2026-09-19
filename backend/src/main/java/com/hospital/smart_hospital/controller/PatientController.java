package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Visit;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.VisitRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hospital.smart_hospital.model.WorkflowEvent;
import com.hospital.smart_hospital.repository.WorkflowEventRepository;

import java.time.LocalDate;
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
    private final WorkflowEventRepository workflowEventRepository;

    public PatientController(
        PatientRepository patientRepository,
        VisitRepository visitRepository,
        WorkflowEventRepository workflowEventRepository) {

    this.patientRepository = patientRepository;
    this.visitRepository = visitRepository;
    this.workflowEventRepository = workflowEventRepository;
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
if ("OP".equalsIgnoreCase(patient.getDepartment())) {

    WorkflowEvent workflowEvent = new WorkflowEvent();

    workflowEvent.setVisit_id(nextVisitId);
    workflowEvent.setStage("OP");
    workflowEvent.setQueue_entry_time(now);

    workflowEventRepository.save(workflowEvent);
}
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
@PutMapping("/{patientId}/status")
public ResponseEntity<?> updatePatientStatus(
        @PathVariable Integer patientId,
        @RequestBody Map<String, String> request) {

    Patient patient = patientRepository.findById(patientId).orElse(null);

    if (patient == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Patient not found."));
    }

    String status = request.get("status");

    if (status == null || status.trim().isEmpty()) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Status is required."));
    }

    String newStatus = status.trim().toUpperCase();

    if ("IN CONSULTATION".equals(newStatus)) {

    LocalDateTime startTime = LocalDateTime.now();

    patient.setConsultation_start_time(startTime);

    WorkflowEvent workflowEvent =
            workflowEventRepository.findByVisitIdAndStage(
                    patient.getVisit_id(),
                    "OP"
            );

    if (workflowEvent != null) {
        workflowEvent.setService_start_time(startTime);
        workflowEventRepository.save(workflowEvent);
    }
}

   if ("COMPLETED".equals(newStatus)) {

    LocalDateTime endTime = LocalDateTime.now();

    patient.setConsultation_end_time(endTime);

    WorkflowEvent workflowEvent =
            workflowEventRepository.findByVisitIdAndStage(
                    patient.getVisit_id(),
                    "OP"
            );

    if (workflowEvent != null) {
        workflowEvent.setService_end_time(endTime);
        workflowEventRepository.save(workflowEvent);
    }
}

    patient.setStatus(newStatus);

    Patient updatedPatient = patientRepository.save(patient);

    return ResponseEntity.ok(updatedPatient);
}}
