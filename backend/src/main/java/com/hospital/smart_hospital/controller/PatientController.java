package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.model.Visit;
import com.hospital.smart_hospital.model.WorkflowEvent;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.repository.VisitRepository;
import com.hospital.smart_hospital.repository.WorkflowEventRepository;
import java.time.temporal.ChronoUnit;
import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final UserRepository userRepository;

    public PatientController(
            PatientRepository patientRepository,
            VisitRepository visitRepository,
            WorkflowEventRepository workflowEventRepository,
            UserRepository userRepository) {

        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registerPatient(
            @RequestBody Patient patientRequest) {

        if (patientRequest.getPatient_name() == null ||
                patientRequest.getPatient_name().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Patient name is required."
                    ));
        }

        if (patientRequest.getAge() == null ||
                patientRequest.getAge() < 0 ||
                patientRequest.getAge() > 120) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Please enter a valid age."
                    ));
        }

        if (patientRequest.getDepartment() == null ||
                patientRequest.getDepartment().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Department is required."
                    ));
        }

        LocalDateTime now = LocalDateTime.now();

        Integer nextVisitId =
                visitRepository.findMaxVisitId() + 1;

        Visit visit = new Visit();

        visit.setVisit_id(nextVisitId);

// Generate a separate display ID for each patient type.
boolean isEmergency =
        "EMERGENCY".equalsIgnoreCase(
                patientRequest.getDepartment().trim()
        );

String displayVisitId;

if (isEmergency) {
    long emergencyCount =
            visitRepository.countByPatientTypeIgnoreCase("EMERGENCY") + 1;

    displayVisitId = String.format("E%03d", emergencyCount);
} else {
    displayVisitId = String.format("OPD-%04d", nextVisitId);
}

visit.setDisplay_visit_id(displayVisitId);
visit.setVisit_date(LocalDate.now());
visit.setArrival_time(now);
visit.setPatient_type(
        patientRequest.getDepartment()
);
visit.setStatus("WAITING");

        visitRepository.save(visit);

        Patient patient = new Patient();

        patient.setVisit_id(nextVisitId);
        patient.setPatient_name(
                patientRequest.getPatient_name().trim()
        );
        patient.setAge(patientRequest.getAge());
        patient.setGender(patientRequest.getGender());
        patient.setPhone(patientRequest.getPhone());
        patient.setDepartment(
                patientRequest.getDepartment()
        );
       patient.setRegistration_time(now);

boolean isEmergencyPatient =
        "EMERGENCY".equalsIgnoreCase(
                patientRequest.getDepartment().trim()
        );

patient.setStatus(
        isEmergencyPatient ? "TRIAGE WAITING" : "WAITING"
);
        Patient saved =
                patientRepository.save(patient);
saved.setDisplay_visit_id(displayVisitId);
        /*
         * ============================================================
         * CREATE OP WORKFLOW
         * ============================================================
         *
         * Department represents the patient's medical department.
         * OP is a workflow stage.
         *
         * Therefore, normal medical departments can enter
         * the OP consultation workflow.
         *
         * Emergency is kept separate for now.
         */

        if (isEmergencyPatient) {

    WorkflowEvent triageEvent = new WorkflowEvent();
    triageEvent.setVisit_id(nextVisitId);
    triageEvent.setStage("TRIAGE");
    triageEvent.setQueue_entry_time(now);

    workflowEventRepository.save(triageEvent);

} else {

    WorkflowEvent opEvent = new WorkflowEvent();
    opEvent.setVisit_id(nextVisitId);
    opEvent.setStage("OP");
    opEvent.setQueue_entry_time(now);

    workflowEventRepository.save(opEvent);
}

        Map<String, Object> response = new HashMap<>();

response.put(
        "message",
        "Patient registered successfully."
);

response.put("patient", saved);
response.put("visit_id", nextVisitId);
response.put("display_visit_id", displayVisitId);

