package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    List<Resource> findByDepartmentIgnoreCase(String department);
}