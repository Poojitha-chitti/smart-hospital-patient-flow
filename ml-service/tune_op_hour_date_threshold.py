import os
import mysql.connector
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.pipeline import Pipeline
from sklearn.impute import SimpleImputer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix
)

# ============================================================
# CONFIGURATION
# ============================================================

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_NAME = "hospital_research_db"

HIGH_WAIT_THRESHOLD_MINUTES = 20
RANDOM_STATE = 42

# Use a fixed reference date for consistent date features.
DATE_REFERENCE = pd.Timestamp("2026-01-01")

FEATURES = ["queue_hour", "queue_date_ordinal"]

# Candidate probability thresholds.
THRESHOLDS = [0.30, 0.35, 0.40, 0.45, 0.50,
              0.55, 0.60, 0.65, 0.70]


# ============================================================
# LOAD DATA
# ============================================================

def load_data():
    if not DB_PASSWORD:
        raise ValueError(
            "DB_PASSWORD is not set. Set it in your terminal first."
        )

    connection = mysql.connector.connect(
        host=DB_HOST,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME
    )

    query = """
        SELECT
            e.event_id,
            e.visit_id,
            e.queue_entry_time,
            e.service_start_time
        FROM workflow_events e
        WHERE e.stage = 'OP'
          AND e.queue_entry_time IS NOT NULL
          AND e.service_start_time IS NOT NULL
          AND e.service_start_time >= e.queue_entry_time
        ORDER BY e.queue_entry_time, e.event_id
    """

    try:
        data = pd.read_sql_query(query, connection)
    finally:
        connection.close()

    if data.empty:
        raise ValueError("No valid OP records found.")

    data["queue_entry_time"] = pd.to_datetime(
        data["queue_entry_time"], errors="coerce"
    )

    data["service_start_time"] = pd.to_datetime(
        data["service_start_time"], errors="coerce"
    )

    data = data.dropna(
        subset=["queue_entry_time", "service_start_time"]
    ).copy()

    data["waiting_time"] = (
        data["service_start_time"] - data["queue_entry_time"]
    ).dt.total_seconds() / 60.0

    data = data[
        (data["waiting_time"] >= 0)
        & (data["waiting_time"] < float("inf"))
    ].copy()

    data["queue_hour"] = (
        data["queue_entry_time"].dt.hour.astype(str)
    )

    data["queue_date_ordinal"] = (
        data["queue_entry_time"].dt.normalize() - DATE_REFERENCE
    ).dt.days

    data["high_wait"] = (
        data["waiting_time"] >= HIGH_WAIT_THRESHOLD_MINUTES
    ).astype(int)

    return data


# ============================================================
# CHRONOLOGICAL SPLIT
# ============================================================

def chronological_split(data):
    dates = sorted(data["queue_entry_time"].dt.date.unique())

    if len(dates) < 5:
        raise ValueError(
            "Not enough distinct dates for chronological validation."
        )

    # Reserve the final 20% of dates as the untouched test period.
    test_start = int(len(dates) * 0.80)

    # Split the earlier 80% into model-fitting and validation periods.
    validation_start = int(test_start * 0.80)

    validation_start = max(1, validation_start)
    validation_start = min(validation_start, test_start - 1)

    fit_dates = set(dates[:validation_start])
    validation_dates = set(dates[validation_start:test_start])
    test_dates = set(dates[test_start:])

    record_dates = data["queue_entry_time"].dt.date

    fit_data = data[record_dates.isin(fit_dates)].copy()
    validation_data = data[
        record_dates.isin(validation_dates)
    ].copy()
    test_data = data[record_dates.isin(test_dates)].copy()

    if (
        fit_data.empty
        or validation_data.empty
        or test_data.empty
    ):
        raise ValueError("Chronological split produced an empty period.")

    print("\n========== CHRONOLOGICAL SPLIT ==========")

    for name, subset in [
        ("MODEL FIT", fit_data),
        ("VALIDATION", validation_data),
        ("FINAL TEST", test_data)
    ]:
        print(
            f"{name}: {len(subset)} records, "
            f"{subset['queue_entry_time'].min().date()} to "
            f"{subset['queue_entry_time'].max().date()}"
        )

    return fit_data, validation_data, test_data


# ============================================================
# MODEL
# ============================================================

