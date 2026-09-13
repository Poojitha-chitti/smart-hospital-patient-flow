from flask import Flask, request, jsonify
import joblib
import pandas as pd

app = Flask(__name__)

# Load the complete trained ML pipeline
model = joblib.load("hospital_wait_model.pkl")


@app.route("/predict", methods=["POST"])
def predict():

    data = request.get_json()

    input_data = pd.DataFrame([{
        "stage": data["stage"],
        "patient_type": data["patient_type"],
        "staff_available": data["staff_available"],
        "staff_capacity": data["staff_capacity"],
        "resource_capacity": data["resource_capacity"],
        "resource_available": data["resource_available"]
    }])

    # The pipeline automatically performs preprocessing
    # and then makes the prediction
    prediction = int(model.predict(input_data)[0])

    if prediction == 1:
        condition = "HIGH WAITING CONDITION"
    else:
        condition = "NORMAL WAITING CONDITION"

    return jsonify({
        "prediction": prediction,
        "condition": condition
    })


if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=False
    )