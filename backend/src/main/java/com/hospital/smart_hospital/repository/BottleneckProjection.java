package com.hospital.smart_hospital.repository;

public interface BottleneckProjection {

    String getStage();

    Double getAverageWaitingTime();

    Double getAverageServiceTime();
}