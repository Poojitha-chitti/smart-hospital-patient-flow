import os
import mysql.connector
import pandas as pd

# 1. Read OP records from the research database
connection = mysql.connector.connect(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", "3306")),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASSWORD", ""),
    database="hospital_research_db",
)

try:
    query = """
        SELECT
            event_id,
            queue_entry_time,
            service_start_time
        FROM workflow_events
        WHERE stage = 'OP'
          AND queue_entry_time IS NOT NULL
          AND service_start_time IS NOT NULL
        ORDER BY queue_entry_time, event_id
    """
    df = pd.read_sql(query, connection)
finally:
    connection.close()

# 2. Calculate waiting time and target
df["queue_entry_time"] = pd.to_datetime(
    df["queue_entry_time"], errors="coerce"
)
df["service_start_time"] = pd.to_datetime(
    df["service_start_time"], errors="coerce"
)

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

# 3. Recreate the chronological split
dates = sorted(df["event_date"].unique())

if len(dates) < 2:
    raise ValueError("Not enough dates for a chronological split.")

split_index = int(len(dates) * 0.80)
split_index = max(1, min(split_index, len(dates) - 1))

train_dates = set(dates[:split_index])
test_dates = set(dates[split_index:])

df["period"] = df["event_date"].apply(
    lambda date: "TRAIN" if date in train_dates else "TEST"
)

# 4. Check basic data quality
print("\n========== DATA QUALITY ==========")
print("Valid OP records:", len(df))
print("Duplicate event IDs:", df["event_id"].duplicated().sum())
print("Training records:", (df["period"] == "TRAIN").sum())
print("Testing records:", (df["period"] == "TEST").sum())
print("Training dates:", min(train_dates), "to", max(train_dates))
print("Testing dates:", min(test_dates), "to", max(test_dates))

# 5. Check waiting-time patterns by hour and period
summary = (
    df.groupby(["period", "queue_hour"])
    .agg(
        records=("high_wait", "size"),
        high_wait_cases=("high_wait", "sum"),
        high_wait_rate=("high_wait", "mean"),
        average_wait=("waiting_time", "mean"),
    )
    .round(3)
)

print("\n========== WAITING-TIME PATTERNS ==========")
print(summary.to_string())

# 6. Check whether the same hours behave differently over time
print("\n========== TRAIN VS TEST HIGH-WAIT RATES ==========")

rates = df.pivot_table(
    index="queue_hour",
    columns="period",
    values="high_wait",
    aggfunc="mean",
)

print((rates * 100).round(2).to_string())

print("\nCheck completed. No database or model files were modified.")