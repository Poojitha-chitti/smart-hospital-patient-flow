import pandas as pd
from pathlib import Path

folder = Path(__file__).parent

events = pd.read_csv(
    folder / "des_workflow_events.csv"
)

events["queue_entry_time"] = pd.to_datetime(
    events["queue_entry_time"]
)

events["hour"] = events["queue_entry_time"].dt.hour

stages = ["OP", "DIAGNOSTICS", "PHARMACY"]

events = events[
    events["stage"].str.upper().isin(stages)
].copy()

print("\nWAITING TIME BY STAGE AND HOUR")
print("=" * 65)

summary = events.groupby(
    [events["stage"].str.upper(), "hour"]
).agg(
    event_count=("waiting_time", "count"),
    average_wait=("waiting_time", "mean"),
    waits_over_20=("waiting_time", lambda x: (x >= 20).sum())
).round(2)

print(summary.to_string())

print("\nAnalysis completed. No files or databases were modified.")