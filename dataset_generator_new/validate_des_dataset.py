from pathlib import Path
import pandas as pd

folder = Path(__file__).parent

visits = pd.read_csv(folder / "des_visits.csv")
events = pd.read_csv(folder / "des_workflow_events.csv")

for df, columns in [
    (visits, ["visit_id", "arrival_time"]),
    (
        events,
        [
            "visit_id",
            "stage",
            "queue_entry_time",
            "service_start_time",
            "service_end_time",
        ],
    ),
]:
    missing = set(columns) - set(df.columns)
    if missing:
        raise ValueError(f"Missing required columns: {sorted(missing)}")

for col in ["arrival_time"]:
    visits[col] = pd.to_datetime(visits[col], errors="coerce")

for col in [
    "queue_entry_time",
    "service_start_time",
    "service_end_time",
]:
    events[col] = pd.to_datetime(events[col], errors="coerce")

events["waiting_minutes"] = (
    events["service_start_time"] - events["queue_entry_time"]
).dt.total_seconds() / 60

events["service_minutes"] = (
    events["service_end_time"] - events["service_start_time"]
).dt.total_seconds() / 60

print("=== DATASET VALIDATION ===")
print("Visits:", len(visits))
print("Workflow events:", len(events))
print("Duplicate visit IDs:", visits["visit_id"].duplicated().sum())
print("Duplicate event rows:", events.duplicated().sum())
print("Missing visit timestamps:", visits["arrival_time"].isna().sum())
print("Missing workflow timestamps:", events[
    [
        "queue_entry_time",
        "service_start_time",
        "service_end_time",
    ]
].isna().any(axis=1).sum())
print("Negative waiting times:", (events["waiting_minutes"] < 0).sum())
print("Negative service times:", (events["service_minutes"] < 0).sum())
print("Events referencing unknown visits:", (
    ~events["visit_id"].isin(visits["visit_id"])
).sum())

print("\n=== HIGH-WAIT RATE BY STAGE ===")
print(
    events.groupby("stage")["waiting_minutes"]
    .agg(
        total_events="count",
        average_wait="mean",
        median_wait="median",
        maximum_wait="max",
        high_wait_cases=lambda x: (x >= 20).sum(),
    )
    .round(2)
)

print("\nValidation completed. No files or database records were modified.")