package com.hospital.smart_hospital.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/research")
public class ResearchBottleneckController {

    private final JdbcTemplate researchJdbcTemplate;

    public ResearchBottleneckController(
            @Qualifier("researchJdbcTemplate")
            JdbcTemplate researchJdbcTemplate) {
        this.researchJdbcTemplate = researchJdbcTemplate;
    }

    @GetMapping("/bottleneck-analysis")
    public List<Map<String, Object>> getHistoricalBottleneckAnalysis() {

        String sql = """
            SELECT
                stage,
                COUNT(*) AS total_cases,
                ROUND(AVG(
                    TIMESTAMPDIFF(
                        SECOND,
                        queue_entry_time,
                        service_start_time
                    ) / 60.0
                ), 2) AS average_waiting_minutes,
                ROUND(AVG(
                    TIMESTAMPDIFF(
                        SECOND,
                        service_start_time,
                        service_end_time
                    ) / 60.0
                ), 2) AS average_service_minutes,
                SUM(
                    CASE
                        WHEN TIMESTAMPDIFF(
                            SECOND,
                            queue_entry_time,
                            service_start_time
                        ) >= 1200
                        THEN 1
                        ELSE 0
                    END
                ) AS high_wait_cases
            FROM workflow_events
            WHERE queue_entry_time IS NOT NULL
              AND service_start_time IS NOT NULL
              AND service_end_time IS NOT NULL
            GROUP BY stage
            ORDER BY average_waiting_minutes DESC
            """;

        return researchJdbcTemplate.queryForList(sql);
    }
}