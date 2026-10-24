package com.hospital.smart_hospital.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer patient_id;

    private Integer visit_id;
    @Transient
private String display_visit_id;
    private String patient_name;

    private Integer age;

    private String gender;

    private String phone;

    private String department;

    private LocalDateTime registration_time;

    private LocalDateTime consultation_start_time;

    private LocalDateTime consultation_end_time;

    private String status;

    /*
     * Pharmacy timing is already stored in
     * workflow_events.
     *
     * These fields are only used to send
     * those values to the frontend.
     *
     * @Transient means they are NOT stored
     * as columns in the patients table.
     */

    @Transient
    private LocalDateTime pharmacy_start_time;

    @Transient
    private LocalDateTime pharmacy_end_time;
    
    @Transient
private LocalDateTime diagnostics_start_time;

@Transient
private LocalDateTime diagnostics_end_time;

@Transient
private Long waiting_time_minutes;
@Transient
private LocalDateTime triage_start_time;
@Transient
private LocalDateTime emergency_treatment_start_time;

@Transient
private LocalDateTime emergency_treatment_end_time;
@Transient
private Long triage_waiting_minutes;

@Transient
private Long emergency_treatment_waiting_minutes;

@Transient
private Long diagnostics_waiting_minutes;

@Transient
private Long pharmacy_waiting_minutes;
@Transient
private LocalDateTime triage_queue_entry_time;

@Transient
private LocalDateTime emergency_treatment_queue_entry_time;

@Transient
private LocalDateTime diagnostics_queue_entry_time;

@Transient
private LocalDateTime pharmacy_queue_entry_time;
public LocalDateTime getTriage_start_time() {
    return triage_start_time;
}

public void setTriage_start_time(LocalDateTime triage_start_time) {
    this.triage_start_time = triage_start_time;
}
public LocalDateTime getEmergency_treatment_start_time() {
    return emergency_treatment_start_time;
}

public void setEmergency_treatment_start_time(
        LocalDateTime emergency_treatment_start_time) {
    this.emergency_treatment_start_time = emergency_treatment_start_time;
}

public LocalDateTime getEmergency_treatment_end_time() {
    return emergency_treatment_end_time;
}

public void setEmergency_treatment_end_time(
        LocalDateTime emergency_treatment_end_time) {
    this.emergency_treatment_end_time = emergency_treatment_end_time;
}

public Long getWaiting_time_minutes() {
    return waiting_time_minutes;
}
public Long getTriage_waiting_minutes() {
    return triage_waiting_minutes;
}

public void setTriage_waiting_minutes(Long value) {
    this.triage_waiting_minutes = value;
}

public Long getEmergency_treatment_waiting_minutes() {
    return emergency_treatment_waiting_minutes;
}

public void setEmergency_treatment_waiting_minutes(Long value) {
    this.emergency_treatment_waiting_minutes = value;
}

public Long getDiagnostics_waiting_minutes() {
    return diagnostics_waiting_minutes;
}

public void setDiagnostics_waiting_minutes(Long value) {
    this.diagnostics_waiting_minutes = value;
}

public Long getPharmacy_waiting_minutes() {
    return pharmacy_waiting_minutes;
}

public void setPharmacy_waiting_minutes(Long value) {
    this.pharmacy_waiting_minutes = value;
}
public LocalDateTime getTriage_queue_entry_time() {
    return triage_queue_entry_time;
}

public void setTriage_queue_entry_time(LocalDateTime value) {
    this.triage_queue_entry_time = value;
}

public LocalDateTime getEmergency_treatment_queue_entry_time() {
    return emergency_treatment_queue_entry_time;
}

public void setEmergency_treatment_queue_entry_time(LocalDateTime value) {
    this.emergency_treatment_queue_entry_time = value;
}

public LocalDateTime getDiagnostics_queue_entry_time() {
    return diagnostics_queue_entry_time;
}

public void setDiagnostics_queue_entry_time(LocalDateTime value) {
    this.diagnostics_queue_entry_time = value;
}

public LocalDateTime getPharmacy_queue_entry_time() {
    return pharmacy_queue_entry_time;
}

public void setPharmacy_queue_entry_time(LocalDateTime value) {
    this.pharmacy_queue_entry_time = value;
}

public void setWaiting_time_minutes(Long waiting_time_minutes) {
    this.waiting_time_minutes = waiting_time_minutes;
}
    public Integer getPatient_id() {
        return patient_id;
    }

    public void setPatient_id(Integer patient_id) {
        this.patient_id = patient_id;
    }


    public Integer getVisit_id() {
        return visit_id;
    }

    public void setVisit_id(Integer visit_id) {
        this.visit_id = visit_id;
    }public String getDisplay_visit_id() {
    return display_visit_id;
}

public void setDisplay_visit_id(String display_visit_id) {
    this.display_visit_id = display_visit_id;
}


    public String getPatient_name() {
        return patient_name;
    }

    public void setPatient_name(String patient_name) {
        this.patient_name = patient_name;
    }


    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }


    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }


    public LocalDateTime getRegistration_time() {
        return registration_time;
    }

    public void setRegistration_time(
            LocalDateTime registration_time) {

        this.registration_time = registration_time;
    }


    public LocalDateTime getConsultation_start_time() {
        return consultation_start_time;
    }

    public void setConsultation_start_time(
            LocalDateTime consultation_start_time) {

        this.consultation_start_time =
                consultation_start_time;
    }


    public LocalDateTime getConsultation_end_time() {
        return consultation_end_time;
    }

    public void setConsultation_end_time(
            LocalDateTime consultation_end_time) {

        this.consultation_end_time =
                consultation_end_time;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public LocalDateTime getPharmacy_start_time() {
        return pharmacy_start_time;
    }

    public void setPharmacy_start_time(
            LocalDateTime pharmacy_start_time) {

        this.pharmacy_start_time =
                pharmacy_start_time;
    }


    public LocalDateTime getPharmacy_end_time() {
        return pharmacy_end_time;
    }

    public void setPharmacy_end_time(
            LocalDateTime pharmacy_end_time) {

        this.pharmacy_end_time =
                pharmacy_end_time;
    }
    public LocalDateTime getDiagnostics_start_time() {
    return diagnostics_start_time;
}

public void setDiagnostics_start_time(LocalDateTime diagnostics_start_time) {
    this.diagnostics_start_time = diagnostics_start_time;
}

public LocalDateTime getDiagnostics_end_time() {
    return diagnostics_end_time;
}

public void setDiagnostics_end_time(LocalDateTime diagnostics_end_time) {
    this.diagnostics_end_time = diagnostics_end_time;
}
}