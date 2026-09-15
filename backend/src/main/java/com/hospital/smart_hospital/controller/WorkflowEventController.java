package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.WorkflowEvent;
import com.hospital.smart_hospital.repository.WorkflowEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/workflow-events")
public class WorkflowEventController {

    private final WorkflowEventRepository workflowEventRepository;

    public WorkflowEventController(
            WorkflowEventRepository workflowEventRepository) {
        this.workflowEventRepository = workflowEventRepository;
    }

    @GetMapping
    public List<WorkflowEvent> getWorkflowEvents() {

        return workflowEventRepository.findFirstWorkflowEvents(
                PageRequest.of(0, 50)
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> getWorkflowSummary() {

        Map<String, Object> summary = new HashMap<>();

        summary.put(
                "total",
                workflowEventRepository.countTotalWorkflowEvents()
        );

        summary.put(
                "registration",
                workflowEventRepository.countWorkflowEventsByStage("REGISTRATION")
        );

        summary.put(
                "op",
                workflowEventRepository.countWorkflowEventsByStage("OP")
        );

        summary.put(
                "pharmacy",
                workflowEventRepository.countWorkflowEventsByStage("PHARMACY")
        );

        summary.put(
                "events",
                workflowEventRepository.findFirstWorkflowEvents(
                        PageRequest.of(0, 50)
                )
        );

        return summary;
    }
}