def build_model():
    categorical = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="most_frequent")),
            ("encoder", OneHotEncoder(handle_unknown="ignore"))
        ]
    )

    numeric = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median"))
        ]
    )

    preprocessor = ColumnTransformer(
        transformers=[
            ("categorical", categorical, ["queue_hour"]),
            ("numeric", numeric, ["queue_date_ordinal"])
        ]
    )

    classifier = RandomForestClassifier(
        n_estimators=200,
        random_state=RANDOM_STATE,
        class_weight="balanced"
    )

    return Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", classifier)
        ]
    )


# ============================================================
# THRESHOLD EVALUATION
# ============================================================

def calculate_metrics(y_true, probabilities, threshold):
    predictions = (probabilities >= threshold).astype(int)

    tn, fp, fn, tp = confusion_matrix(
        y_true, predictions, labels=[0, 1]
    ).ravel()

    return {
        "threshold": threshold,
        "accuracy": accuracy_score(y_true, predictions),
        "balanced_accuracy": balanced_accuracy_score(
            y_true, predictions
        ),
        "precision": precision_score(
            y_true, predictions, zero_division=0
        ),
        "recall": recall_score(
            y_true, predictions, zero_division=0
        ),
        "f1_score": f1_score(
            y_true, predictions, zero_division=0
        ),
        "false_alarms": int(fp),
        "missed_high_wait": int(fn),
        "detected_high_wait": int(tp),
        "true_negatives": int(tn)
    }


# ============================================================
# MAIN
# ============================================================

def main():
    print("Loading OP research data...")
    data = load_data()

    print(f"Valid OP records: {len(data)}")
    print(f"High-wait cases: {int(data['high_wait'].sum())}")

    fit_data, validation_data, test_data = (
        chronological_split(data)
    )

    # --------------------------------------------------------
    # 1. Train a temporary model on the earliest period.
    # --------------------------------------------------------

    tuning_model = build_model()

    tuning_model.fit(
        fit_data[FEATURES],
        fit_data["high_wait"]
    )

    validation_probabilities = tuning_model.predict_proba(
        validation_data[FEATURES]
    )[:, 1]

    # --------------------------------------------------------
    # 2. Select threshold using validation data only.
    # --------------------------------------------------------

    validation_results = []

    for threshold in THRESHOLDS:
        result = calculate_metrics(
            validation_data["high_wait"],
            validation_probabilities,
            threshold
        )
        validation_results.append(result)

    validation_df = pd.DataFrame(validation_results)

    print("\n========== VALIDATION THRESHOLDS ==========")
    print(
        validation_df[
            [
                "threshold",
                "accuracy",
                "balanced_accuracy",
                "precision",
                "recall",
                "false_alarms",
                "missed_high_wait"
            ]
        ].to_string(index=False)
    )

    # Choose the threshold with the highest validation
    # balanced accuracy. Resolve ties using F1 score.
    best_row = validation_df.sort_values(
        ["balanced_accuracy", "f1_score", "accuracy"],
        ascending=False
    ).iloc[0]

    selected_threshold = float(best_row["threshold"])

    print("\nSelected threshold from validation:")
    print(selected_threshold)

    # --------------------------------------------------------
    # 3. Refit using all data before the final test period.
    # --------------------------------------------------------

    development_data = pd.concat(
        [fit_data, validation_data],
        ignore_index=True
    )

    final_model = build_model()

    final_model.fit(
        development_data[FEATURES],
        development_data["high_wait"]
    )

    # --------------------------------------------------------
    # 4. Evaluate once on the untouched test period.
    # --------------------------------------------------------

    test_probabilities = final_model.predict_proba(
        test_data[FEATURES]
    )[:, 1]

    test_results = calculate_metrics(
        test_data["high_wait"],
        test_probabilities,
        selected_threshold
    )

    print("\n========== FINAL TEST RESULTS ==========")

    for metric, value in test_results.items():
        if isinstance(value, float):
            print(f"{metric}: {value:.4f}")
        else:
            print(f"{metric}: {value}")

    print(
        "\nNo existing model files were loaded for deployment, "
        "overwritten, or changed by this script."
    )

    print(
        "This script only evaluates threshold tuning. "
        "It does not save or deploy a model."
    )


if __name__ == "__main__":
    main()
    