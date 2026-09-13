package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.BottleneckResult;
import com.hospital.smart_hospital.repository.BottleneckProjection;
import com.hospital.smart_hospital.repository.WorkflowEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class BottleneckController {
    
    private final WorkflowEventRepository workflowEventRepository;

    public BottleneckController(WorkflowEventRepository workflowEventRepository) {
        this.workflowEventRepository = workflowEventRepository;
    }

    @GetMapping("/api/bottleneck-analysis")
    public List<BottleneckResult> getBottleneckAnalysis() {

        List<BottleneckProjection> analysis =
                workflowEventRepository.findBottleneckAnalysis();

        List<BottleneckResult> results = new ArrayList<>();

        for (BottleneckProjection item : analysis) {

            String severity;

            if (item.getAverageWaitingTime() >= 20) {
                severity = "HIGH";
            } else if (item.getAverageWaitingTime() >= 10) {
                severity = "MEDIUM";
            } else {
                severity = "LOW";
            }

            results.add(
                new BottleneckResult(
                    item.getStage(),
                    item.getAverageWaitingTime(),
                    item.getAverageServiceTime(),
                    severity
                )
            );
        }

        return results;
    }
}