package com.hospital.smart_hospital.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_events")
public class WorkflowEvent {

    @Id
    private Integer event_id;

    private Integer visit_id;
    private String stage;
    private LocalDateTime queue_entry_time;
    private LocalDateTime service_start_time;
    private LocalDateTime service_end_time;
    private Integer staff_id;

    public Integer getEvent_id() {
        return event_id;
    }

    public void setEvent_id(Integer event_id) {
        this.event_id = event_id;
    }

    public Integer getVisit_id() {
        return visit_id;
    }

    public void setVisit_id(Integer visit_id) {
        this.visit_id = visit_id;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public LocalDateTime getQueue_entry_time() {
        return queue_entry_time;
    }

    public void setQueue_entry_time(LocalDateTime queue_entry_time) {
        this.queue_entry_time = queue_entry_time;
    }

    public LocalDateTime getService_start_time() {
        return service_start_time;
    }

    public void setService_start_time(LocalDateTime service_start_time) {
        this.service_start_time = service_start_time;
    }

    public LocalDateTime getService_end_time() {
        return service_end_time;
    }

    public void setService_end_time(LocalDateTime service_end_time) {
        this.service_end_time = service_end_time;
    }

    public Integer getStaff_id() {
        return staff_id;
    }

    public void setStaff_id(Integer staff_id) {
        this.staff_id = staff_id;
    }
}