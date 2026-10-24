package com.hospital.smart_hospital.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResearchEabdaService {

private static final double HIGH_WAIT_THRESHOLD_MINUTES = 20.0;
private static final int REQUIRED_PERSISTENT_PERIODS = 3;

private final JdbcTemplate researchJdbcTemplate;
private final HospitalEmailService hospitalEmailService;

public ResearchEabdaService(
        @Qualifier("researchJdbcTemplate")
        JdbcTemplate researchJdbcTemplate,
        HospitalEmailService hospitalEmailService) {

    this.researchJdbcTemplate = researchJdbcTemplate;
    this.hospitalEmailService = hospitalEmailService;
}

public HistoricalDiagnosis diagnose(String stage) {

    String normalizedStage = stage.trim().toUpperCase();

    List<DailyWait> dailyData = researchJdbcTemplate.query(
            """
            SELECT
                DATE(queue_entry_time) AS event_date,
                COUNT(*) AS total_events,
                AVG(
                    TIMESTAMPDIFF(
                        MICROSECOND,
                        queue_entry_time,
                        service_start_time
                    ) / 60000000.0
                ) AS average_wait
            FROM workflow_events
            WHERE UPPER(stage) = ?
              AND queue_entry_time IS NOT NULL
              AND service_start_time IS NOT NULL
              AND service_start_time >= queue_entry_time
            GROUP BY DATE(queue_entry_time)
            ORDER BY event_date
            """,
            (rs, rowNum) -> new DailyWait(
                    rs.getDate("event_date").toLocalDate(),
                    rs.getInt("total_events"),
                    rs.getDouble("average_wait")
            ),
            normalizedStage
    );

    if (dailyData.isEmpty()) {
        hospitalEmailService.resetResearchAlert(normalizedStage);

        return new HistoricalDiagnosis(
                normalizedStage, "NO_DATA", 0, 0, 0, 0,
                0, 0,
                "No historical workflow observations were found."
        );
    }

    int baselineEnd = Math.max(
            1,
            (int) Math.floor(dailyData.size() * 0.70)
    );

    List<Double> baselineValues = new ArrayList<>();

    for (int i = 0; i < baselineEnd; i++) {
        baselineValues.add(dailyData.get(i).averageWait());
    }

    double baseline = baselineValues.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

    double latestWait =
            dailyData.get(dailyData.size() - 1).averageWait();

    double pressure = baseline > 0
            ? latestWait / baseline
            : (latestWait > 0
                ? Double.POSITIVE_INFINITY
                : 1.0);

    double increase = latestWait - baseline;

    int highWaitStreak = 0;
    int abnormalStreak = 0;

    int latestHighWaitStreak = 0;
    int latestAbnormalStreak = 0;

    // Evaluate daily observations after the historical baseline.
    for (int i = baselineEnd; i < dailyData.size(); i++) {

        DailyWait current = dailyData.get(i);

        // A gap greater than three days breaks consecutive persistence.
        if (i > baselineEnd) {
            LocalDate previousDate = dailyData.get(i - 1).date();

            if (ChronoUnit.DAYS.between(
                    previousDate,
                    current.date()
            ) > 3) {
                highWaitStreak = 0;
                abnormalStreak = 0;
            }
        }

        boolean highWait =
                current.averageWait() >= HIGH_WAIT_THRESHOLD_MINUTES;

        double currentIncrease =
                current.averageWait() - baseline;

        double currentPressure = baseline > 0
                ? current.averageWait() / baseline
                : (current.averageWait() > 0
                    ? Double.POSITIVE_INFINITY
                    : 1.0);

        boolean abnormal =
                (baseline > 0
                        && currentPressure >= 2.0
                        && currentIncrease >= 1.0)
                || (baseline > 0
                        && currentPressure >= 1.5
                        && currentIncrease >= 0.5)
                || currentIncrease >= 3.0;

        highWaitStreak = highWait ? highWaitStreak + 1 : 0;
        abnormalStreak = abnormal ? abnormalStreak + 1 : 0;

        latestHighWaitStreak = highWaitStreak;
        latestAbnormalStreak = abnormalStreak;
    }

    String state;
    String evidence;

    if (baseline == 0 && latestWait == 0) {

        state = "ZERO_BASELINE";
        evidence = "The baseline and latest average wait are both zero; "
                + "relative pressure cannot be assessed reliably.";

    } else if (baseline == 0) {

        state = "REVIEW_REQUIRED";
        evidence = "The baseline is zero but the latest average wait is "
                + "positive. Review the historical data before interpreting "
                + "relative pressure.";

    } else if (
            latestHighWaitStreak >= REQUIRED_PERSISTENT_PERIODS
            || latestAbnormalStreak >= REQUIRED_PERSISTENT_PERIODS
    ) {

        state = "PERSISTENT_BOTTLENECK";

        if (latestHighWaitStreak >= REQUIRED_PERSISTENT_PERIODS) {
            evidence = String.format(
                    "Persistent bottleneck detected: daily average "
                            + "waiting time has remained at or above "
                            + "%.2f minutes for %d consecutive daily periods.",
                    HIGH_WAIT_THRESHOLD_MINUTES,
                    latestHighWaitStreak
            );
        } else {
            evidence = String.format(
                    "Persistent bottleneck detected: baseline-based "
                            + "abnormal waiting has continued for "
                            + "%d consecutive daily periods.",
                    latestAbnormalStreak
            );
        }

    } else if (pressure >= 2.0 && increase >= 1.0) {

        state = "HIGH_WAIT";
        evidence = "The latest daily average is at least twice the "
                + "historical baseline and at least one minute higher.";

    } else if (pressure >= 1.5 && increase >= 0.5) {

        state = "ELEVATED_WAIT";
        evidence = "The latest daily average is elevated compared with "
                + "the historical baseline.";

    } else if (increase >= 3.0) {

        state = "RISING_WAIT";
        evidence = "The latest daily average is at least 3 minutes above "
                + "the historical baseline, indicating a rising wait.";

    } else {

        state = "NORMAL";
        evidence = "The latest daily average does not meet the configured "
                + "elevated or rising-wait thresholds.";
    }

    HistoricalDiagnosis diagnosis = new HistoricalDiagnosis(
            normalizedStage,
            state,
            round(baseline),
            Double.isFinite(pressure) ? round(pressure) : -1.0,
            round(latestWait),
            dailyData.size(),
            latestHighWaitStreak,
            latestAbnormalStreak,
            evidence
    );

    if ("PERSISTENT_BOTTLENECK".equals(state)) {
        hospitalEmailService.sendResearchBottleneckAlert(
                normalizedStage,
                latestWait,
                baseline,
                Double.isFinite(pressure) ? pressure : -1.0,
                evidence
        );
    } else {
        hospitalEmailService.resetResearchAlert(normalizedStage);
    }

    return diagnosis;
}

private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
}

private record DailyWait(
        LocalDate date,
        int totalEvents,
        double averageWait
) {
}

public record HistoricalDiagnosis(
        String stage,
        String state,
        double baselineAverageMinutes,
        double pressureRatio,
        double latestAverageMinutes,
        int daysAnalyzed,
        int highWaitConsecutiveDays,
        int abnormalConsecutiveDays,
        String evidence) {
}

}