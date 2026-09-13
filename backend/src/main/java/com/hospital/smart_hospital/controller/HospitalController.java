package com.hospital.smart_hospital.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HospitalController {

    @GetMapping("/api/hello")
    public String hello() {
        return "Smart Hospital Backend is working!";
    }
}