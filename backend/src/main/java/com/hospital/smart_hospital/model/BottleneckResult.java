package com.hospital.smart_hospital.model;

public class BottleneckResult {

    private String stage;
    private Double averageWaitingTime;
    private Double averageServiceTime;
    private String severity;
    private String eabdaState;
    private Double baselineWaitingTime;
    private Double waitingPressure;
    private Double trend;
    private Integer persistenceCount;
    private String evidence;

    public BottleneckResult(
            String stage,
            Double averageWaitingTime,
            Double averageServiceTime,
            String severity,
            String eabdaState,
            Double baselineWaitingTime,
            Double waitingPressure,
            Double trend,
            Integer persistenceCount,
            String evidence) {

        this.stage = stage;
        this.averageWaitingTime = averageWaitingTime;
        this.averageServiceTime = averageServiceTime;
        this.severity = severity;
        this.eabdaState = eabdaState;
        this.baselineWaitingTime = baselineWaitingTime;
        this.waitingPressure = waitingPressure;
        this.trend = trend;
        this.persistenceCount = persistenceCount;
        this.evidence = evidence;
    }

    public String getStage() { return stage; }
    public Double getAverageWaitingTime() { return averageWaitingTime; }
    public Double getAverageServiceTime() { return averageServiceTime; }
    public String getSeverity() { return severity; }
    public String getEabdaState() { return eabdaState; }
    public Double getBaselineWaitingTime() { return baselineWaitingTime; }
    public Double getWaitingPressure() { return waitingPressure; }
    public Double getTrend() { return trend; }
    public Integer getPersistenceCount() { return persistenceCount; }
    public String getEvidence() { return evidence; }
}
