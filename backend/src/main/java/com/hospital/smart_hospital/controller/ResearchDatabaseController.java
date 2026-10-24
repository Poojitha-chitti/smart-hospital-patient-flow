package com.hospital.smart_hospital.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/research")
public class ResearchDatabaseController {

    private final JdbcTemplate researchJdbcTemplate;

    public ResearchDatabaseController(
            @Qualifier("researchJdbcTemplate")
            JdbcTemplate researchJdbcTemplate) {
        this.researchJdbcTemplate = researchJdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> checkResearchDatabase() {
        Integer totalEvents = researchJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workflow_events",
                Integer.class
        );

        Integer totalVisits = researchJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visits",
                Integer.class
        );

        return Map.of(
                "researchDatabase", "CONNECTED",
                "totalEvents", totalEvents,
                "totalVisits", totalVisits
        );
    }
}