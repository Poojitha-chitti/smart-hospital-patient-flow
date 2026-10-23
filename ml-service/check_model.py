import joblib
import pandas as pd

import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "hospital_wait_model.pkl")

model = joblib.load(MODEL_PATH)

test_data = pd.DataFrame([
    {
        "stage": "OP",
        "patient_type": "OP",
        "staff_available": 2,
        "staff_capacity": 2,
        "resource_capacity": 3,
        "resource_available": 2
    },
    {
        "stage": "PHARMACY",
        "patient_type": "OP",
        "staff_available": 2,
        "staff_capacity": 2,
        "resource_capacity": 3,
        "resource_available": 2
    },
    {
        "stage": "REGISTRATION",
        "patient_type": "OP",
        "staff_available": 2,
        "staff_capacity": 2,
        "resource_capacity": 3,
        "resource_available": 2
    }
])

predictions = model.predict(test_data)

for row, prediction in zip(test_data["stage"], predictions):
    print(f"{row}: {int(prediction)}")