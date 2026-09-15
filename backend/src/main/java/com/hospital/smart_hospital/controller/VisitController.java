package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.model.Visit;
import com.hospital.smart_hospital.repository.VisitRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class VisitController {

    private final VisitRepository visitRepository;

    public VisitController(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    @GetMapping("/api/visits")
    public List<Visit> getAllVisits() {
        return visitRepository.findAll();
    }
}