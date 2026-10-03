import os
import joblib
import pandas as pd
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "hospital_wait_model.pkl")

model = joblib.load(MODEL_PATH)


@app.route("/predict", methods=["POST"])
def predict():
    try:
        data = request.get_json()

        required_fields = [
            "stage",
            "patient_type",
            "staff_available",
            "staff_capacity",
            "resource_capacity",
            "resource_available",
        ]

        missing = [field for field in required_fields if field not in data]
        if missing:
            return jsonify({
                "error": "Missing input fields",
                "missing_fields": missing
            }), 400

        input_data = pd.DataFrame([{
            "stage": data["stage"],
            "patient_type": data["patient_type"],
            "staff_available": float(data["staff_available"]),
            "staff_capacity": float(data["staff_capacity"]),
            "resource_capacity": float(data["resource_capacity"]),
            "resource_available": float(data["resource_available"]),
        }], columns=required_fields)

        prediction = int(model.predict(input_data)[0])

        condition = (
            "HIGH WAITING CONDITION"
            if prediction == 1
            else "NORMAL WAITING CONDITION"
        )

        return jsonify({
            "prediction": prediction,
            "condition": condition
        })

    except Exception as error:
        return jsonify({"error": str(error)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)