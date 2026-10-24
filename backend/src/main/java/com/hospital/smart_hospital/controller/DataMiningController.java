package com.hospital.smart_hospital.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class DataMiningController {

    private final JdbcTemplate researchJdbcTemplate;

    public DataMiningController(
            @Qualifier("researchJdbcTemplate")
            JdbcTemplate researchJdbcTemplate) {
        this.researchJdbcTemplate = researchJdbcTemplate;
    }

    @GetMapping("/api/data-mining/patterns")
    public Map<String, Object> getPatterns() {

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Stage waiting-time patterns
        List<Map<String, Object>> stagePatterns =
                researchJdbcTemplate.queryForList("""
                    SELECT
                        stage,
                        AVG(
                            TIMESTAMPDIFF(
                                SECOND,
                                queue_entry_time,
                                service_start_time
                            )
                        ) / 60.0 AS averageWaitingTime
                    FROM workflow_events
                    WHERE queue_entry_time IS NOT NULL
                      AND service_start_time IS NOT NULL
                    GROUP BY stage
                    ORDER BY averageWaitingTime DESC
                    """);

        // 2. Arrival-hour patterns
        List<Map<String, Object>> arrivalPatterns =
                researchJdbcTemplate.queryForList("""
                    SELECT
                        HOUR(arrival_time) AS hour,
                        COUNT(visit_id) AS patientCount
                    FROM visits
                    WHERE arrival_time IS NOT NULL
                    GROUP BY HOUR(arrival_time)
                    ORDER BY patientCount DESC
                    """);

        // 3. High-waiting events
        Long highWaitingEvents =
                researchJdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM workflow_events
                    WHERE queue_entry_time IS NOT NULL
                      AND service_start_time IS NOT NULL
                      AND TIMESTAMPDIFF(
                          MINUTE,
                          queue_entry_time,
                          service_start_time
                      ) >= 20
                    """, Long.class);

        // 4. Prepare visit-stage records for Apriori.
        // Maximum observed waiting time is used for each visit-stage pair.
        List<Map<String, Object>> transactionRows =
                researchJdbcTemplate.queryForList("""
                    SELECT
                        w.visit_id AS visitId,
                        w.stage AS stage,
                        v.patient_type AS patientType,
                        HOUR(v.arrival_time) AS arrivalHour,
                        MAX(
                            TIMESTAMPDIFF(
                                SECOND,
                                w.queue_entry_time,
                                w.service_start_time
                            ) / 60.0
                        ) AS maxWaitingTime
                    FROM workflow_events w
                    JOIN visits v
                        ON v.visit_id = w.visit_id
                    WHERE w.visit_id IS NOT NULL
                      AND w.stage IS NOT NULL
                      AND v.patient_type IS NOT NULL
                      AND v.arrival_time IS NOT NULL
                      AND w.queue_entry_time IS NOT NULL
                      AND w.service_start_time IS NOT NULL
                    GROUP BY
                        w.visit_id,
                        w.stage,
                        v.patient_type,
                        HOUR(v.arrival_time)
                    ORDER BY
                        w.visit_id,
                        w.stage
                    """);

        result.put("stagePatterns", stagePatterns);
        result.put("arrivalPatterns", arrivalPatterns);
        result.put(
                "highWaitingEvents",
                highWaitingEvents == null ? 0L : highWaitingEvents
        );
        result.put("apriori", buildApriori(transactionRows));

        return result;
    }

    /**
     * Mines association rules using one transaction per patient visit.
     *
     * Each transaction contains:
     * - Patient type
     * - Arrival period
     * - Stages reached
     * - Maximum-wait category for each stage
     *
     * Itemsets are mined up to size three.
     */
    private Map<String, Object> buildApriori(
            List<Map<String, Object>> rows) {

        final double minSupport = 0.05;
        final double minConfidence = 0.60;
        final int maxItemsetSize = 3;

        // Group all stage-level items into one transaction per visit.
        Map<Integer, Set<String>> visitTransactions =
                new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {

            if (row == null
                    || row.get("visitId") == null
                    || row.get("stage") == null
                    || row.get("patientType") == null
                    || row.get("arrivalHour") == null
                    || row.get("maxWaitingTime") == null) {
                continue;
            }

            int visitId =
                    ((Number) row.get("visitId")).intValue();

            String stage = row.get("stage")
                    .toString()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            String patientType = row.get("patientType")
                    .toString()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            int arrivalHour =
                    ((Number) row.get("arrivalHour")).intValue();

            double maxWaitingTime =
                    ((Number) row.get("maxWaitingTime")).doubleValue();

            Set<String> transaction =
                    visitTransactions.computeIfAbsent(
                            visitId,
                            id -> new TreeSet<>()
                    );

            transaction.add("PATIENT_TYPE=" + patientType);
            transaction.add("ARRIVAL=" + hourBucket(arrivalHour));
            transaction.add("STAGE=" + stage);

            transaction.add(
                    "STAGE_WAIT=" + stage + ":" +
                    waitBucket(maxWaitingTime)
            );
        }

        List<Set<String>> transactions =
                new ArrayList<>(visitTransactions.values());

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("transactionCount", transactions.size());
        output.put("minSupport", minSupport);
        output.put("minConfidence", minConfidence);

        if (transactions.isEmpty()) {
            output.put("frequentItemsets", List.of());
            output.put("rules", List.of());
            return output;
        }

        int minCount = Math.max(
                1,
                (int) Math.ceil(minSupport * transactions.size())
        );

        // Stores support counts for frequent itemsets.
        Map<String, Integer> supportCounts = new HashMap<>();

        // Collect every unique item.
        Set<String> allItems = new TreeSet<>();

        for (Set<String> transaction : transactions) {
            allItems.addAll(transaction);
        }

        // Find frequent one-item sets.
        List<Set<String>> level = new ArrayList<>();

        for (String item : allItems) {

            Set<String> oneItemSet = new TreeSet<>();
            oneItemSet.add(item);

            int count = supportCount(oneItemSet, transactions);

            if (count >= minCount) {
                level.add(oneItemSet);
                supportCounts.put(key(oneItemSet), count);
            }
        }

        List<Set<String>> frequent =
                new ArrayList<>(level);

        // Generate frequent itemsets of sizes two and three.
        for (int size = 2;
             size <= maxItemsetSize && !level.isEmpty();
             size++) {

            Set<Set<String>> candidates = new TreeSet<>(
                    Comparator.comparing(this::key)
            );

            for (int i = 0; i < level.size(); i++) {
                for (int j = i + 1; j < level.size(); j++) {

                    Set<String> candidate =
                            new TreeSet<>(level.get(i));

                    candidate.addAll(level.get(j));

                    if (candidate.size() != size) {
                        continue;
                    }

                    // Apriori pruning:
                    // Every immediate subset must be frequent.
                    if (allImmediateSubsetsFrequent(
                            candidate,
                            level)) {
                        candidates.add(candidate);
                    }
                }
            }

            List<Set<String>> nextLevel = new ArrayList<>();

            for (Set<String> candidate : candidates) {

                int count = supportCount(candidate, transactions);

                if (count >= minCount) {
                    nextLevel.add(candidate);
                    supportCounts.put(key(candidate), count);
                }
            }

            frequent.addAll(nextLevel);
            level = nextLevel;
        }

        // Format frequent itemsets.
        List<Map<String, Object>> frequentOutput =
                new ArrayList<>();

        for (Set<String> itemset : frequent) {

            Map<String, Object> item = new LinkedHashMap<>();

            int count = supportCounts.get(key(itemset));

            item.put("items", new ArrayList<>(itemset));
            item.put(
                    "support",
                    round(
                            (double) count /
                            transactions.size() * 100.0
                    )
            );
            item.put("count", count);

            frequentOutput.add(item);
        }

        frequentOutput.sort((a, b) -> {

            int comparison = Double.compare(
                    ((Number) b.get("support")).doubleValue(),
                    ((Number) a.get("support")).doubleValue()
            );

            if (comparison != 0) {
                return comparison;
            }

            return a.get("items").toString()
                    .compareTo(b.get("items").toString());
        });

        // Generate association rules.
        List<Map<String, Object>> rules = new ArrayList<>();

        for (Set<String> itemset : frequent) {

            if (itemset.size() < 2) {
                continue;
            }

            List<String> items = new ArrayList<>(itemset);
            int n = items.size();

            // Each nonempty proper subset becomes an antecedent.
            for (int mask = 1;
                 mask < (1 << n) - 1;
                 mask++) {

                Set<String> antecedent = new TreeSet<>();
                Set<String> consequent = new TreeSet<>();

                for (int i = 0; i < n; i++) {

                    if ((mask & (1 << i)) != 0) {
                        antecedent.add(items.get(i));
                    } else {
                        consequent.add(items.get(i));
                    }
                }

                int unionCount =
                        supportCounts.get(key(itemset));

                // Use cached support when available.
                // Otherwise, calculate it once and cache it.
                String antecedentKey = key(antecedent);

                Integer antecedentCount =
                        supportCounts.get(antecedentKey);

                if (antecedentCount == null) {
                    antecedentCount =
                            supportCount(antecedent, transactions);
                    supportCounts.put(
                            antecedentKey,
                            antecedentCount
                    );
                }

                if (antecedentCount == 0) {
                    continue;
                }

                double confidence =
                        (double) unionCount / antecedentCount;

                if (confidence >= minConfidence) {

                    Map<String, Object> rule =
                            new LinkedHashMap<>();

                    rule.put(
                            "antecedent",
                            new ArrayList<>(antecedent)
                    );
                    rule.put(
                            "consequent",
                            new ArrayList<>(consequent)
                    );
                    rule.put(
                            "support",
                            round(
                                    (double) unionCount /
                                    transactions.size() * 100.0
                            )
                    );
                    rule.put(
                            "confidence",
                            round(confidence * 100.0)
                    );

                    rules.add(rule);
                }
            }
        }

        // Sort rules by confidence, then support.
        rules.sort((a, b) -> {

            int comparison = Double.compare(
                    ((Number) b.get("confidence")).doubleValue(),
                    ((Number) a.get("confidence")).doubleValue()
            );

            if (comparison != 0) {
                return comparison;
            }

            return Double.compare(
                    ((Number) b.get("support")).doubleValue(),
                    ((Number) a.get("support")).doubleValue()
            );
        });

        // Preserve the existing API response limit.
        if (rules.size() > 12) {
            rules = new ArrayList<>(rules.subList(0, 12));
        }

        output.put(
                "frequentItemsets",
                frequentOutput.subList(
                        0,
                        Math.min(12, frequentOutput.size())
                )
        );

        output.put("rules", rules);

        return output;
    }

    /**
     * Returns true when every immediate subset of a candidate
     * appears in the previous frequent-itemset level.
     */
    private boolean allImmediateSubsetsFrequent(
            Set<String> candidate,
            List<Set<String>> previousLevel) {

        for (String removed : candidate) {

            Set<String> subset = new TreeSet<>(candidate);
            subset.remove(removed);

            boolean found = false;

            for (Set<String> previous : previousLevel) {

                if (previous.equals(subset)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Counts transactions containing every item in the itemset.
     */
    private int supportCount(
            Set<String> itemset,
            List<Set<String>> transactions) {

        int count = 0;

        for (Set<String> transaction : transactions) {

            if (transaction.containsAll(itemset)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Creates a stable key for an itemset.
     */
    private String key(Set<String> set) {
        return String.join("|", new TreeSet<>(set));
    }

    /**
     * Categorizes the maximum observed waiting time in minutes.
     */
    private String waitBucket(double wait) {

        if (wait >= 20) {
            return "HIGH";
        }

        if (wait >= 10) {
            return "MEDIUM";
        }

        return "LOW";
    }

    /**
     * Groups arrival times into periods of the day.
     */
    private String hourBucket(int hour) {

        if (hour < 8) {
            return "EARLY";
        }

        if (hour < 12) {
            return "MORNING";
        }

        if (hour < 17) {
            return "AFTERNOON";
        }

        if (hour < 21) {
            return "EVENING";
        }

        return "NIGHT";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}