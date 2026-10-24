import math
from datetime import datetime
from pathlib import Path
from statistics import median

import pandas as pd


# --------------------------------------------------
# Configuration
# --------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent

INPUT_FILE = BASE_DIR / "des_workflow_events_v3_clean.csv"
OUTPUT_FILE = BASE_DIR / "eabda_v3_daily_results.csv"

TARGET_STAGE = "OP"

HIGH_WAIT_THRESHOLD_MINUTES = 20.0
REQUIRED_PERSISTENT_PERIODS = 3
BASELINE_PERCENT = 70

MAX_GAP_DAYS = 3
TREND_THRESHOLD = 0.5


# --------------------------------------------------
# Statistical helpers
# --------------------------------------------------

def percentile(values, percent):
    """Linear-interpolation percentile, matching the
    commonly used percentile calculation."""
    if not values:
        return 0.0

    values = sorted(float(value) for value in values)

    if len(values) == 1:
        return values[0]

    position = (len(values) - 1) * percent / 100.0
    lower_index = math.floor(position)
    upper_index = math.ceil(position)

    if lower_index == upper_index:
        return values[lower_index]

    fraction = position - lower_index

    return (
        values[lower_index] * (1.0 - fraction)
        + values[upper_index] * fraction
    )


def calculate_slope(values):
    """Least-squares slope using the last three observations."""
    values = values[-3:]

    if len(values) < 2:
        return 0.0

    x_values = list(range(len(values)))
    x_mean = sum(x_values) / len(x_values)
    y_mean = sum(values) / len(values)

    numerator = sum(
        (x - x_mean) * (y - y_mean)
        for x, y in zip(x_values, values)
    )

    denominator = sum(
        (x - x_mean) ** 2
        for x in x_values
    )

    if denominator == 0:
        return 0.0

    return numerator / denominator


def positive_robust_z(waiting, baseline, mad):
    """Same positive-only robust score used by EABDA."""
    if waiting <= baseline:
        return 0.0

    if mad == 0:
        return 1.0

    return (waiting - baseline) / (1.4826 * mad)


# --------------------------------------------------
# Load V3 workflow events
# --------------------------------------------------

if not INPUT_FILE.exists():
    raise FileNotFoundError(
        f"\nV3 input file not found:\n{INPUT_FILE}\n"
        "Check that the cleaned V3 CSV is in the same folder "
        "as this script."
    )

df = pd.read_csv(
    INPUT_FILE,
    dtype=str,
    keep_default_na=False
)

required_columns = {
    "stage",
    "queue_entry_time",
    "service_start_time",
}

missing_columns = required_columns - set(df.columns)

if missing_columns:
    raise ValueError(
        f"Missing required CSV columns: {sorted(missing_columns)}"
    )

# Select OP events only.
stage_values = df["stage"].str.strip().str.upper()

op = df.loc[stage_values == TARGET_STAGE].copy()

if op.empty:
    raise ValueError("No OP events were found in the V3 CSV.")

# Parse timestamps.
op["queue_entry_time"] = pd.to_datetime(
    op["queue_entry_time"],
    errors="coerce"
)

op["service_start_time"] = pd.to_datetime(
    op["service_start_time"],
    errors="coerce"
)

# Match Java EabdaDiagnosisService:
# ignore events with missing timestamps.
op = op.dropna(
    subset=["queue_entry_time", "service_start_time"]
).copy()

# Java uses ChronoUnit.SECONDS.between(), which truncates
# fractional seconds before converting to minutes.
seconds = (
    op["service_start_time"] - op["queue_entry_time"]
).dt.total_seconds().apply(math.trunc)

# Ignore negative waiting times.
op = op.loc[seconds >= 0].copy()
seconds = seconds.loc[op.index]

op["waiting_minutes_java_style"] = seconds / 60.0

# Group by queue-entry date and calculate the median wait
# for each day, as the Java service does.
op["date"] = op["queue_entry_time"].dt.date

daily = (
    op.groupby("date")["waiting_minutes_java_style"]
    .agg(["median", "count"])
    .reset_index()
    .sort_values("date")
    .reset_index(drop=True)
)

daily.rename(
    columns={
        "median": "daily_median_wait_minutes",
        "count": "valid_op_events",
    },
    inplace=True
)

waiting_values = daily[
    "daily_median_wait_minutes"
].astype(float).tolist()

number_of_days = len(waiting_values)

if number_of_days == 0:
    raise ValueError("No valid daily OP waiting-time observations.")

# --------------------------------------------------
# EABDA baseline and MAD
# --------------------------------------------------

baseline_end = max(
    1,
    math.floor(number_of_days * BASELINE_PERCENT / 100)
)

baseline_values = waiting_values[:baseline_end]

baseline = median(baseline_values)

absolute_deviations = [
    abs(value - baseline)
    for value in baseline_values
]

mad = median(absolute_deviations)

# Calculate the abnormality threshold using positive robust
# scores from baseline observations.
baseline_scores = [
    positive_robust_z(value, baseline, mad)
    for value in baseline_values
]

positive_scores = [
    score for score in baseline_scores
    if score > 0
]

if not positive_scores:
    abnormal_threshold = 0.0 if mad == 0 else 0.5
else:
    abnormal_threshold = max(
        0.5,
        percentile(positive_scores, 95)
    )

# --------------------------------------------------
# Diagnose the days after the baseline
# --------------------------------------------------

