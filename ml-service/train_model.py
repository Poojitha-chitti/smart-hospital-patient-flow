import os
from pathlib import Path

import joblib
import mysql.connector
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder


# ==========================================
# 1. CONNECT TO RESEARCH DATABASE
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
    # ==========================================
    # 2. LOAD REAL WORKFLOW DATA
    # ==========================================

    query = """
        SELECT
            e.event_id,
            e.visit_id,
            e.stage,
            e.queue_entry_time,
            e.service_start_time,
            e.service_end_time,
            v.patient_type
        FROM workflow_events e
        INNER JOIN visits v
            ON e.visit_id = v.visit_id
    """

    df = pd.read_sql(query, connection)

finally:
    connection.close()


# ==========================================
# 3. CHECK AND PREPARE DATA
# ==========================================

print("\nTotal records loaded:", len(df))

print("\nRecords by hospital stage:")
print(df["stage"].value_counts(dropna=False))

# Convert timestamps to datetime.
for column in [
    "queue_entry_time",
    "service_start_time",
    "service_end_time",
]:
    df[column] = pd.to_datetime(df[column], errors="coerce")

# Calculate waiting time in minutes.
df["waiting_time"] = (
    df["service_start_time"] - df["queue_entry_time"]
).dt.total_seconds() / 60

# Calculate service time in minutes.
df["service_time"] = (
    df["service_end_time"] - df["service_start_time"]
).dt.total_seconds() / 60

# Remove records with missing or invalid waiting times.
df = df[
    df["waiting_time"].notna()
    & (df["waiting_time"] >= 0)
].copy()

if df.empty:
    raise ValueError(
        "No valid waiting-time records were found. "
        "Check the workflow_events timestamps."
    )

print("\nValid workflow records:", len(df))

print("\nWaiting-time statistics:")
print(df["waiting_time"].describe())

# ==========================================
# 4. CREATE PREDICTION TARGET
# ==========================================

# 1 = high waiting time (20 minutes or more)
# 0 = normal waiting time (under 20 minutes)

df["high_wait"] = (
    df["waiting_time"] >= 20
).astype(int)

print("\nHigh-wait class distribution:")
print(df["high_wait"].value_counts())

print("\nHigh-wait percentage by stage:")
print(
    df.groupby("stage")["high_wait"]
    .agg(["count", "sum", "mean"])
    .assign(
        high_wait_percentage=lambda result:
        result["mean"] * 100
    )
)

# ==========================================
# 5. SELECT MODEL FEATURES
# ==========================================

# This baseline model uses information available
# before waiting time is observed.
#
# Do NOT include waiting_time or service_time
# as input features because they are used to
# define the prediction target.

categorical_features = [
    "stage",
    "patient_type",
]

X = df[categorical_features].copy()
y = df["high_wait"]

# Replace missing categorical values with a placeholder.
for column in categorical_features:
    X[column] = X[column].fillna("Unknown").astype(str)

# ==========================================
# 6. CHECK TARGET CLASSES
# ==========================================

class_counts = y.value_counts()

if len(class_counts) < 2:
    raise ValueError(
        "Training requires both normal-wait and "
        "high-wait records."
    )

if class_counts.min() < 2:
    raise ValueError(
        "At least two records per target class are "
        "required for a stratified train/test split."
    )

# ==========================================
# 7. PREPROCESSING
# ==========================================

preprocessor = ColumnTransformer(
    transformers=[
        (
            "categorical",
            Pipeline(
                steps=[
                    (
                        "imputer",
                        SimpleImputer(strategy="most_frequent"),
                    ),
                    (
                        "encoder",
                        OneHotEncoder(handle_unknown="ignore"),
                    ),
                ]
            ),
            categorical_features,
        )
    ]
)

# ==========================================
# 8. RANDOM FOREST MODEL
# ==========================================

random_forest = RandomForestClassifier(
    n_estimators=200,
    random_state=42,
    class_weight="balanced",
)

pipeline = Pipeline(
    steps=[
        ("preprocessor", preprocessor),
        ("model", random_forest),
    ]
)

# ==========================================
# 9. TRAIN / TEST SPLIT
# ==========================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y,
)

print("\nTraining records:", len(X_train))
print("Testing records:", len(X_test))

# ==========================================
# 10. TRAIN MODEL
# ==========================================

print("\nTraining Random Forest model...")

pipeline.fit(X_train, y_train)

# ==========================================
# 11. EVALUATE MODEL
# ==========================================

predictions = pipeline.predict(X_test)

accuracy = accuracy_score(y_test, predictions)

print("\n========== MODEL RESULTS ==========")
print(f"Accuracy: {accuracy:.4f}")
print(f"Accuracy percentage: {accuracy * 100:.2f}%")

print("\nClassification Report:")
print(
    classification_report(
        y_test,
        predictions,
        target_names=["Normal Wait", "High Wait"],
        zero_division=0,
    )
)

print("\nConfusion Matrix:")
print(confusion_matrix(y_test, predictions))

# ==========================================
# 12. SAVE MODEL IN THIS BACKUP FOLDER
# ==========================================

model_path = (
    Path(__file__).resolve().parent
    / "hospital_wait_model.pkl"
)

joblib.dump(pipeline, model_path)

print("\nModel saved successfully!")
print("Saved location:", model_path)

print("\n========== TRAINING COMPLETED ==========")