package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.MLPredictionRequest;
import com.hospital.smart_hospital.model.MLPredictionResponse;
import com.hospital.smart_hospital.model.Resource;
import com.hospital.smart_hospital.model.Staff;
import com.hospital.smart_hospital.repository.ResourceRepository;
import com.hospital.smart_hospital.repository.StaffRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/ml")
public class MLPredictionController {

    private final RestClient restClient;
    private final StaffRepository staffRepository;
    private final ResourceRepository resourceRepository;

    public MLPredictionController(
            StaffRepository staffRepository,
            ResourceRepository resourceRepository,
            @Value("${ml.service.url}") String mlServiceUrl) {

        this.staffRepository = staffRepository;
        this.resourceRepository = resourceRepository;

        this.restClient = RestClient.builder()
                .baseUrl(mlServiceUrl)
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
public MLPredictionRequest getCurrentInput(
        @RequestParam(defaultValue = "OP") String stage) {

    String department;

    switch (stage.toUpperCase()) {
        case "REGISTRATION":
            department = "Registration";
            break;

        case "PHARMACY":
            department = "Pharmacy";
            break;

        case "OP":
        default:
            department = "OP";
            stage = "OP";
            break;
    }

    Staff staff = staffRepository
            .findAvailableStaff(department)
            .stream()
            .findFirst()
            .orElseThrow(() ->
                    new RuntimeException(
                            "No available staff found for " + department));

    Resource resource = resourceRepository
            .findFirstByDepartment(department)
            .orElseThrow(() ->
                    new RuntimeException(
                            "No resource found for " + department));

    MLPredictionRequest request = new MLPredictionRequest();

    request.setStage(stage.toUpperCase());
    request.setPatient_type(stage.toUpperCase());

    request.setStaff_available(1);
    request.setStaff_capacity(staff.getCapacity());

    request.setResource_capacity(resource.getCapacity());
    request.setResource_available(resource.getAvailable());

    return request;
}
    @GetMapping("/predict-current")
public MLPredictionResponse predictCurrent(
        @RequestParam(defaultValue = "OP") String stage) {

    MLPredictionRequest request =
            getCurrentInput(stage);

    return restClient.post()
            .uri("/predict")
            .body(request)
            .retrieve()
            .body(MLPredictionResponse.class);
}
}