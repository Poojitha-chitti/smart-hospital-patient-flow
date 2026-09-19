package com.hospital.smart_hospital.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer patient_id;

    private Integer visit_id;
    private String patient_name;
    private Integer age;
    private String gender;
    private String phone;
    private String department;
    private LocalDateTime registration_time;
    private LocalDateTime consultation_start_time;
    private LocalDateTime consultation_end_time;
    private String status;

    public Integer getPatient_id() { return patient_id; }
    public void setPatient_id(Integer patient_id) { this.patient_id = patient_id; }

    public Integer getVisit_id() { return visit_id; }
    public void setVisit_id(Integer visit_id) { this.visit_id = visit_id; }

    public String getPatient_name() { return patient_name; }
    public void setPatient_name(String patient_name) { this.patient_name = patient_name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
public LocalDateTime getRegistration_time() {
    return registration_time;
}

public void setRegistration_time(LocalDateTime registration_time) {
    this.registration_time = registration_time;
}

public LocalDateTime getConsultation_start_time() {
    return consultation_start_time;
}

public void setConsultation_start_time(LocalDateTime consultation_start_time) {
    this.consultation_start_time = consultation_start_time;
}

public LocalDateTime getConsultation_end_time() {
    return consultation_end_time;
}

public void setConsultation_end_time(LocalDateTime consultation_end_time) {
    this.consultation_end_time = consultation_end_time;
}

public String getStatus() {
    return status;
}

public void setStatus(String status) {
    this.status = status;
}
   }
