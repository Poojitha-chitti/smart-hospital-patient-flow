package com.hospital.smart_hospital.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ResearchDataController {

private final JdbcTemplate researchJdbcTemplate;

public ResearchDataController(
        @Qualifier("researchJdbcTemplate") JdbcTemplate researchJdbcTemplate) {
    this.researchJdbcTemplate = researchJdbcTemplate;
}

@GetMapping("/api/research/summary")
public Map<String, Object> getResearchSummary() {

    Map<String, Object> result = new LinkedHashMap<>();

    Integer totalVisits = researchJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM visits", Integer.class);

    Integer totalEvents = researchJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workflow_events", Integer.class);

    Integer registration = countStage("REGISTRATION");
    Integer op = countStage("OP");
    Integer pharmacy = countStage("PHARMACY");

    List<Map<String, Object>> events = researchJdbcTemplate.queryForList("""
            SELECT event_id, visit_id, stage, queue_entry_time,
                   service_start_time, service_end_time, staff_id
            FROM workflow_events
            ORDER BY event_id DESC
            LIMIT 50
            """);

    result.put("totalVisits", totalVisits);
    result.put("totalWorkflowEvents", totalEvents);
    result.put("registration", registration);
    result.put("op", op);
    result.put("pharmacy", pharmacy);
    result.put("events", events);

    return result;
}

private Integer countStage(String stage) {
    return researchJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workflow_events WHERE stage = ?",
            Integer.class,
            stage);
}

}