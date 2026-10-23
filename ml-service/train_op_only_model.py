import os
import hashlib
import joblib
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
    confusion_matrix,
    classification_report
)

# ============================================================
# CONFIGURATION
# ============================================================

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD")

DB_NAME = "hospital_research_db"

# Save this experiment separately.
# Existing models will not be intentionally overwritten.
EXPERIMENTAL_MODEL_FILE = "hospital_op_hour_date_experimental.pkl"
EXPERIMENTAL_RESULTS_FILE = "op_hour_date_experimental_results.csv"

HIGH_WAIT_THRESHOLD_MINUTES = 20
RANDOM_STATE = 42

# Fixed reference date keeps the date feature consistent.
DATE_REFERENCE = pd.Timestamp("2026-01-01")


# ============================================================
# DATABASE CONNECTION
# ============================================================

def load_research_data():
    if not DB_PASSWORD:
        raise ValueError(
            "DB_PASSWORD is not set. Set it in your terminal before running."
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
        raise ValueError("No valid OP workflow records were found.")

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

    # Features available from the queue-entry timestamp.
    data["queue_hour"] = (
        data["queue_entry_time"].dt.hour.astype(str)
    )

    data["queue_day"] = (
        data["queue_entry_time"].dt.day_name()
    )

    # Fixed-reference date feature.
    # This is an experiment, not yet a production feature.
    data["queue_date_ordinal"] = (
        data["queue_entry_time"].dt.normalize() - DATE_REFERENCE
    ).dt.days

    # Target: observed waiting time >= 20 minutes.
    data["high_wait"] = (
        data["waiting_time"] >= HIGH_WAIT_THRESHOLD_MINUTES
    ).astype(int)

    return data


# ============================================================
# CHRONOLOGICAL TRAIN/TEST SPLIT
# ============================================================

def chronological_split(data):
    dates = sorted(data["queue_entry_time"].dt.date.unique())

    if len(dates) < 2:
        raise ValueError(
            "At least two distinct dates are required for a chronological split."
        )

    split_index = int(len(dates) * 0.80)
    split_index = max(1, min(split_index, len(dates) - 1))

    training_dates = set(dates[:split_index])
    testing_dates = set(dates[split_index:])

    record_dates = data["queue_entry_time"].dt.date

    train_data = data[record_dates.isin(training_dates)].copy()
    test_data = data[record_dates.isin(testing_dates)].copy()

    if train_data.empty or test_data.empty:
        raise ValueError(
            "The chronological split produced an empty dataset."
        )

    print("\n--- CHRONOLOGICAL DATA SPLIT ---")
    print(f"Total valid OP records: {len(data)}")
    print(f"Training records: {len(train_data)}")
    print(f"Testing records: {len(test_data)}")
    print(
        f"Training dates: {min(training_dates)} to {max(training_dates)}"
    )
    print(
        f"Testing dates: {min(testing_dates)} to {max(testing_dates)}"
    )

    return train_data, test_data


# ============================================================
# MODEL CREATION
# ============================================================

def build_model(feature_columns):
    categorical_columns = [
        column
        for column in feature_columns
        if column != "queue_date_ordinal"
    ]

    numeric_columns = [
        column
        for column in feature_columns
        if column == "queue_date_ordinal"
    ]

    transformers = []

    if categorical_columns:
        categorical_transformer = Pipeline(
            steps=[
                ("imputer", SimpleImputer(strategy="most_frequent")),
                (
                    "encoder",
                    OneHotEncoder(handle_unknown="ignore")
                )
            ]
        )

        transformers.append(
            (
                "categorical",
                categorical_transformer,
                categorical_columns
            )
        )

    if numeric_columns:
        numeric_transformer = Pipeline(
            steps=[
                ("imputer", SimpleImputer(strategy="median"))
            ]
        )

        transformers.append(
            (
                "numeric",
                numeric_transformer,
                numeric_columns
            )
        )

    preprocessor = ColumnTransformer(
        transformers=transformers
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
# EVALUATION
# ============================================================

def evaluate_model(model_name, model, X_test, y_test):
    predictions = model.predict(X_test)

    results = {
        "model": model_name,
        "accuracy": accuracy_score(y_test, predictions),
        "balanced_accuracy": balanced_accuracy_score(
            y_test, predictions
        ),
        "precision": precision_score(
            y_test, predictions, zero_division=0
        ),
        "recall": recall_score(
            y_test, predictions, zero_division=0
        ),
        "f1_score": f1_score(
            y_test, predictions, zero_division=0
        )
    }

    tn, fp, fn, tp = confusion_matrix(
        y_test, predictions, labels=[0, 1]
    ).ravel()

    results["false_alarms"] = int(fp)
    results["missed_high_wait"] = int(fn)
    results["detected_high_wait"] = int(tp)
    results["true_negatives"] = int(tn)

    print(f"\n========== {model_name} ==========")

    for metric, value in results.items():
        if metric == "model":
            continue

        if isinstance(value, float):
            print(f"{metric}: {value:.4f}")
        else:
            print(f"{metric}: {value}")

    print("\nClassification report:")
    print(
        classification_report(
            y_test,
            predictions,
            labels=[0, 1],
            target_names=["Normal wait", "High wait"],
            zero_division=0
        )
    )

    print("Confusion matrix [[TN, FP], [FN, TP]]:")
    print([[int(tn), int(fp)], [int(fn), int(tp)]])

    return results


# ============================================================
# MAIN
# ============================================================

def main():
    print("Loading OP research data...")

    data = load_research_data()

    print(f"\nValid OP records: {len(data)}")
    print(
        f"High-wait threshold: {HIGH_WAIT_THRESHOLD_MINUTES} minutes"
    )
    print(f"High-wait cases: {int(data['high_wait'].sum())}")
    print(
        f"High-wait rate: {data['high_wait'].mean() * 100:.2f}%"
    )

    train_data, test_data = chronological_split(data)

    y_train = train_data["high_wait"]
    y_test = test_data["high_wait"]

    # --------------------------------------------------------
    # MODEL A: QUEUE HOUR ONLY
    # --------------------------------------------------------

    hour_features = ["queue_hour"]

    hour_model = build_model(hour_features)

    hour_model.fit(
        train_data[hour_features],
        y_train
    )

    hour_results = evaluate_model(
        "QUEUE HOUR ONLY",
        hour_model,
        test_data[hour_features],
        y_test
    )

    # --------------------------------------------------------
    # MODEL B: QUEUE HOUR + DAY OF WEEK
    # --------------------------------------------------------

    hour_day_features = ["queue_hour", "queue_day"]

    hour_day_model = build_model(hour_day_features)

    hour_day_model.fit(
        train_data[hour_day_features],
        y_train
    )

    hour_day_results = evaluate_model(
        "QUEUE HOUR + DAY OF WEEK",
        hour_day_model,
        test_data[hour_day_features],
        y_test
    )

    # --------------------------------------------------------
    # MODEL C: QUEUE HOUR + DATE TREND
    # --------------------------------------------------------

    hour_date_features = [
        "queue_hour",
        "queue_date_ordinal"
    ]

    hour_date_model = build_model(hour_date_features)

    hour_date_model.fit(
        train_data[hour_date_features],
        y_train
    )

    hour_date_results = evaluate_model(
        "QUEUE HOUR + DATE TREND",
        hour_date_model,
        test_data[hour_date_features],
        y_test
    )

    # --------------------------------------------------------
    # COMPARE ALL MODELS
    # --------------------------------------------------------

    comparison = pd.DataFrame(
        [
            hour_results,
            hour_day_results,
            hour_date_results
        ]
    )

    comparison.to_csv(
        EXPERIMENTAL_RESULTS_FILE,
        index=False
    )

    print("\n========== MODEL COMPARISON ==========")

    columns_to_show = [
        "model",
        "accuracy",
        "balanced_accuracy",
        "precision",
        "recall",
        "f1_score",
        "false_alarms",
        "missed_high_wait",
        "detected_high_wait"
    ]

    print(
        comparison[columns_to_show].to_string(index=False)
    )

    # --------------------------------------------------------
    # SAVE ONLY THE NEW EXPERIMENTAL MODEL
    # --------------------------------------------------------

    joblib.dump(
        hour_date_model,
        EXPERIMENTAL_MODEL_FILE
    )

    with open(EXPERIMENTAL_MODEL_FILE, "rb") as model_file:
        model_hash = hashlib.sha256(
            model_file.read()
        ).hexdigest()

    print("\nExperimental model saved as:")
    print(EXPERIMENTAL_MODEL_FILE)

    print("\nComparison results saved as:")
    print(EXPERIMENTAL_RESULTS_FILE)

    print("\nExperimental model SHA-256:")
    print(model_hash)

    print("\nExisting model files were not intentionally modified.")
    print("The experimental model has not been deployed to the application.")
    print(
        "\nNote: Date-trend features may not generalize to future dates. "
        "Evaluate the test metrics before considering deployment."
    )


if __name__ == "__main__":
    main()