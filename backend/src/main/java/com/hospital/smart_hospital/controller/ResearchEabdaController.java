package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.service.ResearchEabdaService;
import com.hospital.smart_hospital.service.ResearchEabdaService.HistoricalDiagnosis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research/eabda")
public class ResearchEabdaController {

    private final ResearchEabdaService researchEabdaService;

    public ResearchEabdaController(
            ResearchEabdaService researchEabdaService) {
        this.researchEabdaService = researchEabdaService;
    }

    @GetMapping
    public HistoricalDiagnosis getHistoricalDiagnosis(
            @RequestParam(defaultValue = "OP") String stage) {

        if (stage == null || stage.trim().isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Stage must not be empty."
            );
        }

        return researchEabdaService.diagnose(stage);
    }
}