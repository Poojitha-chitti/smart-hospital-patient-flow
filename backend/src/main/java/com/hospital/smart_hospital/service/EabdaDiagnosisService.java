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

    private static final double HIGH_WAIT_THRESHOLD_MINUTES = 20.0;
    private static final int REQUIRED_PERSISTENT_PERIODS = 3;
    private static final int BASELINE_PERCENT = 70;

    public Diagnosis diagnose(String stage, List<WorkflowEvent> events) {

        List<DailyPoint> points = aggregateDaily(events, stage);

        if (points.isEmpty()) {
            return new Diagnosis(
                    "NORMAL", 0.0, 1.0, 0.0, 0,
                    "No workflow observations are available."
            );
        }

        int baselineEnd = Math.max(
                1,
                (int) Math.floor(points.size() * BASELINE_PERCENT / 100.0)
        );

        List<Double> baselineValues = new ArrayList<>();

        for (int i = 0; i < baselineEnd; i++) {
            baselineValues.add(points.get(i).waiting());
        }

        double baseline = median(baselineValues);
        double mad = medianAbsoluteDeviation(baselineValues, baseline);

        double abnormalThreshold =
                calculatePositiveRzThreshold(points, baselineEnd, baseline, mad);

        String state = "NORMAL";

        int persistence = 0;
        int highWaitPersistence = 0;

        double latestTrend = 0.0;

        double latestPressure = safePressure(
                points.get(points.size() - 1).waiting(),
                baseline
        );

        String latestEvidence = "Waiting is within the stage baseline.";

        String previousState = "NORMAL";

        int recoveryCounter = 0;

        for (int i = baselineEnd; i < points.size(); i++) {

            DailyPoint current = points.get(i);

            // Reset consecutive-period counters when there is a large data gap.
            if (i > baselineEnd
                    && ChronoUnit.DAYS.between(
                            points.get(i - 1).date(),
                            current.date()
                    ) > 3) {

                persistence = 0;
                highWaitPersistence = 0;
                recoveryCounter = 0;
                previousState = "NORMAL";
            }

            double rz = positiveRz(current.waiting(), baseline, mad);

            // Existing EABDA baseline-based detection.
            boolean abnormal =
                    rz >= abnormalThreshold
                            && current.waiting() > baseline;

            // Additional absolute high-wait detection.
            boolean highWait =
                    current.waiting() >= HIGH_WAIT_THRESHOLD_MINUTES;

            // Track baseline-based abnormal persistence separately.
            if (abnormal) {
                persistence++;
            } else {
                persistence = 0;
            }

            // Track consecutive high-wait daily periods separately.
            if (highWait) {
                highWaitPersistence++;
            } else {
                highWaitPersistence = 0;
            }

            double trend = recentSlope(points, i, 3);

            boolean increasing = trend > 0.5;
            boolean decreasing = trend < -0.5;

            // Either detection rule can establish persistence.
            boolean persistent =
                    persistence >= REQUIRED_PERSISTENT_PERIODS
                            || highWaitPersistence >= REQUIRED_PERSISTENT_PERIODS;

            String nextState;

            if (previousState.equals("PERSISTENT")
                    && decreasing
                    && current.waiting() > baseline
                    && !persistent) {

                recoveryCounter++;

                nextState = recoveryCounter >= 2
                        ? "RECOVERING"
                        : "PERSISTENT";

            } else if (persistent) {

                recoveryCounter = 0;
                nextState = "PERSISTENT";

            } else if (abnormal && increasing) {

                recoveryCounter = 0;
                nextState = "EMERGING";

            } else if (abnormal || highWait) {

                recoveryCounter = 0;
                nextState = "ACTIVE";

            } else {

                recoveryCounter = 0;
                nextState = "NORMAL";
            }

            String evidence;

            if (highWait) {

                evidence = String.format(
                        "Daily median waiting time %.2f minutes meets or exceeds "
                                + "the configured high-wait threshold of %.2f minutes. "
                                + "Consecutive high-wait periods: %d. "
                                + "Baseline: %.2f minutes.",
                        current.waiting(),
                        HIGH_WAIT_THRESHOLD_MINUTES,
                        highWaitPersistence,
                        baseline
                );

                if (abnormal) {
                    evidence += String.format(
                            " The EABDA baseline-based abnormality criterion "
                                    + "is also satisfied (robust deviation %.2f).",
                            rz
                    );
                }

                if (persistent) {
                    evidence += " The persistence criterion has been met.";
                }

            } else {

                evidence = buildEvidence(
                        abnormal,
                        increasing,
                        decreasing,
                        persistence,
                        current.waiting(),
                        baseline,
                        rz,
                        trend
                );
            }

            state = nextState;
            previousState = nextState;

            latestTrend = trend;

            latestPressure = safePressure(
                    current.waiting(),
                    baseline
            );

            latestEvidence = evidence;
        }

        if (points.size() <= baselineEnd) {
            DailyPoint latest = points.get(points.size() - 1);

            latestPressure = safePressure(latest.waiting(), baseline);
            latestTrend = recentSlope(points, points.size() - 1, 3);
        }

        // Report the larger of the two consecutive-period counters.
        int reportedPersistence = Math.max(
                persistence,
                highWaitPersistence
        );

        return new Diagnosis(
                state,
                baseline,
                latestPressure,
                latestTrend,
                reportedPersistence,
                latestEvidence
        );
    }

    private List<DailyPoint> aggregateDaily(
            List<WorkflowEvent> events,
            String stage
    ) {

        Map<LocalDate, List<Double>> dailyWaiting = new HashMap<>();

        if (events == null) {
            return new ArrayList<>();
        }

        for (WorkflowEvent event : events) {

            if (event == null
                    || event.getStage() == null
                    || event.getQueue_entry_time() == null
                    || event.getService_start_time() == null) {
                continue;
            }

            if (stage != null
                    && !stage.equalsIgnoreCase(event.getStage())) {
                continue;
            }

            long waitingSeconds = ChronoUnit.SECONDS.between(
                    event.getQueue_entry_time(),
                    event.getService_start_time()
            );

            if (waitingSeconds < 0) {
                continue;
            }

            double waitingMinutes = waitingSeconds / 60.0;

            LocalDate date = event.getQueue_entry_time().toLocalDate();

            dailyWaiting
                    .computeIfAbsent(date, key -> new ArrayList<>())
                    .add(waitingMinutes);
        }

        List<DailyPoint> points = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Double>> entry
                : dailyWaiting.entrySet()) {

            points.add(new DailyPoint(
                    entry.getKey(),
                    median(entry.getValue())
            ));
        }

        points.sort(Comparator.comparing(DailyPoint::date));

        return points;
    }

    private double calculatePositiveRzThreshold(
            List<DailyPoint> points,
            int baselineEnd,
            double baseline,
            double mad
    ) {

        List<Double> scores = new ArrayList<>();

        for (int i = 0; i < baselineEnd; i++) {

            double rz = positiveRz(
                    points.get(i).waiting(),
                    baseline,
                    mad
            );

            if (rz > 0) {
                scores.add(rz);
            }
        }

        if (scores.isEmpty()) {
            return mad == 0.0 ? 0.0 : 0.5;
        }

        return Math.max(0.5, percentile(scores, 95));
    }

    private double positiveRz(
            double waiting,
            double baseline,
            double mad
    ) {

        if (waiting <= baseline) {
            return 0.0;
        }

        if (mad == 0.0) {
            return 1.0;
        }

        return (waiting - baseline) / (1.4826 * mad);
    }

    private double recentSlope(
            List<DailyPoint> points,
            int endIndex,
            int window
    ) {

        int startIndex = Math.max(0, endIndex - window + 1);

        if (endIndex <= startIndex) {
            return 0.0;
        }

        int n = endIndex - startIndex + 1;

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXX = 0.0;

        for (int i = startIndex; i <= endIndex; i++) {

            double x = i - startIndex;
            double y = points.get(i).waiting();

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double denominator = n * sumXX - sumX * sumX;

        if (denominator == 0.0) {
            return 0.0;
        }

        return (n * sumXY - sumX * sumY) / denominator;
    }

    private String buildEvidence(
            boolean abnormal,
            boolean increasing,
            boolean decreasing,
            int persistence,
            double waiting,
            double baseline,
            double rz,
            double trend
    ) {

        if (!abnormal) {
            return "Waiting is within the stage baseline.";
        }

        String direction;

        if (increasing) {
            direction = "recent trend is increasing";
        } else if (decreasing) {
            direction = "recent trend is decreasing";
        } else {
            direction = "recent trend is approximately stable";
        }

        return String.format(
                "Waiting %.2f minutes is above baseline %.2f minutes; "
                        + "robust deviation %.2f; %s; "
                        + "abnormal persistence count %d.",
                waiting,
                baseline,
                rz,
                direction,
                persistence
        );
    }

    private double safePressure(
            double waiting,
            double baseline
    ) {

        if (baseline <= 0.0) {
            return waiting > 0.0 ? 1.0 : 0.0;
        }

        return Math.max(0.0, (waiting - baseline) / baseline);
    }

    private double medianAbsoluteDeviation(
            List<Double> values,
            double median
    ) {

        List<Double> deviations = new ArrayList<>();

        for (double value : values) {
            deviations.add(Math.abs(value - median));
        }

        return median(deviations);
    }

    private double median(List<Double> values) {

        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        List<Double> sorted = new ArrayList<>(values);

        sorted.sort(Double::compareTo);

        int size = sorted.size();
        int middle = size / 2;

        if (size % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        }

        return sorted.get(middle);
    }

    private double percentile(
            List<Double> values,
            double percentile
    ) {

        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        List<Double> sorted = new ArrayList<>(values);

        sorted.sort(Double::compareTo);

        double rank = (percentile / 100.0) * (sorted.size() - 1);

        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);

        if (lowerIndex == upperIndex) {
            return sorted.get(lowerIndex);
        }

        double fraction = rank - lowerIndex;

        return sorted.get(lowerIndex)
                + fraction
                * (sorted.get(upperIndex) - sorted.get(lowerIndex));
    }

    private record DailyPoint(
            LocalDate date,
            double waiting
    ) {
    }

    public record Diagnosis(
            String state,
            double baseline,
            double pressure,
            double trend,
            int persistence,
            String evidence
    ) {
    }
}