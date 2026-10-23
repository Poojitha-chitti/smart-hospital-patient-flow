package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.repository.WorkflowEventRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class DataMiningController {

    private final WorkflowEventRepository repository;

    public DataMiningController(WorkflowEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/data-mining/patterns")
    public Map<String, Object> getPatterns() {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> stagePatterns = new ArrayList<>();
        for (Object[] row : repository.findStageWaitingPatterns()) {
            Map<String, Object> item = new HashMap<>();
            item.put("stage", row[0]);
            item.put("averageWaitingTime", row[1]);
            stagePatterns.add(item);
        }

        List<Map<String, Object>> arrivalPatterns = new ArrayList<>();
        for (Object[] row : repository.findArrivalHourPatterns()) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", row[0]);
            item.put("patientCount", row[1]);
            arrivalPatterns.add(item);
        }

        result.put("stagePatterns", stagePatterns);
        result.put("arrivalPatterns", arrivalPatterns);
        result.put("highWaitingEvents", repository.countHighWaitingEvents());
        result.put("apriori", buildApriori(repository.findAprioriTransactions()));
        return result;
    }

    /**
     * Lightweight Apriori association-rule mining over workflow-event transactions.
     * Each workflow event becomes a transaction containing stage, patient type,
     * waiting-time bucket and arrival-hour bucket. Rules are mined from the
     * observed data rather than being hard-coded.
     */
    private Map<String, Object> buildApriori(List<Object[]> rows) {
        final double minSupport = 0.05;
        final double minConfidence = 0.60;

        List<Set<String>> transactions = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 4 || row[0] == null || row[1] == null || row[2] == null || row[3] == null) continue;
            double wait = ((Number) row[2]).doubleValue();
            int hour = ((Number) row[3]).intValue();
            Set<String> t = new LinkedHashSet<>();
            t.add("STAGE=" + row[0].toString().toUpperCase(Locale.ROOT));
            t.add("PATIENT_TYPE=" + row[1].toString().toUpperCase(Locale.ROOT));
            t.add("WAIT=" + waitBucket(wait));
            t.add("ARRIVAL=" + hourBucket(hour));
            transactions.add(t);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("transactionCount", transactions.size());
        out.put("minSupport", minSupport);
        out.put("minConfidence", minConfidence);

        if (transactions.isEmpty()) {
            out.put("frequentItemsets", List.of());
            out.put("rules", List.of());
            return out;
        }

        List<Set<String>> frequent = new ArrayList<>();
        Map<String, Integer> supportCounts = new HashMap<>();
        int minCount = Math.max(1, (int) Math.ceil(minSupport * transactions.size()));

        // 1-itemsets
        Set<String> items = new TreeSet<>();
        for (Set<String> t : transactions) items.addAll(t);
        List<Set<String>> level = new ArrayList<>();
        for (String item : items) {
            Set<String> one = new TreeSet<>(); one.add(item);
            int count = supportCount(one, transactions);
            if (count >= minCount) { level.add(one); supportCounts.put(key(one), count); }
        }
        frequent.addAll(level);

        // 2- and 3-itemsets are enough for a compact, explainable hospital dashboard.
        for (int size = 2; size <= 3 && !level.isEmpty(); size++) {
            Set<Set<String>> candidates = new TreeSet<>(Comparator.comparing(this::key));
            for (int i = 0; i < level.size(); i++) {
                for (int j = i + 1; j < level.size(); j++) {
                    Set<String> candidate = new TreeSet<>(level.get(i));
                    candidate.addAll(level.get(j));
                    if (candidate.size() == size) candidates.add(candidate);
                }
            }
            List<Set<String>> next = new ArrayList<>();
            for (Set<String> c : candidates) {
                int count = supportCount(c, transactions);
                if (count >= minCount) {
                    next.add(c);
                    supportCounts.put(key(c), count);
                }
            }
            frequent.addAll(next);
            level = next;
        }

        List<Map<String, Object>> frequentOutput = new ArrayList<>();
        for (Set<String> set : frequent) {
            Map<String, Object> item = new LinkedHashMap<>();
            int count = supportCounts.getOrDefault(key(set), 0);
            item.put("items", new ArrayList<>(set));
            item.put("support", round((double) count / transactions.size() * 100.0));
            item.put("count", count);
            frequentOutput.add(item);
        }
        frequentOutput.sort((a,b) -> Double.compare(((Number)b.get("support")).doubleValue(), ((Number)a.get("support")).doubleValue()));

        List<Map<String, Object>> rules = new ArrayList<>();
        for (Set<String> set : frequent) {
            if (set.size() < 2) continue;
            List<String> list = new ArrayList<>(set);
            int n = list.size();
            for (int mask = 1; mask < (1 << n) - 1; mask++) {
                Set<String> lhs = new TreeSet<>();
                Set<String> rhs = new TreeSet<>();
                for (int i = 0; i < n; i++) {
                    if ((mask & (1 << i)) != 0) lhs.add(list.get(i)); else rhs.add(list.get(i));
                }
                int unionCount = supportCounts.getOrDefault(key(set), supportCount(set, transactions));
                int lhsCount = supportCounts.getOrDefault(key(lhs), supportCount(lhs, transactions));
                if (lhsCount == 0) continue;
                double confidence = (double) unionCount / lhsCount;
                if (confidence >= minConfidence) {
                    Map<String, Object> rule = new LinkedHashMap<>();
                    rule.put("antecedent", new ArrayList<>(lhs));
                    rule.put("consequent", new ArrayList<>(rhs));
                    rule.put("support", round((double) unionCount / transactions.size() * 100.0));
                    rule.put("confidence", round(confidence * 100.0));
                    rules.add(rule);
                }
            }
        }
        rules.sort((a,b) -> {
            int c = Double.compare(((Number)b.get("confidence")).doubleValue(), ((Number)a.get("confidence")).doubleValue());
            if (c != 0) return c;
            return Double.compare(((Number)b.get("support")).doubleValue(), ((Number)a.get("support")).doubleValue());
        });
        if (rules.size() > 12) rules = new ArrayList<>(rules.subList(0, 12));

        out.put("frequentItemsets", frequentOutput.subList(0, Math.min(12, frequentOutput.size())));
        out.put("rules", rules);
        return out;
    }

    private int supportCount(Set<String> itemset, List<Set<String>> transactions) {
        int count = 0;
        for (Set<String> t : transactions) if (t.containsAll(itemset)) count++;
        return count;
    }

    private String key(Set<String> set) { return String.join("|", set); }

    private String waitBucket(double wait) {
        if (wait >= 20) return "HIGH";
        if (wait >= 10) return "MEDIUM";
        return "LOW";
    }

    private String hourBucket(int hour) {
        if (hour < 8) return "EARLY";
        if (hour < 12) return "MORNING";
        if (hour < 17) return "AFTERNOON";
        if (hour < 21) return "EVENING";
        return "NIGHT";
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
