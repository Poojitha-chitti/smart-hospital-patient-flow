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
        @RequestParam(defaultValue = "OP") String stage,
        @RequestParam(defaultValue = "NORMAL") String patientType) {

    String department;

    switch (stage.toUpperCase()) {
        case "REGISTRATION":
            department = "Registration";
            break;
        case "DIAGNOSTICS":
            department = "Diagnostics";
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

    // Get all staff for this department
    java.util.List<Staff> staffMembers =
            staffRepository.findByDepartmentIgnoreCase(department);

    int staffAvailable = (int) staffMembers.stream()
            .filter(s -> Boolean.TRUE.equals(s.getAvailable()))
            .count();

    int staffCapacity = staffMembers.stream()
            .mapToInt(s -> s.getCapacity() == null ? 0 : s.getCapacity())
            .sum();

    // Get all resources for this department
    java.util.List<Resource> resources =
            resourceRepository.findByDepartmentIgnoreCase(department);

    int resourceAvailable = resources.stream()
            .mapToInt(r -> r.getAvailable() == null ? 0 : r.getAvailable())
            .sum();

    int resourceCapacity = resources.stream()
            .mapToInt(r -> r.getCapacity() == null ? 0 : r.getCapacity())
            .sum();

    MLPredictionRequest request = new MLPredictionRequest();

    request.setStage(stage.toUpperCase());
    request.setPatient_type(patientType.toUpperCase());
    request.setStaff_available(staffAvailable);
    request.setStaff_capacity(staffCapacity);
    request.setResource_available(resourceAvailable);
    request.setResource_capacity(resourceCapacity);

    return request;
}
    @GetMapping("/predict-current")
    public MLPredictionResponse predictCurrent(
            @RequestParam(defaultValue = "OP") String stage,
            @RequestParam(defaultValue = "NORMAL") String patientType) {

        MLPredictionRequest request =
                getCurrentInput(stage, patientType);

        return restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(MLPredictionResponse.class);
    }
}