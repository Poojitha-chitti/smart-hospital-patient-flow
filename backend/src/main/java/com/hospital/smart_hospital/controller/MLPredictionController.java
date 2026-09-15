package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.MLPredictionRequest;
import com.hospital.smart_hospital.model.MLPredictionResponse;
import com.hospital.smart_hospital.model.Resource;
import com.hospital.smart_hospital.model.Staff;
import com.hospital.smart_hospital.repository.ResourceRepository;
import com.hospital.smart_hospital.repository.StaffRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/ml")
public class MLPredictionController {

    private final RestClient restClient;
    private final StaffRepository staffRepository;
    private final ResourceRepository resourceRepository;

    public MLPredictionController(
            StaffRepository staffRepository,
            ResourceRepository resourceRepository) {

        this.staffRepository = staffRepository;
        this.resourceRepository = resourceRepository;

        this.restClient = RestClient.builder()
        .baseUrl(System.getenv().getOrDefault("ML_SERVICE_URL", "http://127.0.0.1:5000"))
        .build();
    }

    @PostMapping("/predict")
    public MLPredictionResponse predict(
            @RequestBody MLPredictionRequest request) {

        return restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(MLPredictionResponse.class);
    }

    @GetMapping("/current-input")
    public MLPredictionRequest getCurrentInput() {

        String department = "OP";
        Staff staff = staffRepository
        .findAvailableStaff(department)
        .stream()
        .findFirst()
        .orElseThrow(() ->
                new RuntimeException("No available OP staff found"));

        Resource resource = resourceRepository
                .findFirstByDepartment(department)
                .orElseThrow(() ->
                        new RuntimeException("No OP resource found"));

        MLPredictionRequest request = new MLPredictionRequest();

        request.setStage("OP");
        request.setPatient_type("OP");
        request.setStaff_available(1);
        request.setStaff_capacity(staff.getCapacity());
        request.setResource_capacity(resource.getCapacity());
        request.setResource_available(resource.getAvailable());

        return request;
    }
    @GetMapping("/predict-current")
public MLPredictionResponse predictCurrent() {

    MLPredictionRequest request = getCurrentInput();

    return restClient.post()
            .uri("/predict")
            .body(request)
            .retrieve()
            .body(MLPredictionResponse.class);
}
}