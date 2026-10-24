import os
from pathlib import Path

import joblib
import mysql.connector
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.dummy import DummyClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    confusion_matrix,
    precision_score,
    recall_score,
)
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder


# ==========================================
# 1. LOAD RESEARCH DATA
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
            e.stage,
            e.queue_entry_time,
            e.service_start_time,
            v.patient_type
        FROM workflow_events e
        INNER JOIN visits v
            ON e.visit_id = v.visit_id
        WHERE e.queue_entry_time IS NOT NULL
          AND e.service_start_time IS NOT NULL
        ORDER BY e.queue_entry_time, e.event_id
    """
    df = pd.read_sql(query, connection)
finally:
    connection.close()


# ==========================================
# 2. PREPARE DATA
# ==========================================

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

if len(df) < 100:
    raise ValueError("Not enough valid records.")

df["high_wait"] = (df["waiting_time"] >= 20).astype(int)
df["queue_hour"] = df["queue_entry_time"].dt.hour
df["event_date"] = df["queue_entry_time"].dt.date
df["stage"] = df["stage"].fillna("Unknown").astype(str)

print("Valid records:", len(df))
print("High-wait records:", int(df["high_wait"].sum()))


# ==========================================
# 3. CHRONOLOGICAL TRAIN / TEST SPLIT
# ==========================================

unique_dates = sorted(df["event_date"].unique())

if len(unique_dates) < 2:
    raise ValueError("At least two dates are required.")

split_index = int(len(unique_dates) * 0.80)
split_index = max(1, min(split_index, len(unique_dates) - 1))

train_dates = set(unique_dates[:split_index])
test_dates = set(unique_dates[split_index:])

train_df = df[df["event_date"].isin(train_dates)].copy()
test_df = df[df["event_date"].isin(test_dates)].copy()

y_train = train_df["high_wait"]
y_test = test_df["high_wait"]

if y_train.nunique() < 2 or y_test.nunique() < 2:
    raise ValueError("Both train and test sets must contain both classes.")

print("Training records:", len(train_df))
print("Testing records:", len(test_df))
print(
    "Training dates:",
    train_df["event_date"].min(),
    "to",
    train_df["event_date"].max(),
)
print(
    "Testing dates:",
    test_df["event_date"].min(),
    "to",
    test_df["event_date"].max(),
)


# ==========================================
# 4. MODEL BUILDER
# ==========================================

def build_model(categorical_features, numeric_features):
    transformers = []

    if categorical_features:
        transformers.append((
            "categorical",
            Pipeline([
                ("imputer", SimpleImputer(strategy="most_frequent")),
                ("encoder", OneHotEncoder(handle_unknown="ignore")),
            ]),
            categorical_features,
        ))

    if numeric_features:
        transformers.append((
            "numeric",
            SimpleImputer(strategy="median"),
            numeric_features,
        ))

    preprocessor = ColumnTransformer(transformers=transformers)

    return Pipeline([
        ("preprocessor", preprocessor),
        ("classifier", RandomForestClassifier(
            n_estimators=200,
            random_state=42,
            class_weight="balanced",
        )),
    ])


# ==========================================
# 5. EVALUATION FUNCTION
# ==========================================

def evaluate(name, model, X_test, y_test):
    predicted = model.predict(X_test)

    tn, fp, fn, tp = confusion_matrix(
        y_test, predicted, labels=[0, 1]
    ).ravel()

    result = {
        "model": name,
        "accuracy": accuracy_score(y_test, predicted),
        "balanced_accuracy": balanced_accuracy_score(y_test, predicted),
        "precision": precision_score(
            y_test, predicted, zero_division=0
        ),
        "recall": recall_score(
            y_test, predicted, zero_division=0
        ),
        "true_negatives": int(tn),
        "false_alarms": int(fp),
        "missed_high_wait": int(fn),
        "detected_high_wait": int(tp),
    }

    print(f"\n========== {name} ==========")
    for key, value in result.items():
        if key == "model":
            continue
        if isinstance(value, float):
            print(f"{key}: {value:.4f}")
        else:
            print(f"{key}: {value}")

    return result


# ==========================================
# 6. BASELINE
# ==========================================

baseline_features = ["stage", "queue_hour"]

baseline = DummyClassifier(strategy="most_frequent")
baseline.fit(train_df[baseline_features], y_train)

baseline_result = evaluate(
    "BASELINE: MOST COMMON CLASS",
    baseline,
    test_df[baseline_features],
    y_test,
)


# ==========================================
# 7. MODEL A: STAGE ONLY
# ==========================================

stage_only = build_model(
    categorical_features=["stage"],
    numeric_features=[],
)

stage_only.fit(train_df[["stage"]], y_train)

stage_result = evaluate(
    "MODEL A: STAGE ONLY",
    stage_only,
    test_df[["stage"]],
    y_test,
)


# ==========================================
# 8. MODEL B: STAGE + HOUR
# ==========================================

time_model = build_model(
    categorical_features=["stage"],
    numeric_features=["queue_hour"],
)

time_model.fit(
    train_df[["stage", "queue_hour"]],
    y_train,
)

time_result = evaluate(
    "MODEL B: STAGE + QUEUE HOUR",
    time_model,
    test_df[["stage", "queue_hour"]],
    y_test,
)


# ==========================================
# 9. COMPARE MODELS
# ==========================================

comparison = pd.DataFrame([
    baseline_result,
    stage_result,
    time_result,
])

print("\n========== FINAL COMPARISON ==========")

print(comparison[[
    "model",
    "accuracy",
    "balanced_accuracy",
    "precision",
    "recall",
    "false_alarms",
    "missed_high_wait",
]].round(4).to_string(index=False))


# ==========================================
# 10. FALSE ALARMS BY HOUR
# ==========================================

test_predictions = test_df[
    ["event_id", "stage", "queue_hour", "waiting_time", "high_wait"]
].copy()

test_predictions["predicted_high_wait"] = time_model.predict(
    test_df[["stage", "queue_hour"]]
)

test_predictions["false_alarm"] = (
    (test_predictions["high_wait"] == 0)
    & (test_predictions["predicted_high_wait"] == 1)
).astype(int)

test_predictions["correct_high_wait"] = (
    (test_predictions["high_wait"] == 1)
    & (test_predictions["predicted_high_wait"] == 1)
).astype(int)

hourly = (
    test_predictions
    .groupby("queue_hour")
    .agg(
        total_events=("event_id", "count"),
        actual_high_wait=("high_wait", "sum"),
        predicted_high_wait=("predicted_high_wait", "sum"),
        false_alarms=("false_alarm", "sum"),
        detected_high_wait=("correct_high_wait", "sum"),
    )
    .reset_index()
)

hourly["actual_normal_wait"] = (
    hourly["total_events"] - hourly["actual_high_wait"]
)

hourly["false_alarm_rate_percent"] = (
    hourly["false_alarms"]
    / hourly["actual_normal_wait"].replace(0, pd.NA)
    * 100
).round(2)

print("\n========== MODEL B: FALSE ALARMS BY HOUR ==========")
print(hourly.to_string(index=False))
# ==========================================
# 11. FALSE ALARMS BY HOSPITAL STAGE
# ==========================================

stage_analysis = (
    test_predictions
    .groupby("stage")
    .agg(
        total_events=("event_id", "count"),
        actual_high_wait=("high_wait", "sum"),
        predicted_high_wait=("predicted_high_wait", "sum"),
        false_alarms=("false_alarm", "sum"),
        detected_high_wait=("correct_high_wait", "sum"),
    )
    .reset_index()
)

stage_analysis["actual_normal_wait"] = (
    stage_analysis["total_events"]
    - stage_analysis["actual_high_wait"]
)

stage_analysis["false_alarm_rate_percent"] = (
    stage_analysis["false_alarms"]
    / stage_analysis["actual_normal_wait"].replace(0, pd.NA)
    * 100
).round(2)

print("\n========== MODEL B: FALSE ALARMS BY STAGE ==========")
print(stage_analysis.to_string(index=False))

stage_path = (
    Path(__file__).resolve().parent
    / "research_false_alarms_by_stage.csv"
)

stage_analysis.to_csv(stage_path, index=False)

print("\nStage analysis saved to:")
print(stage_path)

# ==========================================
# 11. SAVE RESEARCH MODEL SEPARATELY
# ==========================================

folder = Path(__file__).resolve().parent

model_path = folder / "hospital_wait_research_stage_hour_comparison.pkl"
joblib.dump(time_model, model_path)

results_path = folder / "research_hourly_false_alarms.csv"
hourly.to_csv(results_path, index=False)

print("\nResearch model saved to:")
print(model_path)
print("\nHourly analysis saved to:")
print(results_path)
print("Existing hospital_wait_model.pkl was not modified.")
print("Comparison completed.")