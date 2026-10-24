package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.model.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EabdaV3ComparisonTest {

    private static final Path BASE_DIR = Path.of(
        "C:/Users/Dell/Downloads/smart-hospital-eabda-v1/"
        + "smart-hospital-backup/dataset_generator_new"
    );

    private static final Path INPUT_FILE =
        BASE_DIR.resolve("des_workflow_events_v3_clean.csv");

    private static final Path PYTHON_RESULTS =
        BASE_DIR.resolve("eabda_v3_daily_results.csv");

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .appendFraction(
                ChronoField.NANO_OF_SECOND, 0, 9, true
            )
            .toFormatter();

    private static final double HIGH_WAIT_THRESHOLD = 20.0;
    private static final double TOLERANCE = 0.00011;

    @Test
    void validateEabdaStatisticsAndJavaDiagnosis() throws IOException {

        assertTrue(
            Files.exists(INPUT_FILE),
            "V3 workflow CSV not found: " + INPUT_FILE
        );

        assertTrue(
            Files.exists(PYTHON_RESULTS),
            "Python results CSV not found: " + PYTHON_RESULTS
        );

        // --------------------------------------------------
        // 1. Load OP events and calculate daily medians
        // --------------------------------------------------

        Map<LocalDate, List<Double>> waitsByDate = new TreeMap<>();
        List<WorkflowEvent> opEvents = new ArrayList<>();

        List<String> inputLines = Files.readAllLines(INPUT_FILE);

        for (int i = 1; i < inputLines.size(); i++) {

            String[] columns = inputLines.get(i).split(",", -1);

            if (columns.length < 4) {
                throw new IllegalArgumentException(
                    "Invalid input CSV row: " + (i + 1)
                );
            }

            if (!"OP".equalsIgnoreCase(columns[1].trim())) {
                continue;
            }

            String queueText = columns[2].trim();
            String startText = columns[3].trim();

            if (queueText.isEmpty() || startText.isEmpty()) {
                continue;
            }

            LocalDateTime queueTime =
                LocalDateTime.parse(queueText, TIMESTAMP_FORMAT);

            LocalDateTime startTime =
                LocalDateTime.parse(startText, TIMESTAMP_FORMAT);

            // Match Java EABDA's whole-second waiting calculation.
            long seconds = ChronoUnit.SECONDS.between(
                queueTime, startTime
            );

            if (seconds < 0) {
                continue;
            }

            double waitMinutes = seconds / 60.0;

            waitsByDate
                .computeIfAbsent(
                    queueTime.toLocalDate(),
                    key -> new ArrayList<>()
                )
                .add(waitMinutes);

            WorkflowEvent event = new WorkflowEvent();
            event.setStage("OP");
            event.setQueue_entry_time(queueTime);
            event.setService_start_time(startTime);

            opEvents.add(event);
        }

        Map<LocalDate, Double> javaDailyMedians = new TreeMap<>();

        for (Map.Entry<LocalDate, List<Double>> entry
                : waitsByDate.entrySet()) {

            javaDailyMedians.put(
                entry.getKey(),
                median(entry.getValue())
            );
        }

        List<Double> dailyValues =
            new ArrayList<>(javaDailyMedians.values());

        int numberOfDays = dailyValues.size();

        assertTrue(numberOfDays > 0, "No daily OP data found.");

        // --------------------------------------------------
        // 2. Calculate baseline and MAD independently
        // --------------------------------------------------

        int baselineEnd = Math.max(
            1,
            (int) Math.floor(numberOfDays * 0.70)
        );

        List<Double> baselineValues = new ArrayList<>(
            dailyValues.subList(0, baselineEnd)
        );

        double baseline = median(baselineValues);

        List<Double> absoluteDeviations = new ArrayList<>();

        for (double value : baselineValues) {
            absoluteDeviations.add(Math.abs(value - baseline));
        }

        double mad = median(absoluteDeviations);

        // --------------------------------------------------
        // 3. Calculate the positive robust-Z threshold
        // --------------------------------------------------

        List<Double> positiveScores = new ArrayList<>();

        for (double value : baselineValues) {

            double score = positiveRobustZ(value, baseline, mad);

            if (score > 0) {
                positiveScores.add(score);
            }
        }

        double abnormalThreshold;

        if (positiveScores.isEmpty()) {
            abnormalThreshold = mad == 0 ? 0.0 : 0.5;
        } else {
            abnormalThreshold = Math.max(
                0.5,
                percentile(positiveScores, 95)
            );
        }

        // --------------------------------------------------
        // 4. Load Python daily results
        // --------------------------------------------------

        List<String> pythonLines =
            Files.readAllLines(PYTHON_RESULTS);

        Map<LocalDate, String[]> pythonByDate = new TreeMap<>();

        for (int i = 1; i < pythonLines.size(); i++) {

            String[] columns = pythonLines.get(i).split(",", -1);

            if (columns.length < 14) {
                throw new IllegalArgumentException(
                    "Invalid Python results row: " + (i + 1)
                );
            }

            LocalDate date = LocalDate.parse(columns[0].trim());
            pythonByDate.put(date, columns);
        }

        int comparedDays = 0;
        int mismatchedDays = 0;

        double maxMedianDifference = 0;
        double maxRobustZDifference = 0;

        // --------------------------------------------------
        // 5. Compare Python and Java-derived daily statistics
        // --------------------------------------------------

        for (Map.Entry<LocalDate, Double> entry
                : javaDailyMedians.entrySet()) {

            LocalDate date = entry.getKey();
            double javaMedian = entry.getValue();

            String[] python = pythonByDate.get(date);

            if (python == null) {
                System.out.println(
                    "Missing Python result for " + date
                );
                mismatchedDays++;
                continue;
            }

            double pythonMedian = Double.parseDouble(python[2]);
            double pythonBaseline = Double.parseDouble(python[3]);
            double pythonMad = Double.parseDouble(python[4]);
            double pythonRobustZ = Double.parseDouble(python[5]);

            double medianDifference =
                Math.abs(javaMedian - pythonMedian);

            maxMedianDifference = Math.max(
                maxMedianDifference,
                medianDifference
            );

            double robustZ = positiveRobustZ(
                javaMedian,
                baseline,
                mad
            );

            double robustZDifference =
                Math.abs(robustZ - pythonRobustZ);

            maxRobustZDifference = Math.max(
                maxRobustZDifference,
                robustZDifference
            );

            boolean abnormal =
                robustZ >= abnormalThreshold
                && javaMedian > baseline;

            boolean highWait =
                javaMedian >= HIGH_WAIT_THRESHOLD;

            boolean pythonAbnormal =
                Boolean.parseBoolean(python[6]);

            boolean pythonHighWait =
                Boolean.parseBoolean(python[7]);

            boolean mismatch =
                medianDifference > TOLERANCE
                || Math.abs(baseline - pythonBaseline) > TOLERANCE
                || Math.abs(mad - pythonMad) > TOLERANCE
                || robustZDifference > TOLERANCE
                || abnormal != pythonAbnormal
                || highWait != pythonHighWait;

            if (mismatch) {
                mismatchedDays++;

                System.out.printf(
                    "Mismatch %s | median Java=%.6f Python=%.4f"
                    + " | Z Java=%.5f Python=%.5f%n",
                    date,
                    javaMedian,
                    pythonMedian,
                    robustZ,
                    pythonRobustZ
                );
            }

            comparedDays++;
        }

        // --------------------------------------------------
        // 6. Run the actual Java EABDA service
        // --------------------------------------------------

        EabdaDiagnosisService service =
            new EabdaDiagnosisService();

        EabdaDiagnosisService.Diagnosis diagnosis =
            service.diagnose("OP", opEvents);

        // --------------------------------------------------
        // 7. Print validation results
        // --------------------------------------------------

        System.out.println();
        System.out.println("======================================");
        System.out.println("EABDA V3 STATISTICAL VALIDATION");
        System.out.println("======================================");

        System.out.println("OP events loaded       : " + opEvents.size());
        System.out.println("Daily dates compared   : " + comparedDays);
        System.out.println("Mismatched dates       : " + mismatchedDays);

        System.out.printf(
            "Baseline              : %.6f minutes%n",
            baseline
        );

        System.out.printf(
            "MAD                   : %.6f minutes%n",
            mad
        );

        System.out.printf(
            "Abnormality threshold : %.6f%n",
            abnormalThreshold
        );

        System.out.printf(
            "Max median difference : %.8f minutes%n",
            maxMedianDifference
        );

        System.out.printf(
            "Max robust-Z difference: %.8f%n",
            maxRobustZDifference
        );

        System.out.println();
        System.out.println("ACTUAL JAVA EABDA DIAGNOSIS");
        System.out.println("State                  : " + diagnosis.state());

        System.out.printf(
            "Baseline               : %.4f minutes%n",
            diagnosis.baseline()
        );

        System.out.printf(
            "Pressure               : %.4f%n",
            diagnosis.pressure()
        );

        System.out.println("Trend slope            : " + diagnosis.trend());
        System.out.println("Persistence            : " + diagnosis.persistence());
        System.out.println("Evidence               : " + diagnosis.evidence());

        System.out.println("======================================");

        // Fail the test if daily statistical results do not match.
        assertEquals(
            0,
            mismatchedDays,
            "Python and Java-derived daily statistics differ."
        );

        // Verify that the independently calculated baseline
        // agrees with the actual Java service's reported baseline.
        assertEquals(
            baseline,
            diagnosis.baseline(),
            TOLERANCE,
            "Calculated baseline differs from Java EABDA baseline."
        );
    }

    private static double median(List<Double> input) {

        if (input.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot calculate median of an empty list."
            );
        }

        List<Double> values = new ArrayList<>(input);
        values.sort(Comparator.naturalOrder());

        int size = values.size();

        if (size % 2 == 0) {
            return (
                values.get(size / 2 - 1)
                + values.get(size / 2)
            ) / 2.0;
        }

        return values.get(size / 2);
    }

    private static double positiveRobustZ(
            double waiting,
            double baseline,
            double mad) {

        if (waiting <= baseline) {
            return 0.0;
        }

        if (mad == 0) {
            return 1.0;
        }

        return (waiting - baseline) / (1.4826 * mad);
    }

    private static double percentile(
            List<Double> input,
            double percent) {

        if (input.isEmpty()) {
            return 0.0;
        }

        List<Double> values = new ArrayList<>(input);
        values.sort(Comparator.naturalOrder());

        if (values.size() == 1) {
            return values.get(0);
        }

        double position =
            (values.size() - 1) * percent / 100.0;

        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);

        if (lower == upper) {
            return values.get(lower);
        }

        double fraction = position - lower;

        return values.get(lower) * (1.0 - fraction)
            + values.get(upper) * fraction;
    }
}