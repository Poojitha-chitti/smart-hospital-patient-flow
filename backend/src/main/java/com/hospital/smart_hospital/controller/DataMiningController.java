package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.repository.WorkflowEventRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class DataMiningController {

    private final WorkflowEventRepository repository;

    public DataMiningController(WorkflowEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/data-mining/patterns")
    public Map<String, Object> getPatterns() {

        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> stagePatterns = new ArrayList<>();

        for (Object[] row : repository.findStageWaitingPatterns()) {

            Map<String, Object> item = new HashMap<>();

            item.put("stage", row[0]);
            item.put("averageWaitingTime", row[1]);

            stagePatterns.add(item);
        }

        List<Map<String, Object>> arrivalPatterns = new ArrayList<>();

        for (Object[] row : repository.findArrivalHourPatterns()) {

            Map<String, Object> item = new HashMap<>();

            item.put("hour", row[0]);
            item.put("patientCount", row[1]);

            arrivalPatterns.add(item);
        }

        result.put("stagePatterns", stagePatterns);
        result.put("arrivalPatterns", arrivalPatterns);
        result.put(
                "highWaitingEvents",
                repository.countHighWaitingEvents()
        );

        return result;
    }
}