results = []

abnormal_counter = 0
high_wait_counter = 0
recovery_counter = 0

previous_date = None
previous_state = "NORMAL"

for index, row in daily.iterrows():

    current_date = row["date"]
    waiting = float(row["daily_median_wait_minutes"])

    rz = positive_robust_z(
        waiting,
        baseline,
        mad
    )

    abnormal = (
        rz >= abnormal_threshold
        and waiting > baseline
    )

    high_wait = (
        waiting >= HIGH_WAIT_THRESHOLD_MINUTES
    )

    # Calculate trend using up to the last three daily
    # median observations.
    start_index = max(0, index - 2)
    recent_values = waiting_values[start_index:index + 1]

    slope = calculate_slope(recent_values)

    if slope > TREND_THRESHOLD:
        trend = "INCREASING"
    elif slope < -TREND_THRESHOLD:
        trend = "DECREASING"
    else:
        trend = "STABLE"

    # Reset consecutive counters if dates have a gap
    # greater than three days.
    if previous_date is not None:
        gap_days = (current_date - previous_date).days

        if gap_days > MAX_GAP_DAYS:
            abnormal_counter = 0
            high_wait_counter = 0

    if abnormal:
        abnormal_counter += 1
    else:
        abnormal_counter = 0

    if high_wait:
        high_wait_counter += 1
    else:
        high_wait_counter = 0

    persistent = (
        abnormal_counter >= REQUIRED_PERSISTENT_PERIODS
        or high_wait_counter >= REQUIRED_PERSISTENT_PERIODS
    )

    # During the baseline period, the Java implementation
    # does not run the state-transition loop.
    if index < baseline_end:
        state = "NORMAL"
        evidence = "Baseline observation"
        persistence = 0

    else:
        if (
            previous_state == "PERSISTENT"
            and trend == "DECREASING"
            and waiting > baseline
            and not persistent
        ):
            recovery_counter += 1

            if recovery_counter >= 2:
                state = "RECOVERING"
            else:
                state = "PERSISTENT"

        elif persistent:
            state = "PERSISTENT"
            recovery_counter = 0

        elif abnormal and trend == "INCREASING":
            state = "EMERGING"
            recovery_counter = 0

        elif abnormal or high_wait:
            state = "ACTIVE"
            recovery_counter = 0

        else:
            state = "NORMAL"
            recovery_counter = 0

        if high_wait:
            evidence = (
                f"Daily median waiting time is {waiting:.2f} minutes, "
                f"at or above the {HIGH_WAIT_THRESHOLD_MINUTES:.0f}-minute "
                "high-wait threshold."
            )
        elif abnormal:
            evidence = (
                f"Positive robust score {rz:.2f} meets or exceeds "
                f"the abnormality threshold {abnormal_threshold:.2f}."
            )
        else:
            evidence = (
                "Waiting time is below the high-wait threshold "
                "and is not abnormal against the baseline."
            )

        persistence = max(
            abnormal_counter,
            high_wait_counter
        )

    results.append({
        "date": current_date,
        "valid_op_events": int(row["valid_op_events"]),
        "daily_median_wait_minutes": round(waiting, 4),
        "baseline_minutes": round(baseline, 4),
        "mad_minutes": round(mad, 4),
        "positive_robust_z": round(rz, 4),
        "abnormal": abnormal,
        "high_wait": high_wait,
        "abnormal_consecutive_days": abnormal_counter,
        "high_wait_consecutive_days": high_wait_counter,
        "trend_slope": round(slope, 4),
        "trend": trend,
        "state": state,
        "persistence": persistence,
        "evidence": evidence,
    })

    previous_date = current_date
    previous_state = state


# --------------------------------------------------
# Save a separate daily results file
# --------------------------------------------------

results_df = pd.DataFrame(results)

results_df.to_csv(
    OUTPUT_FILE,
    index=False
)

# --------------------------------------------------
# Print final EABDA result
# --------------------------------------------------

latest = results_df.iloc[-1]

latest_wait = float(
    latest["daily_median_wait_minutes"]
)

if baseline > 0:
    pressure = max(
        0.0,
        (latest_wait - baseline) / baseline
    )
else:
    pressure = 1.0 if latest_wait > 0 else 0.0

print("\n" + "=" * 55)
print("EABDA V3 — OP BOTTLENECK DIAGNOSIS")
print("=" * 55)

print(f"Input file              : {INPUT_FILE.name}")
print(f"Valid OP events         : {len(op)}")
print(f"Daily observations      : {number_of_days}")
print(f"Baseline observations   : {baseline_end}")
print(f"Baseline median         : {baseline:.2f} minutes")
print(f"MAD                     : {mad:.4f}")
print(f"Abnormality threshold   : {abnormal_threshold:.4f}")
print("-" * 55)
print(f"Latest date             : {latest['date']}")
print(f"Latest daily median     : {latest_wait:.2f} minutes")
print(f"Latest robust Z-score   : {latest['positive_robust_z']:.4f}")
print(f"Latest trend            : {latest['trend']}")
print(f"Latest state            : {latest['state']}")
print(f"Latest pressure         : {pressure:.4f}")
print(f"Persistence count       : {latest['persistence']}")
print(f"Evidence                : {latest['evidence']}")
print("-" * 55)
print(f"Daily results saved to  : {OUTPUT_FILE.name}")
print("=" * 55)