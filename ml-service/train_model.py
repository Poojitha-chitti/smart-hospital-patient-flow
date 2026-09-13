import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score, classification_report


# ==========================================
# 1. LOAD 30,000 WORKFLOW RECORDS
# ==========================================

file_path = r"C:\Users\Dell\Downloads\smart-hospital\hospital_workflow_data.csv"

df = pd.read_csv(file_path)

print("Total workflow records:", len(df))


# ==========================================
# 2. CREATE TARGET VARIABLE
# ==========================================

# Waiting time >= 20 minutes = High Waiting
# Waiting time < 20 minutes = Normal Waiting

df["high_wait"] = (df["waiting_time"] >= 20).astype(int)


# ==========================================
# 3. SELECT FEATURES
# ==========================================

X = df[
    [
        "stage",
        "patient_type",
        "staff_available",
        "staff_capacity",
        "resource_capacity",
        "resource_available"
    ]
]

y = df["high_wait"]


# ==========================================
# 4. CATEGORICAL AND NUMERICAL FEATURES
# ==========================================

categorical_features = [
    "stage",
    "patient_type"
]

numerical_features = [
    "staff_available",
    "staff_capacity",
    "resource_capacity",
    "resource_available"
]


# ==========================================
# 5. PREPROCESSING
# ==========================================

preprocessor = ColumnTransformer(
    transformers=[
        (
            "cat",
            OneHotEncoder(handle_unknown="ignore"),
            categorical_features
        ),
        (
            "num",
            "passthrough",
            numerical_features
        )
    ]
)


# ==========================================
# 6. RANDOM FOREST MODEL
# ==========================================

model = RandomForestClassifier(
    n_estimators=100,
    random_state=42
)


# ==========================================
# 7. COMPLETE ML PIPELINE
# ==========================================

pipeline = Pipeline(
    steps=[
        ("preprocessor", preprocessor),
        ("model", model)
    ]
)


# ==========================================
# 8. TRAIN / TEST SPLIT
# ==========================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y
)


print("Training records:", len(X_train))
print("Testing records:", len(X_test))


# ==========================================
# 9. TRAIN MODEL
# ==========================================

print("\nTraining Random Forest model...")

pipeline.fit(X_train, y_train)


# ==========================================
# 10. TEST MODEL
# ==========================================

y_pred = pipeline.predict(X_test)

accuracy = accuracy_score(y_test, y_pred)

print("\n=== ML MODEL RESULTS ===")
print("Accuracy:", accuracy)

print("\nClassification Report:")
print(classification_report(y_test, y_pred))


# ==========================================
# 11. SAVE COMPLETE PIPELINE
# ==========================================

model_path = r"C:\Users\Dell\Downloads\smart-hospital\ml-service\hospital_wait_model.pkl"

joblib.dump(pipeline, model_path)

print("\nModel saved successfully!")
print("Location:", model_path)


print("\n=== ML TRAINING COMPLETED ===")                                        