return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
    }

    @GetMapping
    public List<Patient> getPatients(
            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "50")
            int size) {

        if (page < 0) {
            page = 0;
        }

        if (size <= 0 || size > 100) {
            size = 50;
        }

        List<Patient> patients =
                patientRepository
                        .findAllPatientsNewestFirst(
                                org.springframework.data.domain
                                        .PageRequest.of(
                                                page,
                                                size
                                        )
                        );

        /*
         * Add Pharmacy start/end times from
         * the existing workflow_events table.
         *
         * Nothing is inserted or changed here.
         */
        for (Patient patient : patients) {

            if (patient.getVisit_id() == null) {
                continue;
            }
Visit visit = visitRepository
        .findById(patient.getVisit_id())
        .orElse(null);

if (visit != null) {
    patient.setDisplay_visit_id(
            visit.getDisplay_visit_id()
    );
}
String currentStatus = patient.getStatus() == null
        ? ""
        : patient.getStatus().trim().toUpperCase();

String currentStage = null;

if ("TRIAGE WAITING".equals(currentStatus)) {
    currentStage = "TRIAGE";
} else if ("EMERGENCY TREATMENT WAITING".equals(currentStatus)) {
    currentStage = "EMERGENCY_TREATMENT";
} else if ("PHARMACY WAITING".equals(currentStatus)) {
    currentStage = "PHARMACY";
} else if ("DIAGNOSTICS WAITING".equals(currentStatus)) {
    currentStage = "DIAGNOSTICS";
} else if ("WAITING".equals(currentStatus)) {
    currentStage = "OP";
}

if (currentStage != null) {
    WorkflowEvent waitingEvent =
            workflowEventRepository.findByVisitIdAndStage(
                    patient.getVisit_id(),
                    currentStage
            );

    if (waitingEvent != null
            && waitingEvent.getQueue_entry_time() != null) {

        long waitingMinutes = ChronoUnit.MINUTES.between(
                waitingEvent.getQueue_entry_time(),
                LocalDateTime.now()
        );

        patient.setWaiting_time_minutes(
                Math.max(0L, waitingMinutes)
        );
    } else {
        patient.setWaiting_time_minutes(null);
    }
} else {
    patient.setWaiting_time_minutes(null);
}
            WorkflowEvent pharmacyEvent =
                    workflowEventRepository
                            .findByVisitIdAndStage(
                                    patient.getVisit_id(),
                                    "PHARMACY"
                            );

            if (pharmacyEvent != null) {

                patient.setPharmacy_start_time(
                        pharmacyEvent
                                .getService_start_time()
                );

                patient.setPharmacy_end_time(
                        pharmacyEvent
                                .getService_end_time()
                );
                patient.setPharmacy_queue_entry_time(
        pharmacyEvent.getQueue_entry_time());
            }
            WorkflowEvent diagnosticsEvent =
        workflowEventRepository
                .findByVisitIdAndStage(
                        patient.getVisit_id(),
                        "DIAGNOSTICS"
                );

if (diagnosticsEvent != null) {

    patient.setDiagnostics_start_time(
            diagnosticsEvent
                    .getService_start_time()
    );

    patient.setDiagnostics_end_time(
            diagnosticsEvent
                    .getService_end_time()
    );
    patient.setDiagnostics_queue_entry_time(
        diagnosticsEvent.getQueue_entry_time());
}
WorkflowEvent triageEvent =
        workflowEventRepository.findByVisitIdAndStage(
                patient.getVisit_id(),
                "TRIAGE"
        );

if (triageEvent != null) {
    patient.setTriage_start_time(
            triageEvent.getService_start_time()
    );
}
WorkflowEvent treatmentEvent =
        workflowEventRepository.findByVisitIdAndStage(
                patient.getVisit_id(), "EMERGENCY_TREATMENT");

if (treatmentEvent != null) {
    patient.setEmergency_treatment_start_time(
            treatmentEvent.getService_start_time());

    patient.setEmergency_treatment_end_time(
            treatmentEvent.getService_end_time());
            patient.setTriage_queue_entry_time(
        triageEvent.getQueue_entry_time());
        patient.setEmergency_treatment_queue_entry_time(
        treatmentEvent.getQueue_entry_time());
}
LocalDateTime now = LocalDateTime.now();

WorkflowEvent triage =
        workflowEventRepository.findByVisitIdAndStage(
                patient.getVisit_id(), "TRIAGE");

if (triage != null && triage.getQueue_entry_time() != null) {
    LocalDateTime end = triage.getService_start_time() != null
            ? triage.getService_start_time() : now;

    patient.setTriage_waiting_minutes(
            Math.max(0L, ChronoUnit.MINUTES.between(
                    triage.getQueue_entry_time(), end)));
}

WorkflowEvent treatment =
        workflowEventRepository.findByVisitIdAndStage(
                patient.getVisit_id(), "EMERGENCY_TREATMENT");

if (treatment != null && treatment.getQueue_entry_time() != null) {
    LocalDateTime end = treatment.getService_start_time() != null
            ? treatment.getService_start_time() : now;

    patient.setEmergency_treatment_waiting_minutes(
            Math.max(0L, ChronoUnit.MINUTES.between(
                    treatment.getQueue_entry_time(), end)));
}

WorkflowEvent diagnostics =
        workflowEventRepository.findByVisitIdAndStage(
                patient.getVisit_id(), "DIAGNOSTICS");

if (diagnostics != null && diagnostics.getQueue_entry_time() != null) {
    LocalDateTime end = diagnostics.getService_start_time() != null
            ? diagnostics.getService_start_time() : now;

    patient.setDiagnostics_waiting_minutes(
            Math.max(0L, ChronoUnit.MINUTES.between(
                    diagnostics.getQueue_entry_time(), end)));
}

WorkflowEvent pharmacy =
        workflowEventRepository.findByVisitIdAndStage(
                patient.getVisit_id(), "PHARMACY");

if (pharmacy != null && pharmacy.getQueue_entry_time() != null) {
    LocalDateTime end = pharmacy.getService_start_time() != null
            ? pharmacy.getService_start_time() : now;

    patient.setPharmacy_waiting_minutes(
            Math.max(0L, ChronoUnit.MINUTES.between(
                    pharmacy.getQueue_entry_time(), end)));
}
        }
        

        return patients;
    }

    @PutMapping("/{patientId}/status")
    @Transactional
    public ResponseEntity<?> updatePatientStatus(
            @PathVariable Integer patientId,
            @RequestBody Map<String, String> request) {

        Patient patient =
                patientRepository
                        .findById(patientId)
                        .orElse(null);

        if (patient == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Patient not found."
                    ));
        }

        String status =
                request.get("status");

        String username =
                request.get("username");

        String nextStage =
                request.get("nextStage");

        if (status == null ||
                status.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Status is required."
                    ));
        }

        if (username == null ||
                username.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Logged-in user is required."
                    ));
        }

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

        String role =
                user.getRole() == null
                        ? ""
                        : user.getRole()
                                .trim()
                                .toUpperCase();

        String workArea =
                user.getWork_area() == null
                        ? ""
                        : user.getWork_area()
                                .trim()
                                .toUpperCase();

        boolean isAdmin =
                "ADMIN".equals(role);

        boolean isOPStaff =
                "STAFF".equals(role)
                        && "OP".equals(workArea);

        boolean isPharmacyStaff =
                "STAFF".equals(role)
                        && "PHARMACY".equals(workArea);

        boolean isDiagnosticsStaff =
                "STAFF".equals(role)
                        && "DIAGNOSTICS".equals(workArea);

        boolean isEmergencyStaff =
        "STAFF".equals(role)
                && ("EMERGENCY".equals(workArea)
                || "CASUALTY".equals(workArea));

