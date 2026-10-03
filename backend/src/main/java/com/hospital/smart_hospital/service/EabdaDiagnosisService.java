package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.model.WorkflowEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EabdaDiagnosisService {

    public Diagnosis diagnose(String stage, List<WorkflowEvent> events) {
        List<DailyPoint> points = aggregateDaily(events, stage);
        if (points.isEmpty()) {
            return new Diagnosis("NORMAL", 0.0, 1.0, 0.0, 0,
                    "No workflow observations are available.");
        }

        int baselineEnd = Math.max(1, (int) Math.floor(points.size() * 0.70));
        List<Double> baselineValues = new ArrayList<>();
        for (int i = 0; i < baselineEnd; i++) {
            baselineValues.add(points.get(i).waiting);
        }

        double baseline = median(baselineValues);
        double mad = medianAbsoluteDeviation(baselineValues, baseline);
        double abnormalThreshold = calculatePositiveRzThreshold(points, baselineEnd, baseline, mad);

        String state = "NORMAL";
        int persistence = 0;
        int persistentPeriods = 0;
        double latestTrend = 0.0;
        double latestPressure = safePressure(points.get(points.size() - 1).waiting, baseline);
        String latestEvidence = "Waiting is within the stage baseline.";
        String previousState = "NORMAL";
        int recoveryCounter = 0;

        for (int i = baselineEnd; i < points.size(); i++) {
            DailyPoint current = points.get(i);
            if (i > baselineEnd && ChronoUnit.DAYS.between(points.get(i - 1).date, current.date) > 3) {
                persistence = 0;
                recoveryCounter = 0;
                previousState = "NORMAL";
            }

            double rz = positiveRz(current.waiting, baseline, mad);
            boolean abnormal = rz >= abnormalThreshold && current.waiting > baseline;
            double trend = recentSlope(points, i, 3);
            boolean increasing = trend > 0.5;
            boolean decreasing = trend < -0.5;

            if (abnormal) {
                persistence++;
            } else {
                persistence = 0;
            }

            String nextState;
            if (previousState.equals("PERSISTENT") && decreasing && current.waiting > baseline) {
                recoveryCounter++;
                nextState = recoveryCounter >= 2 ? "RECOVERING" : "PERSISTENT";
            } else if (abnormal && persistence >= 3) {
                recoveryCounter = 0;
                nextState = "PERSISTENT";
                persistentPeriods++;
            } else if (abnormal && increasing) {
                recoveryCounter = 0;
                nextState = "EMERGING";
            } else if (abnormal) {
                recoveryCounter = 0;
                nextState = "ACTIVE";
            } else {
                recoveryCounter = 0;
                nextState = "NORMAL";
            }

            String evidence = buildEvidence(abnormal, increasing, decreasing, persistence, current.waiting, baseline, rz, trend);
            state = nextState;
            previousState = nextState;
            latestTrend = trend;
            latestPressure = safePressure(current.waiting, baseline);
            latestEvidence = evidence;
        }

        if (points.size() <= baselineEnd) {
            latestPressure = safePressure(points.get(points.size() - 1).waiting, baseline);
            latestTrend = recentSlope(points, points.size() - 1, 3);
        }

        return new Diagnosis(state, baseline, latestPressure, latestTrend, persistence, latestEvidence);
    }

    private List<DailyPoint> aggregateDaily(List<WorkflowEvent> events, String stage) {
        Map<LocalDate, List<Double>> byDate = new HashMap<>();
        for (WorkflowEvent event : events) {
            if (!stage.equalsIgnoreCase(event.getStage()) || event.getQueue_entry_time() == null || event.getService_start_time() == null) {
                continue;
            }
            long seconds = ChronoUnit.SECONDS.between(event.getQueue_entry_time(), event.getService_start_time());
            if (seconds < 0) {
                continue;
            }
            byDate.computeIfAbsent(event.getQueue_entry_time().toLocalDate(), d -> new ArrayList<>())
                    .add(seconds / 60.0);
        }

        List<DailyPoint> points = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Double>> entry : byDate.entrySet()) {
            points.add(new DailyPoint(entry.getKey(), median(entry.getValue())));
        }
        points.sort(Comparator.comparing(p -> p.date));
        return points;
    }

    private double calculatePositiveRzThreshold(List<DailyPoint> points, int baselineEnd, double baseline, double mad) {
        List<Double> scores = new ArrayList<>();
        for (int i = 0; i < baselineEnd; i++) {
            double rz = positiveRz(points.get(i).waiting, baseline, mad);
            if (rz > 0) scores.add(rz);
        }
        if (scores.isEmpty()) return mad == 0 ? 0.0 : 0.5;
        scores.sort(Double::compareTo);
        double q95 = percentile(scores, 0.95);
        return Math.max(0.5, q95);
    }

    private double positiveRz(double waiting, double baseline, double mad) {
        if (waiting <= baseline) return 0.0;
        if (mad == 0) return waiting > baseline ? 1.0 : 0.0;
        return (waiting - baseline) / (1.4826 * mad);
    }

    private double recentSlope(List<DailyPoint> points, int end, int window) {
        int start = Math.max(0, end - window + 1);
        int n = end - start + 1;
        if (n < 2) return 0.0;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int j = 0; j < n; j++) {
            double x = j;
            double y = points.get(start + j).waiting;
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x;
        }
        double denominator = n * sumX2 - sumX * sumX;
        return denominator == 0 ? 0.0 : (n * sumXY - sumX * sumY) / denominator;
    }

    private String buildEvidence(boolean abnormal, boolean increasing, boolean decreasing,
                                 int persistence, double waiting, double baseline,
                                 double rz, double trend) {
        if (!abnormal) return "Waiting is within the stage baseline.";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Waiting %.2f min is above baseline %.2f min", waiting, baseline));
        sb.append(String.format("; robust deviation %.2f", rz));
        if (increasing) sb.append("; recent trend is increasing");
        else if (decreasing) sb.append("; recent trend is decreasing");
        else sb.append("; recent trend is approximately stable");
        sb.append(String.format("; abnormal persistence count %d", persistence));
        return sb.toString();
    }

    private double safePressure(double waiting, double baseline) {
        return baseline == 0 ? 1.0 : waiting / baseline;
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> copy = new ArrayList<>(values);
        copy.sort(Double::compareTo);
        int n = copy.size();
        return n % 2 == 1 ? copy.get(n / 2) : (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
    }

    private double medianAbsoluteDeviation(List<Double> values, double center) {
        List<Double> deviations = new ArrayList<>();
        for (double value : values) deviations.add(Math.abs(value - center));
        return median(deviations);
    }

    private double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0.0;
        double index = p * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sorted.get(lower);
        return sorted.get(lower) + (index - lower) * (sorted.get(upper) - sorted.get(lower));
    }

    private record DailyPoint(LocalDate date, double waiting) {}

    public record Diagnosis(String state, double baseline, double pressure,
                            double trend, int persistence, String evidence) {}
}
