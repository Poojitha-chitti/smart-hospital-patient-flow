import os
import hashlib
import joblib
import pandas as pd
import mysql.connector

from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.ensemble import RandomForestClassifier
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score, balanced_accuracy_score

# --------------------------------------------------
# 1. Load the research database
# --------------------------------------------------

db_password = os.environ.get("DB_PASSWORD")

if not db_password:
    raise ValueError(
        "DB_PASSWORD is not set. Set it in this PowerShell session first."
    )

conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password=db_password,
    database="hospital_research_db"
)

query = """
SELECT
    event_id,
    stage,
    queue_entry_time,
    service_start_time
FROM workflow_events
WHERE stage = 'OP'
  AND queue_entry_time IS NOT NULL
  AND service_start_time IS NOT NULL
ORDER BY queue_entry_time, event_id
"""

df = pd.read_sql(query, conn)
conn.close()

# --------------------------------------------------
# 2. Prepare the data
# --------------------------------------------------

df["queue_entry_time"] = pd.to_datetime(df["queue_entry_time"])
df["service_start_time"] = pd.to_datetime(df["service_start_time"])

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

# --------------------------------------------------
# 3. Recreate the chronological split
# --------------------------------------------------

unique_dates = sorted(df["event_date"].unique())

if len(unique_dates) < 2:
    raise ValueError("Not enough dates to create a train/test split.")

split_index = int(len(unique_dates) * 0.80)
split_index = max(1, min(split_index, len(unique_dates) - 1))

train_dates = set(unique_dates[:split_index])
test_dates = set(unique_dates[split_index:])

train_df = df[df["event_date"].isin(train_dates)].copy()
test_df = df[df["event_date"].isin(test_dates)].copy()

X_train = train_df[["queue_hour"]]
y_train = train_df["high_wait"]

X_test = test_df[["queue_hour"]]
y_test = test_df["high_wait"]

print("\n--- DATA SPLIT ---")
print("Total valid OP records:", len(df))
print("Training records:", len(train_df))
print("Testing records:", len(test_df))
print("Training dates:", train_df["event_date"].min(), "to", train_df["event_date"].max())
print("Testing dates:", test_df["event_date"].min(), "to", test_df["event_date"].max())

# --------------------------------------------------
# 4. Load the previously saved research model
# --------------------------------------------------

model_path = "hospital_op_only_research_model.pkl"
saved_model = joblib.load(model_path)

saved_predictions = saved_model.predict(X_test)
saved_probabilities = saved_model.predict_proba(X_test)

# --------------------------------------------------
# 5. Retrain a separate model using the same settings
# --------------------------------------------------

fresh_model = Pipeline([
    (
        "preprocessor",
        ColumnTransformer([
            (
                "numeric",
                SimpleImputer(strategy="median"),
                ["queue_hour"]
            )
        ])
    ),
    (
        "classifier",
        RandomForestClassifier(
            n_estimators=200,
            random_state=42,
            class_weight="balanced"
        )
    )
])

fresh_model.fit(X_train, y_train)

fresh_predictions = fresh_model.predict(X_test)
fresh_probabilities = fresh_model.predict_proba(X_test)

# --------------------------------------------------
# 6. Compare predictions
# --------------------------------------------------

print("\n--- REPRODUCIBILITY RESULTS ---")

print(
    "Saved model accuracy:",
    round(accuracy_score(y_test, saved_predictions), 4)
)

print(
    "Fresh model accuracy:",
    round(accuracy_score(y_test, fresh_predictions), 4)
)

print(
    "Saved model balanced accuracy:",
    round(balanced_accuracy_score(y_test, saved_predictions), 4)
)

print(
    "Fresh model balanced accuracy:",
    round(balanced_accuracy_score(y_test, fresh_predictions), 4)
)

print(
    "Identical class predictions:",
    (saved_predictions == fresh_predictions).all()
)

print(
    "Identical predicted probabilities:",
    (saved_probabilities == fresh_probabilities).all()
)

# --------------------------------------------------
# 7. Report model file fingerprint
# --------------------------------------------------

with open(model_path, "rb") as model_file:
    fingerprint = hashlib.sha256(model_file.read()).hexdigest()

print("\nSaved model SHA-256:", fingerprint)
print("\nAudit completed. No existing model file was overwritten.")