boolean isEmergencyPatient =
        "EMERGENCY".equalsIgnoreCase(
                patient.getDepartment() == null
                        ? ""
                        : patient.getDepartment().trim()
        );
                        
        String newStatus =
                status.trim().toUpperCase();
/*
 * ============================================================
 * EMERGENCY WORKFLOW
 * ============================================================
 */
if (isEmergencyPatient) {

    boolean canHandleEmergency = isAdmin || isEmergencyStaff;

    // Start triage
    if ("IN TRIAGE".equals(newStatus)) {

        if (!canHandleEmergency) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Emergency staff or Admin can start triage."));
        }

        if (!"TRIAGE WAITING".equalsIgnoreCase(patient.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Patient is not waiting for triage."));
        }

        WorkflowEvent triageEvent =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "TRIAGE");

        if (triageEvent == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Triage workflow record not found."));
        }

        if (user.getStaff_id() != null) {
            triageEvent.setStaff_id(user.getStaff_id());
        }

        triageEvent.setService_start_time(LocalDateTime.now());
        workflowEventRepository.save(triageEvent);

        patient.setStatus("IN TRIAGE");
        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // Complete triage and place the patient in the treatment queue
    if ("COMPLETED".equals(newStatus)
            && "IN TRIAGE".equalsIgnoreCase(patient.getStatus())) {

        if (!canHandleEmergency) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Emergency staff or Admin can complete triage."));
        }

        WorkflowEvent triageEvent =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "TRIAGE");

        if (triageEvent == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Triage workflow record not found."));
        }

        LocalDateTime endTime = LocalDateTime.now();
        triageEvent.setService_end_time(endTime);

        if (user.getStaff_id() != null) {
            triageEvent.setStaff_id(user.getStaff_id());
        }

        workflowEventRepository.save(triageEvent);

        WorkflowEvent treatmentEvent =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "EMERGENCY_TREATMENT");

        if (treatmentEvent == null) {
            treatmentEvent = new WorkflowEvent();
            treatmentEvent.setVisit_id(patient.getVisit_id());
            treatmentEvent.setStage("EMERGENCY_TREATMENT");
            treatmentEvent.setQueue_entry_time(endTime);
        }

        workflowEventRepository.save(treatmentEvent);
        patient.setStatus("EMERGENCY TREATMENT WAITING");

        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // Start emergency treatment
    if ("IN EMERGENCY TREATMENT".equals(newStatus)) {

        if (!canHandleEmergency) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Emergency staff or Admin can start emergency treatment."));
        }

        if (!"EMERGENCY TREATMENT WAITING".equalsIgnoreCase(patient.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Patient is not waiting for emergency treatment."));
        }

        WorkflowEvent treatmentEvent =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "EMERGENCY_TREATMENT");

        if (treatmentEvent == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Emergency treatment workflow record not found."));
        }

        if (user.getStaff_id() != null) {
            treatmentEvent.setStaff_id(user.getStaff_id());
        }

        treatmentEvent.setService_start_time(LocalDateTime.now());
        workflowEventRepository.save(treatmentEvent);

        patient.setStatus("IN EMERGENCY TREATMENT");
        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // Complete emergency treatment or record its outcome
    if ("IN EMERGENCY TREATMENT".equalsIgnoreCase(patient.getStatus())
            && ("COMPLETED".equals(newStatus)
            || "ADMITTED".equals(newStatus)
            || "DISCHARGED".equals(newStatus)
            || "REFERRED".equals(newStatus)
            || "TRANSFERRED".equals(newStatus)
            || "DIAGNOSTICS WAITING".equals(newStatus)
            || "PHARMACY WAITING".equals(newStatus))) {

        if (!canHandleEmergency) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Emergency staff or Admin can complete emergency treatment."));
        }

        WorkflowEvent treatmentEvent =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "EMERGENCY_TREATMENT");

        if (treatmentEvent == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Emergency treatment workflow record not found."));
        }

        LocalDateTime endTime = LocalDateTime.now();
        treatmentEvent.setService_end_time(endTime);

        if (user.getStaff_id() != null) {
            treatmentEvent.setStaff_id(user.getStaff_id());
        }

        workflowEventRepository.save(treatmentEvent);

        String outcome = newStatus;
        if ("COMPLETED".equals(newStatus)) {
            outcome = nextStage == null || nextStage.trim().isEmpty()
                    ? "DISCHARGED"
                    : nextStage.trim().toUpperCase();
        }

        if ("DIAGNOSTICS".equals(outcome)
                || "DIAGNOSTICS WAITING".equals(outcome)) {

            WorkflowEvent event =
                    workflowEventRepository.findByVisitIdAndStage(
                            patient.getVisit_id(), "DIAGNOSTICS");

            if (event == null) {
                event = new WorkflowEvent();
                event.setVisit_id(patient.getVisit_id());
                event.setStage("DIAGNOSTICS");
                event.setQueue_entry_time(endTime);
            }

            workflowEventRepository.save(event);
            patient.setStatus("DIAGNOSTICS WAITING");

            return ResponseEntity.ok(patientRepository.save(patient));
        }

        if ("PHARMACY".equals(outcome)
                || "PHARMACY WAITING".equals(outcome)) {

            WorkflowEvent event =
                    workflowEventRepository.findByVisitIdAndStage(
                            patient.getVisit_id(), "PHARMACY");

            if (event == null) {
                event = new WorkflowEvent();
                event.setVisit_id(patient.getVisit_id());
                event.setStage("PHARMACY");
                event.setQueue_entry_time(endTime);
            }

            workflowEventRepository.save(event);
            patient.setStatus("PHARMACY WAITING");

            return ResponseEntity.ok(patientRepository.save(patient));
        }

        if (!"ADMITTED".equals(outcome)
                && !"DISCHARGED".equals(outcome)
                && !"REFERRED".equals(outcome)
                && !"TRANSFERRED".equals(outcome)
                && !"COMPLETED".equals(outcome)
                && !"NONE".equals(outcome)) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Choose Diagnostics, Pharmacy, Admitted, Discharged, Referred, or Transferred."));
        }

        if ("COMPLETED".equals(outcome) || "NONE".equals(outcome)) {
            outcome = "DISCHARGED";
        }

        patient.setStatus(outcome);

        Visit visit = visitRepository.findById(patient.getVisit_id())
                .orElse(null);

        if (visit != null) {
            visit.setStatus(outcome);
            visitRepository.save(visit);
        }

        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // Emergency patients must never enter the normal OP consultation queue.
    // Start Pharmacy service for Emergency patients
    if ("IN PHARMACY".equals(newStatus)) {

        if (!isAdmin && !isPharmacyStaff) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Pharmacy staff or Admin can start Pharmacy service."));
        }

        if (!"PHARMACY WAITING".equalsIgnoreCase(patient.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Patient is not waiting for Pharmacy service."));
        }

        WorkflowEvent event =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "PHARMACY");

        if (event == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Pharmacy workflow record not found."));
        }

        event.setService_start_time(LocalDateTime.now());

        if (user.getStaff_id() != null) {
            event.setStaff_id(user.getStaff_id());
        }

        workflowEventRepository.save(event);
        patient.setStatus("IN PHARMACY");

        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // Start Diagnostics for Emergency patients
    if ("IN DIAGNOSTICS".equals(newStatus)) {

        if (!isAdmin && !isDiagnosticsStaff) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Diagnostics staff or Admin can start Diagnostics."));
        }

        if (!"DIAGNOSTICS WAITING".equalsIgnoreCase(patient.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Patient is not waiting for Diagnostics."));
        }

        WorkflowEvent event =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "DIAGNOSTICS");

        if (event == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Diagnostics workflow record not found."));
        }

        event.setService_start_time(LocalDateTime.now());

        if (user.getStaff_id() != null) {
            event.setStaff_id(user.getStaff_id());
        }

        workflowEventRepository.save(event);
        patient.setStatus("IN DIAGNOSTICS");

        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // Complete Pharmacy service for Emergency patients
    if ("IN PHARMACY".equalsIgnoreCase(patient.getStatus())
            && "COMPLETED".equals(newStatus)) {

        if (!isAdmin && !isPharmacyStaff) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Pharmacy staff or Admin can complete Pharmacy service."));
        }

        WorkflowEvent event =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "PHARMACY");

        if (event == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Pharmacy workflow record not found."));
        }

        event.setService_end_time(LocalDateTime.now());

        if (user.getStaff_id() != null) {
            event.setStaff_id(user.getStaff_id());
        }

        workflowEventRepository.save(event);
        patient.setStatus("DISCHARGED");

        Visit visit = visitRepository.findById(patient.getVisit_id())
                .orElse(null);

        if (visit != null) {
            visit.setStatus("DISCHARGED");
            visitRepository.save(visit);
        }

        return ResponseEntity.ok(patientRepository.save(patient));
    }

    // Complete Diagnostics for Emergency patients
    if ("IN DIAGNOSTICS".equalsIgnoreCase(patient.getStatus())
            && "COMPLETED".equals(newStatus)) {

        if (!isAdmin && !isDiagnosticsStaff) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Only Diagnostics staff or Admin can complete Diagnostics."));
        }

        WorkflowEvent event =
                workflowEventRepository.findByVisitIdAndStage(
                        patient.getVisit_id(), "DIAGNOSTICS");

        if (event == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Diagnostics workflow record not found."));
        }

        LocalDateTime endTime = LocalDateTime.now();
        event.setService_end_time(endTime);

        if (user.getStaff_id() != null) {
            event.setStaff_id(user.getStaff_id());
        }

        workflowEventRepository.save(event);

        String outcome = nextStage == null
                ? ""
                : nextStage.trim().toUpperCase();

        if ("PHARMACY".equals(outcome)
                || "PHARMACY WAITING".equals(outcome)) {

            WorkflowEvent pharmacyEvent =
                    workflowEventRepository.findByVisitIdAndStage(
                            patient.getVisit_id(), "PHARMACY");

            if (pharmacyEvent == null) {
                pharmacyEvent = new WorkflowEvent();
                pharmacyEvent.setVisit_id(patient.getVisit_id());
                pharmacyEvent.setStage("PHARMACY");
            }

            pharmacyEvent.setQueue_entry_time(endTime);
            workflowEventRepository.save(pharmacyEvent);

            patient.setStatus("PHARMACY WAITING");
            return ResponseEntity.ok(patientRepository.save(patient));
        }

        patient.setStatus("DISCHARGED");

        Visit visit = visitRepository.findById(patient.getVisit_id())
                .orElse(null);

        if (visit != null) {
            visit.setStatus("DISCHARGED");
            visitRepository.save(visit);
        }

        return ResponseEntity.ok(patientRepository.save(patient));
    }
    if ("IN CONSULTATION".equals(newStatus)) {
        return ResponseEntity.badRequest()
                .body(Map.of("message",
                        "Emergency patients follow triage and emergency treatment, not normal OP consultation."));
    }
}
        /*
         * ============================================================
         * OP CONSULTATION
         * ============================================================
         */

        if ("IN CONSULTATION".equals(newStatus)
                || "PHARMACY WAITING".equals(newStatus)
                || "DIAGNOSTICS WAITING".equals(newStatus)) {

            /*
             * Start OP consultation
             */
            if ("IN CONSULTATION".equals(newStatus)) {

                if (!isAdmin && !isOPStaff) {

                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(Map.of(
                                    "message",
                                    "Only OP staff can start OP consultation."
                            ));
                }

                if (!"WAITING".equalsIgnoreCase(
                        patient.getStatus())) {

                    return ResponseEntity
                            .badRequest()
                            .body(Map.of(
                                    "message",
                                    "Patient is not currently waiting."
                            ));
                }

                WorkflowEvent opEvent =
        workflowEventRepository
                .findByVisitIdAndStage(
                        patient.getVisit_id(),
                        "OP"
                );

// Create the missing workflow record for an existing visit.
if (opEvent == null) {

    opEvent = new WorkflowEvent();

    opEvent.setVisit_id(patient.getVisit_id());
    opEvent.setStage("OP");

    LocalDateTime queueTime =
            patient.getRegistration_time() != null
                    ? patient.getRegistration_time()
                    : LocalDateTime.now();

    opEvent.setQueue_entry_time(queueTime);
}

                if (user.getStaff_id() != null) {

                    opEvent.setStaff_id(
                            user.getStaff_id()
                    );
                }

                LocalDateTime startTime =
                        LocalDateTime.now();

                patient.setConsultation_start_time(
                        startTime
                );

                opEvent.setService_start_time(
                        startTime
                );

                workflowEventRepository.save(
                        opEvent
                );

                patient.setStatus(
                        "IN CONSULTATION"
                );

                Patient updatedPatient =
                        patientRepository.save(
                                patient
                        );

                return ResponseEntity.ok(
                        updatedPatient
                );
            }

            /*
             * These waiting statuses are created by
             * completion of a previous workflow stage.
             */
            if ("PHARMACY WAITING".equals(newStatus)
                    || "DIAGNOSTICS WAITING".equals(newStatus)) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "This waiting status is created automatically after the previous workflow stage."
                        ));
            }
        }

        /*
         * ============================================================
         * OP / DIAGNOSTICS / PHARMACY COMPLETION
         * ============================================================
         */

        if ("COMPLETED".equals(newStatus)) {

            /*
             * ========================================================
             * OP COMPLETION
             * ========================================================
             */

            if ("IN CONSULTATION".equalsIgnoreCase(
                    patient.getStatus())) {

                if (!isAdmin && !isOPStaff) {

                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(Map.of(
                                    "message",
                                    "Only OP staff can complete OP consultation."
                            ));
                }

                WorkflowEvent opEvent =
                        workflowEventRepository
                                .findByVisitIdAndStage(
                                        patient.getVisit_id(),
                                        "OP"
                                );

                if (opEvent == null) {

                    return ResponseEntity
                            .badRequest()
                            .body(Map.of(
                                    "message",
                                    "OP workflow record not found."
                            ));
                }

                if (user.getStaff_id() != null) {

                    opEvent.setStaff_id(
                            user.getStaff_id()
                    );
                }

                LocalDateTime endTime =
                        LocalDateTime.now();

                patient.setConsultation_end_time(
                        endTime
                );

                opEvent.setService_end_time(
                        endTime
                );

                workflowEventRepository.save(
                        opEvent
                );

                /*
                 * ====================================================
                 * OP NEXT-STAGE SELECTION
                 * ====================================================
                 */

                String selectedNextStage;

                if (nextStage == null ||
                        nextStage.trim().isEmpty()) {

                    /*
                     * Backward compatibility with the
                     * previous frontend.
                     */
                    selectedNextStage = "PHARMACY";

                } else {

                    selectedNextStage =
                            nextStage
                                    .trim()
                                    .toUpperCase();
                }

                if ("COMPLETE".equals(selectedNextStage)
                        || "COMPLETED".equals(selectedNextStage)
                        || "NONE".equals(selectedNextStage)) {

                    selectedNextStage = "NONE";
                }

                if (!"NONE".equals(selectedNextStage)
                        && !"DIAGNOSTICS".equals(selectedNextStage)
                        && !"PHARMACY".equals(selectedNextStage)) {

                    return ResponseEntity
                            .badRequest()
                            .body(Map.of(
                                    "message",
                                    "Invalid next stage. Choose NONE, DIAGNOSTICS, or PHARMACY."
                            ));
                }

                /*
                 * Complete visit directly.
                 */
                if ("NONE".equals(selectedNextStage)) {

                    patient.setStatus(
                            "COMPLETED"
                    );

                    Patient updatedPatient =
                            patientRepository.save(
                                    patient
                            );

                    return ResponseEntity.ok(
                            updatedPatient
                    );
                }

                /*
                 * Send to Diagnostics.
                 */
                if ("DIAGNOSTICS".equals(
                        selectedNextStage)) {

                    WorkflowEvent diagnosticsEvent =
                            workflowEventRepository
                                    .findByVisitIdAndStage(
                                            patient.getVisit_id(),
                                            "DIAGNOSTICS"
                                    );

                    if (diagnosticsEvent == null) {

                        diagnosticsEvent =
                                new WorkflowEvent();

                        diagnosticsEvent.setVisit_id(
                                patient.getVisit_id()
                        );

                        diagnosticsEvent.setStage(
                                "DIAGNOSTICS"
                        );

                        diagnosticsEvent.setQueue_entry_time(
                                endTime
                        );

                        workflowEventRepository.save(
                                diagnosticsEvent
                        );
                    }

                    patient.setStatus(
                            "DIAGNOSTICS WAITING"
                    );

                    Patient updatedPatient =
                            patientRepository.save(
                                    patient
                            );

                    return ResponseEntity.ok(
                            updatedPatient
                    );
                }

                /*
                 * Send to Pharmacy.
                 */
                if ("PHARMACY".equals(
                        selectedNextStage)) {

                    WorkflowEvent pharmacyEvent =
                            workflowEventRepository
                                    .findByVisitIdAndStage(
                                            patient.getVisit_id(),
                                            "PHARMACY"
                                    );

                    if (pharmacyEvent == null) {

                        pharmacyEvent =
                                new WorkflowEvent();

                        pharmacyEvent.setVisit_id(
                                patient.getVisit_id()
                        );

                        pharmacyEvent.setStage(
                                "PHARMACY"
                        );

                        pharmacyEvent.setQueue_entry_time(
                                endTime
                        );
                        workflowEventRepository.save(
                                pharmacyEvent
                        );
                    }

                    patient.setStatus(
                            "PHARMACY WAITING"
                    );

                    Patient updatedPatient =
                            patientRepository.save(
                                    patient
                            );

                    return ResponseEntity.ok(
                            updatedPatient
                    );
                }
            }

            /*
             * ========================================================
             * DIAGNOSTICS COMPLETION
             * ========================================================
             *
             * This will be used by the Diagnostics staff screen.
             *
             * After Diagnostics:
             *
             * NONE     -> COMPLETED
             * PHARMACY -> PHARMACY WAITING
             */

            if ("IN DIAGNOSTICS".equalsIgnoreCase(
                    patient.getStatus())) {

                if (!isAdmin && !isDiagnosticsStaff) {

                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(Map.of(
                                    "message",
                                    "Only Diagnostics staff can complete Diagnostics service."
                            ));
                }

                WorkflowEvent diagnosticsEvent =
                        workflowEventRepository
                                .findByVisitIdAndStage(
                                        patient.getVisit_id(),
                                        "DIAGNOSTICS"
                                );

                if (diagnosticsEvent == null) {

                    return ResponseEntity
                            .badRequest()
                            .body(Map.of(
                                    "message",
                                    "Diagnostics workflow record not found."
                            ));
                }

                if (user.getStaff_id() != null) {

                    diagnosticsEvent.setStaff_id(
                            user.getStaff_id()
                    );
                }

                LocalDateTime endTime =
                        LocalDateTime.now();

                diagnosticsEvent.setService_end_time(
                        endTime
                );

                workflowEventRepository.save(
                        diagnosticsEvent
                );

                String selectedNextStage;

                if (nextStage == null ||
                        nextStage.trim().isEmpty()) {

                    selectedNextStage = "NONE";

                } else {

                    selectedNextStage =
                            nextStage
                                    .trim()
                                    .toUpperCase();
                }

                if ("COMPLETE".equals(selectedNextStage)
                        || "COMPLETED".equals(selectedNextStage)
                        || "NONE".equals(selectedNextStage)) {

                    selectedNextStage = "NONE";
                }

                if (!"NONE".equals(selectedNextStage)
                        && !"PHARMACY".equals(selectedNextStage)) {

                    return ResponseEntity
                            .badRequest()
                            .body(Map.of(
                                    "message",
                                    "Invalid next stage after Diagnostics. Choose NONE or PHARMACY."
                            ));
                }

                /*
                 * Diagnostics finished and
                 * no further service is required.
                 */
                if ("NONE".equals(selectedNextStage)) {

                    patient.setStatus(
                            "COMPLETED"
                    );

                    Patient updatedPatient =
                            patientRepository.save(
                                    patient
                            );

                    return ResponseEntity.ok(
                            updatedPatient
                    );
                }

                /*
                 * Diagnostics finished and
                 * patient goes to Pharmacy.
                 */
                if ("PHARMACY".equals(
                        selectedNextStage)) {

                    WorkflowEvent pharmacyEvent =
                            workflowEventRepository
                                    .findByVisitIdAndStage(
                                            patient.getVisit_id(),
                                            "PHARMACY"
                                    );

                    if (pharmacyEvent == null) {

                        pharmacyEvent =
                                new WorkflowEvent();

                        pharmacyEvent.setVisit_id(
                                patient.getVisit_id()
                        );

                        pharmacyEvent.setStage(
                                "PHARMACY"
                        );

                        pharmacyEvent.setQueue_entry_time(
                                endTime
                        );

                        workflowEventRepository.save(
                                pharmacyEvent
                        );
                    }

                    patient.setStatus(
                            "PHARMACY WAITING"
                    );

                    Patient updatedPatient =
                            patientRepository.save(
                                    patient
                            );

                    return ResponseEntity.ok(
                            updatedPatient
                    );
                }
            }

            /*
             * ========================================================
             * PHARMACY COMPLETION
             * ========================================================
             */

            if ("IN PHARMACY".equalsIgnoreCase(
                    patient.getStatus())) {

                if (!isAdmin && !isPharmacyStaff) {

                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(Map.of(
                                    "message",
                                    "Only Pharmacy staff can complete Pharmacy service."
                            ));
                }

                WorkflowEvent pharmacyEvent =
                        workflowEventRepository
                                .findByVisitIdAndStage(
                                        patient.getVisit_id(),
                                        "PHARMACY"
                                );

                if (pharmacyEvent == null) {

                    return ResponseEntity
                            .badRequest()
                            .body(Map.of(
                                    "message",
                                    "Pharmacy workflow record not found."
                            ));
                }

                if (user.getStaff_id() != null) {

                    pharmacyEvent.setStaff_id(
                            user.getStaff_id()
                    );
                }

                LocalDateTime endTime =
                        LocalDateTime.now();

                pharmacyEvent.setService_end_time(
                        endTime
                );

                workflowEventRepository.save(
                        pharmacyEvent
                );

                patient.setStatus(
                        "COMPLETED"
                );

                Patient updatedPatient =
                        patientRepository.save(
                                patient
                        );

                return ResponseEntity.ok(
                        updatedPatient
                );
            }

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Patient is not currently in OP consultation, Diagnostics service, or Pharmacy service."
                    ));
        }

        /*
         * ============================================================
         * PHARMACY START
         * ============================================================
         */

        if ("IN PHARMACY".equals(newStatus)) {

            if (!isAdmin && !isPharmacyStaff) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "message",
                                "Only Pharmacy staff can start Pharmacy service."
                        ));
            }

            if (!"PHARMACY WAITING".equalsIgnoreCase(
                    patient.getStatus())) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "Patient is not currently waiting for Pharmacy."
                        ));
            }

            WorkflowEvent pharmacyEvent =
                    workflowEventRepository
                            .findByVisitIdAndStage(
                                    patient.getVisit_id(),
                                    "PHARMACY"
                            );

            if (pharmacyEvent == null) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "Pharmacy workflow record not found."
                        ));
            }

            if (user.getStaff_id() != null) {

                pharmacyEvent.setStaff_id(
                        user.getStaff_id()
                );
            }

            LocalDateTime startTime =
                    LocalDateTime.now();

            pharmacyEvent.setService_start_time(
                    startTime
            );

            workflowEventRepository.save(
                    pharmacyEvent
            );

            patient.setStatus(
                    "IN PHARMACY"
            );

            Patient updatedPatient =
                    patientRepository.save(
                            patient
                    );

            return ResponseEntity.ok(
                    updatedPatient
            );
        }

        /*
         * ============================================================
         * DIAGNOSTICS START
         * ============================================================
         */

        if ("IN DIAGNOSTICS".equals(newStatus)) {

            if (!isAdmin && !isDiagnosticsStaff) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "message",
                                "Only Diagnostics staff can start Diagnostics service."
                        ));
            }

            if (!"DIAGNOSTICS WAITING".equalsIgnoreCase(
                    patient.getStatus())) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "Patient is not currently waiting for Diagnostics."
                        ));
            }

            WorkflowEvent diagnosticsEvent =
                    workflowEventRepository
                            .findByVisitIdAndStage(
                                    patient.getVisit_id(),
                                    "DIAGNOSTICS"
                            );

            if (diagnosticsEvent == null) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "message",
                                "Diagnostics workflow record not found."
                        ));
            }

            if (user.getStaff_id() != null) {

                diagnosticsEvent.setStaff_id(
                        user.getStaff_id()
                );
            }

            LocalDateTime startTime =
                    LocalDateTime.now();

            diagnosticsEvent.setService_start_time(
                    startTime
            );

            workflowEventRepository.save(
                    diagnosticsEvent
            );

            patient.setStatus(
                    "IN DIAGNOSTICS"
            );

            Patient updatedPatient =
                    patientRepository.save(
                            patient
                    );

            return ResponseEntity.ok(
                    updatedPatient
            );
        }

        /*
         * ============================================================
         * INVALID STATUS
         * ============================================================
         */

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        "Invalid patient status."
                ));
    }
}