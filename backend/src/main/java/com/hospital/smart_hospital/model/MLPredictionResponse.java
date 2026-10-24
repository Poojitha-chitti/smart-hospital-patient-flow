package com.hospital.smart_hospital.model;

public class MLPredictionResponse {

    private int prediction;
    private String condition;

    public int getPrediction() {
        return prediction;
    }

    public void setPrediction(int prediction) {
        this.prediction = prediction;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}