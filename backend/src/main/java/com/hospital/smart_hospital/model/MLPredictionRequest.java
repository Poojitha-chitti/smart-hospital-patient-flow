package com.hospital.smart_hospital.model;

public class MLPredictionRequest {

    private String stage;
    private String patient_type;
    private int staff_available;
    private int staff_capacity;
    private int resource_capacity;
    private int resource_available;

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getPatient_type() {
        return patient_type;
    }

    public void setPatient_type(String patient_type) {
        this.patient_type = patient_type;
    }

    public int getStaff_available() {
        return staff_available;
    }

    public void setStaff_available(int staff_available) {
        this.staff_available = staff_available;
    }

    public int getStaff_capacity() {
        return staff_capacity;
    }

    public void setStaff_capacity(int staff_capacity) {
        this.staff_capacity = staff_capacity;
    }

    public int getResource_capacity() {
        return resource_capacity;
    }

    public void setResource_capacity(int resource_capacity) {
        this.resource_capacity = resource_capacity;
    }

    public int getResource_available() {
        return resource_available;
    }

    public void setResource_available(int resource_available) {
        this.resource_available = resource_available;
    }
}