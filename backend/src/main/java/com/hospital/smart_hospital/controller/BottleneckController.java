package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.service.HospitalEmailService;
import com.hospital.smart_hospital.model.BottleneckResult;
import com.hospital.smart_hospital.model.WorkflowEvent;
import com.hospital.smart_hospital.repository.BottleneckProjection;
import com.hospital.smart_hospital.repository.WorkflowEventRepository;
import com.hospital.smart_hospital.service.EabdaDiagnosisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class BottleneckController {

    private final WorkflowEventRepository workflowEventRepository;
    private final EabdaDiagnosisService eabdaDiagnosisService;
    private final HospitalEmailService hospitalEmailService;

    public BottleneckController(
        WorkflowEventRepository workflowEventRepository,
        EabdaDiagnosisService eabdaDiagnosisService,
        HospitalEmailService hospitalEmailService) {

    this.workflowEventRepository = workflowEventRepository;
    this.eabdaDiagnosisService = eabdaDiagnosisService;
    this.hospitalEmailService = hospitalEmailService;
}
    @GetMapping("/api/bottleneck-analysis")
    public List<BottleneckResult> getBottleneckAnalysis() {

        List<BottleneckProjection> analysis =
                workflowEventRepository.findBottleneckAnalysis();

        List<WorkflowEvent> events = workflowEventRepository.findAll();
        Map<String, List<WorkflowEvent>> eventsByStage = events.stream()
                .filter(e -> e.getStage() != null)
                .collect(Collectors.groupingBy(WorkflowEvent::getStage));

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

            EabdaDiagnosisService.Diagnosis diagnosis =
                    eabdaDiagnosisService.diagnose(item.getStage(),
                            eventsByStage.getOrDefault(item.getStage(), List.of()));
            if ("PERSISTENT".equalsIgnoreCase(diagnosis.state())) {

    hospitalEmailService.sendBottleneckAlert(
            item.getStage(),
            item.getAverageWaitingTime(),
            diagnosis.baseline(),
            diagnosis.pressure(),
            diagnosis.evidence()
    );

} else {

    hospitalEmailService.resetAlert(item.getStage());
}
            results.add(new BottleneckResult(
                    item.getStage(),
                    item.getAverageWaitingTime(),
                    item.getAverageServiceTime(),
                    severity,
                    diagnosis.state(),
                    diagnosis.baseline(),
                    diagnosis.pressure(),
                    diagnosis.trend(),
                    diagnosis.persistence(),
                    diagnosis.evidence()
            ));
        }

        return results;
    }
    
}
