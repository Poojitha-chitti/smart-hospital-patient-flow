package com.hospital.smart_hospital.model;

public class BottleneckResult {

    private String stage;
    private Double averageWaitingTime;
    private Double averageServiceTime;
    private String severity;

    public BottleneckResult(
            String stage,
            Double averageWaitingTime,
            Double averageServiceTime,
            String severity) {

        this.stage = stage;
        this.averageWaitingTime = averageWaitingTime;
        this.averageServiceTime = averageServiceTime;
        this.severity = severity;
    }

    public String getStage() {
        return stage;
    }

    public Double getAverageWaitingTime() {
        return averageWaitingTime;
    }

    public Double getAverageServiceTime() {
        return averageServiceTime;
    }

    public String getSeverity() {
        return severity;
    }
}