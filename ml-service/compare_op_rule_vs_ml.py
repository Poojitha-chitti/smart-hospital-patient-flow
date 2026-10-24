import os
from pathlib import Path

import mysql.connector
import pandas as pd

from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    confusion_matrix,
    precision_score,
    recall_score,
)

# ==========================================
# 1. LOAD OP RESEARCH DATA
# ==========================================

print("Connecting to hospital_research_db...")

connection = mysql.connector.connect(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", "3306")),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASSWORD", ""),
    database="hospital_research_db",
)

try:
    query = """
        SELECT
            e.event_id,
            e.queue_entry_time,
            e.service_start_time
        FROM workflow_events e
        WHERE e.stage = 'OP'
          AND e.queue_entry_time IS NOT NULL
          AND e.service_start_time IS NOT NULL
        ORDER BY e.queue_entry_time, e.event_id
    """
    df = pd.read_sql(query, connection)
finally:
    connection.close()

df["queue_entry_time"] = pd.to_datetime(
    df["queue_entry_time"], errors="coerce"
)
df["service_start_time"] = pd.to_datetime(
    df["service_start_time"], errors="coerce"
)

df["waiting_time"] = (
    df["service_start_time"] - df["queue_entry_time"]
).dt.total_seconds() / 60

df = df[
    df["waiting_time"].notna()
    & (df["waiting_time"] >= 0)
].copy()

df["high_wait"] = (df["waiting_time"] >= 20).astype(int)
df["queue_hour"] = df["queue_entry_time"].dt.hour
df["event_date"] = df["queue_entry_time"].dt.date

# ==========================================
# 2. CHRONOLOGICAL SPLIT
# ==========================================

unique_dates = sorted(df["event_date"].unique())
split_index = int(len(unique_dates) * 0.80)
split_index = max(1, min(split_index, len(unique_dates) - 1))

train_dates = set(unique_dates[:split_index])
test_dates = set(unique_dates[split_index:])

train_df = df[df["event_date"].isin(train_dates)].copy()
test_df = df[df["event_date"].isin(test_dates)].copy()

print("Training records:", len(train_df))
print("Testing records:", len(test_df))

# ==========================================
# 3. MODEL: QUEUE HOUR ONLY
# ==========================================

ml_model = RandomForestClassifier(
    n_estimators=200,
    random_state=42,
    class_weight="balanced",
)

ml_model.fit(
    train_df[["queue_hour"]],
    train_df["high_wait"],
)

ml_predictions = (
    ml_model.predict_proba(test_df[["queue_hour"]])[:, 1] >= 0.50
).astype(int)

# ==========================================
# 4. RULE: HISTORICAL HIGH-WAIT RATE
# ==========================================

# Calculate high-wait rates using training data only.
# This prevents test outcomes from influencing the rule.

hour_rates = train_df.groupby("queue_hour")["high_wait"].mean()

# Flag an hour if at least 50% of training records
# for that hour had waiting times of 20 minutes or more.
risky_hours = set(hour_rates[hour_rates >= 0.50].index)

rule_predictions = (
    test_df["queue_hour"].isin(risky_hours)
).astype(int).to_numpy()

print("\nHours flagged by historical rule:", sorted(risky_hours))

# ==========================================
# 5. EVALUATE BOTH METHODS
# ==========================================

def evaluate(name, actual, predicted):
    tn, fp, fn, tp = confusion_matrix(
        actual, predicted, labels=[0, 1]
    ).ravel()

    return {
        "method": name,
        "accuracy": round(accuracy_score(actual, predicted), 4),
        "balanced_accuracy": round(
            balanced_accuracy_score(actual, predicted), 4
        ),
        "precision": round(
            precision_score(actual, predicted, zero_division=0), 4
        ),
        "recall": round(
            recall_score(actual, predicted, zero_division=0), 4
        ),
        "false_alarms": int(fp),
        "missed_high_wait": int(fn),
        "detected_high_wait": int(tp),
        "true_negatives": int(tn),
    }

results = pd.DataFrame([
    evaluate(
        "ML: QUEUE HOUR",
        test_df["high_wait"],
        ml_predictions,
    ),
    evaluate(
        "RULE: HISTORICAL HOURLY RATE",
        test_df["high_wait"],
        rule_predictions,
    ),
])

print("\n========== RULE VS ML COMPARISON ==========")
print(results.to_string(index=False))

# ==========================================
# 6. SAVE RESULTS
# ==========================================

output_path = (
    Path(__file__).resolve().parent
    / "op_rule_vs_ml_results.csv"
)

results.to_csv(output_path, index=False)

print("\nResults saved to:")
print(output_path)
print("\nNo production model was modified.")