package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.WorkflowEvent;
import com.hospital.smart_hospital.repository.WorkflowEventRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/workflow-events")
public class WorkflowEventController {

    private final WorkflowEventRepository workflowEventRepository;

    public WorkflowEventController(
            WorkflowEventRepository workflowEventRepository) {
        this.workflowEventRepository = workflowEventRepository;
    }

    @GetMapping
    public List<WorkflowEvent> getWorkflowEvents() {
        return workflowEventRepository.findAll();
    }
}