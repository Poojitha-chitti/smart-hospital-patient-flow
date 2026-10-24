import os
from pathlib import Path

import joblib
import mysql.connector
import pandas as pd

from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    confusion_matrix,
    precision_score,
    recall_score,
)

# 1. Load OP research data
connection = mysql.connector.connect(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", "3306")),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASSWORD", ""),
    database="hospital_research_db",
)

try:
    query = """
        SELECT event_id, queue_entry_time, service_start_time
        FROM workflow_events
        WHERE stage = 'OP'
          AND queue_entry_time IS NOT NULL
          AND service_start_time IS NOT NULL
        ORDER BY queue_entry_time, event_id
    """
    df = pd.read_sql(query, connection)
finally:
    connection.close()

# 2. Prepare data
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

# 3. Recreate the chronological split
dates = sorted(df["event_date"].unique())

if len(dates) < 2:
    raise ValueError("Not enough dates for a chronological split.")

split_index = int(len(dates) * 0.80)
split_index = max(1, min(split_index, len(dates) - 1))

train_dates = set(dates[:split_index])
test_dates = set(dates[split_index:])

train_df = df[df["event_date"].isin(train_dates)].copy()
test_df = df[df["event_date"].isin(test_dates)].copy()

X_test = test_df[["queue_hour"]]
y_test = test_df["high_wait"]

print("Training records:", len(train_df))
print("Testing records:", len(test_df))
print("Training dates:", min(train_dates), "to", max(train_dates))
print("Testing dates:", min(test_dates), "to", max(test_dates))

# 4. Load the saved model — do not retrain it
model_path = Path(__file__).resolve().parent / (
    "hospital_op_only_research_model.pkl"
)
saved_model = joblib.load(model_path)

saved_predictions = saved_model.predict(X_test)

# 5. Build historical hourly rule from training data only
hour_rates = train_df.groupby("queue_hour")["high_wait"].mean()
risky_hours = set(hour_rates[hour_rates >= 0.50].index)

rule_predictions = (
    test_df["queue_hour"].isin(risky_hours).astype(int).to_numpy()
)

print("\nHours flagged by historical rule:", sorted(risky_hours))

# 6. Evaluate both methods on the same test records
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
    evaluate("SAVED ML MODEL", y_test, saved_predictions),
    evaluate("HISTORICAL HOURLY RULE", y_test, rule_predictions),
])

print("\n========== SAVED MODEL VS RULE ==========")
print(results.to_string(index=False))

# Save to a new CSV so existing results remain untouched
output_path = Path(__file__).resolve().parent / (
    "saved_model_vs_rule_verification.csv"
)
results.to_csv(output_path, index=False)

print("\nResults saved to:", output_path)
print("The saved model was loaded, not retrained or overwritten.")