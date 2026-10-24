import os
import joblib
import pandas as pd

from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# ==========================================
# 1. LOAD TRAINED MODEL
# ==========================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "hospital_wait_model.pkl")

model = joblib.load(MODEL_PATH)

# These must match the features used in train_model.py.
REQUIRED_FIELDS = [
    "stage",
    "patient_type",
]


# ==========================================
# 2. HEALTH CHECK
# ==========================================

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "UP",
        "service": "Smart Hospital ML Service",
        "model_loaded": model is not None
    })


# ==========================================
# 3. PREDICT HIGH WAITING CONDITION
# ==========================================

@app.route("/predict", methods=["POST"])
def predict():
    try:
        data = request.get_json(silent=True)

        if not isinstance(data, dict):
            return jsonify({
                "error": "Please send a valid JSON object."
            }), 400

        missing = [
            field
            for field in REQUIRED_FIELDS
            if field not in data
            or data[field] is None
            or str(data[field]).strip() == ""
        ]

        if missing:
            return jsonify({
                "error": "Missing required input fields.",
                "missing_fields": missing
            }), 400

        # Prepare exactly the features expected by the model.
        input_data = pd.DataFrame([{
            "stage": str(data["stage"]),
            "patient_type": str(data["patient_type"]),
        }], columns=REQUIRED_FIELDS)

        # Predict.
        prediction = int(model.predict(input_data)[0])

        condition = (
            "HIGH WAITING CONDITION"
            if prediction == 1
            else "NORMAL WAITING CONDITION"
        )

        return jsonify({
            "prediction": prediction,
            "condition": condition,
            "stage": str(data["stage"]),
            "patient_type": str(data["patient_type"]),
            "threshold_minutes": 20,
            "model": "Random Forest",
            "note": (
                "This is a stage-and-patient-type baseline prediction, "
                "not a real-time measurement of the current queue."
            )
        })

    except Exception:
        app.logger.exception("Prediction failed.")

        return jsonify({
            "error": "Prediction failed. Check the ML service logs."
        }), 500


# ==========================================
# 4. START SERVICE
# ==========================================

if __name__ == "__main__":
    app.run(
        host="127.0.0.1",
        port=5000,
        debug=False
    )