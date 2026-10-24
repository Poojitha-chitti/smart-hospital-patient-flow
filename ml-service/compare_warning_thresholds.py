import os
from pathlib import Path

import mysql.connector
import pandas as pd
import joblib

from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    confusion_matrix,
    precision_score,
    recall_score,
)

BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "hospital_op_only_research_model.pkl"

print("Loading saved OP research model...")
model = joblib.load(MODEL_PATH)

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

# Use the same chronological 80/20 date split.
dates = sorted(df["event_date"].unique())
split_index = int(len(dates) * 0.80)
split_index = max(1, min(split_index, len(dates) - 1))

test_dates = set(dates[split_index:])
test_df = df[df["event_date"].isin(test_dates)].copy()

X_test = test_df[["queue_hour"]]
y_test = test_df["high_wait"]

# Predict probabilities once; only the decision threshold changes.
positive_class_index = list(model.classes_).index(1)
probabilities = model.predict_proba(X_test)[:, positive_class_index]

results = []

for threshold in [0.30, 0.40, 0.50, 0.60, 0.70]:
    predictions = (probabilities >= threshold).astype(int)

    tn, fp, fn, tp = confusion_matrix(
        y_test, predictions, labels=[0, 1]
    ).ravel()

    results.append({
        "threshold": threshold,
        "accuracy": round(accuracy_score(y_test, predictions), 4),
        "balanced_accuracy": round(
            balanced_accuracy_score(y_test, predictions), 4
        ),
        "precision": round(
            precision_score(y_test, predictions, zero_division=0), 4
        ),
        "recall": round(
            recall_score(y_test, predictions, zero_division=0), 4
        ),
        "false_alarms": int(fp),
        "missed_high_wait": int(fn),
        "detected_high_wait": int(tp),
        "true_negatives": int(tn),
    })

results_df = pd.DataFrame(results)

print("\n========== WARNING THRESHOLD COMPARISON ==========")
print(results_df.to_string(index=False))

output_path = BASE_DIR / "op_warning_threshold_comparison.csv"
results_df.to_csv(output_path, index=False)

print("\nResults saved to:")
print(output_path)
print("\nSaved research model was not modified.")