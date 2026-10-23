import simpy
import pandas as pd
import numpy as np
import random

from datetime import datetime, timedelta
from pathlib import Path


# ============================================================
# SMART HOSPITAL - DES DATASET GENERATOR V3
# ============================================================
# Changes from V2:
# 1. Correctly handles hourly arrival-rate transitions.
# 2. Preserves existing patient routes and service distributions.
# 3. Saves V3 files separately from V2.
# 4. Adds data-quality validation.
#
# This script does NOT modify MySQL.
# ============================================================


# ============================================================
# CONFIGURATION
# ============================================================

NUM_VISITS = 10032

START_DATE = datetime(2026, 1, 1, 7, 0, 0)

HOSPITAL_OPEN_HOUR = 7
HOSPITAL_CLOSE_HOUR = 19

RANDOM_SEED = 42

random.seed(RANDOM_SEED)
np.random.seed(RANDOM_SEED)


# ============================================================
# RESOURCE CAPACITIES
# ============================================================

# Registration capacity is retained for documentation.
# Registration is not simulated as a workflow stage in this version.
REGISTRATION_CAPACITY = 3

OP_CAPACITY = 2
DIAGNOSTICS_CAPACITY = 2
PHARMACY_CAPACITY = 3

TRIAGE_CAPACITY = 2
EMERGENCY_TREATMENT_CAPACITY = 2


# ============================================================
# SIMPY ENVIRONMENT
# ============================================================

env = simpy.Environment()

op = simpy.Resource(env, capacity=OP_CAPACITY)
diagnostics = simpy.Resource(
    env, capacity=DIAGNOSTICS_CAPACITY
)
pharmacy = simpy.Resource(
    env, capacity=PHARMACY_CAPACITY
)

triage = simpy.Resource(
    env, capacity=TRIAGE_CAPACITY
)
emergency_treatment = simpy.Resource(
    env, capacity=EMERGENCY_TREATMENT_CAPACITY
)


# ============================================================
# DATA STORAGE
# ============================================================

visits = []
workflow_events = []


# ============================================================
# TIME CONVERSION
# ============================================================

def to_datetime(sim_time):
    """Convert SimPy minutes into a calendar datetime."""
    return START_DATE + timedelta(minutes=float(sim_time))


def calendar_to_sim_minutes(calendar_time):
    """Convert a calendar datetime into SimPy minutes."""
    return (
        calendar_time - START_DATE
    ).total_seconds() / 60.0


# ============================================================
# SERVICE-TIME DISTRIBUTIONS
# ============================================================

def gamma_service(shape, scale):
    return max(
        1.0,
        float(np.random.gamma(shape, scale))
    )


def lognormal_service(log_mean, sigma):
    """
    log_mean is the mean in log-space, not the arithmetic mean.
    This preserves the service-time distributions used in V2.
    """
    return max(
        1.0,
        float(np.random.lognormal(log_mean, sigma))
    )


# ============================================================
# RECORD WORKFLOW EVENT
# ============================================================

def record_event(
    visit_id,
    stage,
    queue_entry,
    service_start,
    service_end,
    staff_id
):
    workflow_events.append({
        "visit_id": visit_id,
        "stage": stage,
        "queue_entry_time": to_datetime(queue_entry),
        "service_start_time": to_datetime(service_start),
        "service_end_time": to_datetime(service_end),
        "staff_id": staff_id
    })


# ============================================================
# NORMAL PATIENT WORKFLOW
# ============================================================

def normal_patient(env, visit_id):

    # --------------------------------------------------------
    # OP CONSULTATION
    # --------------------------------------------------------

    queue_entry = env.now

    with op.request() as request:
        yield request

        service_start = env.now

        service_time = lognormal_service(
            np.log(14),
            0.30
        )

        yield env.timeout(service_time)

        service_end = env.now

    record_event(
        visit_id,
        "OP",
        queue_entry,
        service_start,
        service_end,
        random.choice([2, 3])
    )

    # --------------------------------------------------------
    # DIAGNOSTICS: 45% OF NORMAL PATIENTS
    # --------------------------------------------------------

    if random.random() < 0.45:

        queue_entry = env.now

        with diagnostics.request() as request:
            yield request

            service_start = env.now

            service_time = gamma_service(
                4.0,
                3.0
            )

            yield env.timeout(service_time)

            service_end = env.now

        record_event(
            visit_id,
            "DIAGNOSTICS",
            queue_entry,
            service_start,
            service_end,
            5
        )

    # --------------------------------------------------------
    # PHARMACY
    # --------------------------------------------------------

    queue_entry = env.now

    with pharmacy.request() as request:
        yield request

        service_start = env.now

        service_time = gamma_service(
            3.5,
            2.0
        )

        yield env.timeout(service_time)

        service_end = env.now

    record_event(
        visit_id,
        "PHARMACY",
        queue_entry,
        service_start,
        service_end,
        4
    )


# ============================================================
# EMERGENCY PATIENT WORKFLOW
# ============================================================

def emergency_patient(env, visit_id):

    # --------------------------------------------------------
    # TRIAGE
    # --------------------------------------------------------

    queue_entry = env.now

    with triage.request() as request:
        yield request

        service_start = env.now

        service_time = gamma_service(
            3.0,
            2.0
        )

        yield env.timeout(service_time)

        service_end = env.now

    record_event(
        visit_id,
        "TRIAGE",
        queue_entry,
        service_start,
        service_end,
        None
    )

    # --------------------------------------------------------
    # EMERGENCY TREATMENT
    # --------------------------------------------------------

    queue_entry = env.now

    with emergency_treatment.request() as request:
        yield request

        service_start = env.now

        service_time = lognormal_service(
            np.log(22),
            0.25
        )

        yield env.timeout(service_time)

        service_end = env.now

    record_event(
        visit_id,
        "EMERGENCY_TREATMENT",
        queue_entry,
        service_start,
        service_end,
        None
    )

    # --------------------------------------------------------
    # DIAGNOSTICS: 55% OF EMERGENCY PATIENTS
    # --------------------------------------------------------

    if random.random() < 0.55:

        queue_entry = env.now

        with diagnostics.request() as request:
            yield request

            service_start = env.now

            service_time = gamma_service(
                4.0,
                3.0
            )

            yield env.timeout(service_time)

            service_end = env.now

        record_event(
            visit_id,
            "DIAGNOSTICS",
            queue_entry,
            service_start,
            service_end,
            None
        )

    # --------------------------------------------------------
    # PHARMACY: 65% OF EMERGENCY PATIENTS
    # --------------------------------------------------------

    if random.random() < 0.65:

        queue_entry = env.now

        with pharmacy.request() as request:
            yield request

            service_start = env.now

            service_time = gamma_service(
                3.5,
                2.0
            )

            yield env.timeout(service_time)

            service_end = env.now

        record_event(
            visit_id,
            "PHARMACY",
            queue_entry,
            service_start,
            service_end,
            4
        )


# ============================================================
# ARRIVAL RATE
# ============================================================

def get_mean_interarrival(hour):
    """
    Mean arrival interval in minutes.

    These assumptions are retained from V2.
    """

    if 9 <= hour < 12:
        return 6.0

    elif 16 <= hour < 18:
        return 7.0

    elif 12 <= hour < 14:
        return 12.0

    else:
        return 9.0


# ============================================================
# ARRIVAL PROCESS - V3
# ============================================================

def generate_patients():

    visit_id = 1

    current_day = START_DATE.replace(
        hour=HOSPITAL_OPEN_HOUR,
        minute=0,
        second=0,
        microsecond=0
    )

    while visit_id <= NUM_VISITS:

        # ----------------------------------------------------
        # WAIT UNTIL THE NEXT HOSPITAL DAY OPENS
        # ----------------------------------------------------

        day_start_sim_time = calendar_to_sim_minutes(
            current_day
        )

        if env.now < day_start_sim_time:
            yield env.timeout(
                day_start_sim_time - env.now
            )

        # ----------------------------------------------------
        # CALCULATE CLOSING TIME
        # ----------------------------------------------------

        day_end_datetime = current_day + timedelta(
            hours=HOSPITAL_CLOSE_HOUR - HOSPITAL_OPEN_HOUR
        )

        day_end_sim_time = calendar_to_sim_minutes(
            day_end_datetime
        )

        # ----------------------------------------------------
        # GENERATE ARRIVALS FOR THIS HOSPITAL DAY
        # ----------------------------------------------------

        while (
            visit_id <= NUM_VISITS
            and env.now < day_end_sim_time
        ):

            current_datetime = to_datetime(env.now)

            hour = current_datetime.hour

            mean_gap = get_mean_interarrival(hour)

            # Find the next whole-hour boundary.
            next_hour = current_datetime.replace(
                minute=0,
                second=0,
                microsecond=0
            ) + timedelta(hours=1)

            next_boundary_sim_time = (
                calendar_to_sim_minutes(next_hour)
            )

            # Do not cross the hospital closing time.
            next_boundary_sim_time = min(
                next_boundary_sim_time,
                day_end_sim_time
            )

            time_to_boundary = (
                next_boundary_sim_time - env.now
            )

            if time_to_boundary <= 0:
                break

            # Sample the arrival interval using the current rate.
            interarrival = np.random.exponential(
                scale=mean_gap
            )

            # ------------------------------------------------
            # IMPORTANT V3 FIX
            # ------------------------------------------------
            # If the sampled interval crosses the next hourly
            # boundary, advance to that boundary and sample
            # again using the new hour's arrival rate.
            # ------------------------------------------------

            if interarrival >= time_to_boundary:

                yield env.timeout(time_to_boundary)

                continue

            yield env.timeout(interarrival)

            arrival_time = env.now
            arrival_datetime = to_datetime(arrival_time)

            # ------------------------------------------------
            # PATIENT TYPE
            # ------------------------------------------------

            patient_type = (
                "EMERGENCY"
                if random.random() < 0.10
                else "OP"
            )

            # ------------------------------------------------
            # SAVE VISIT
            # ------------------------------------------------

            visits.append({
                "visit_id": visit_id,
                "visit_date": arrival_datetime.date(),
                "arrival_time": arrival_datetime,
                "patient_type": patient_type,
                "status": "COMPLETED"
            })

            # ------------------------------------------------
            # START PATIENT WORKFLOW
            # ------------------------------------------------

            if patient_type == "EMERGENCY":

                env.process(
                    emergency_patient(
                        env,
                        visit_id
                    )
                )

            else:

                env.process(
                    normal_patient(
                        env,
                        visit_id
                    )
                )

            visit_id += 1

        # ----------------------------------------------------
        # ADVANCE TO THE NEXT CALENDAR DAY
        # ----------------------------------------------------

        current_day += timedelta(days=1)


# ============================================================
# RUN SIMULATION
# ============================================================

print("=" * 60)
print("SMART HOSPITAL DES SIMULATION V3")
print("=" * 60)

print(f"Target visits: {NUM_VISITS}")
print(
    f"Hospital hours: "
    f"{HOSPITAL_OPEN_HOUR}:00 - "
    f"{HOSPITAL_CLOSE_HOUR}:00"
)

print("Starting simulation...")

env.process(generate_patients())
env.run()

print("Simulation completed.")


# ============================================================
# CREATE DATAFRAMES
# ============================================================

visits_df = pd.DataFrame(visits)
workflow_df = pd.DataFrame(workflow_events)

if visits_df.empty or workflow_df.empty:
    raise RuntimeError(
        "Simulation produced no visit or workflow records."
    )


# ============================================================
# SORT RECORDS
# ============================================================

visits_df = visits_df.sort_values(
    "visit_id"
).reset_index(drop=True)

workflow_df = workflow_df.sort_values(
    ["visit_id", "queue_entry_time"]
).reset_index(drop=True)


# ============================================================
# CALCULATE WAITING TIME
# ============================================================

workflow_df["waiting_time"] = (
    workflow_df["service_start_time"]
    - workflow_df["queue_entry_time"]
).dt.total_seconds() / 60.0


# ============================================================
# CALCULATE SERVICE TIME
# ============================================================

workflow_df["service_time"] = (
    workflow_df["service_end_time"]
    - workflow_df["service_start_time"]
).dt.total_seconds() / 60.0


# ============================================================
# ROUND VALUES
# ============================================================

workflow_df["waiting_time"] = (
    workflow_df["waiting_time"].round(2)
)

workflow_df["service_time"] = (
    workflow_df["service_time"].round(2)
)


# ============================================================
# DATA VALIDATION
# ============================================================

print("\n" + "=" * 60)
print("DATA QUALITY VALIDATION")
print("=" * 60)

print(f"Expected visits: {NUM_VISITS}")
print(f"Generated visits: {len(visits_df)}")
print(f"Workflow events: {len(workflow_df)}")

assert len(visits_df) == NUM_VISITS, (
    "Generated visit count does not match NUM_VISITS."
)

assert visits_df["visit_id"].is_unique, (
    "Duplicate visit IDs detected."
)

assert not visits_df["arrival_time"].isna().any(), (
    "Missing arrival timestamps detected."
)

assert not workflow_df["queue_entry_time"].isna().any(), (
    "Missing queue-entry timestamps detected."
)

assert not workflow_df["service_start_time"].isna().any(), (
    "Missing service-start timestamps detected."
)

assert not workflow_df["service_end_time"].isna().any(), (
    "Missing service-end timestamps detected."
)

assert (
    workflow_df["service_start_time"]
    >= workflow_df["queue_entry_time"]
).all(), "Invalid waiting-time timestamps detected."

assert (
    workflow_df["service_end_time"]
    >= workflow_df["service_start_time"]
).all(), "Invalid service-time timestamps detected."

assert (workflow_df["waiting_time"] >= 0).all(), (
    "Negative waiting times detected."
)

assert (workflow_df["service_time"] >= 0).all(), (
    "Negative service times detected."
)

print("All mandatory validation checks passed.")


# ============================================================
# STAGE SUMMARY
# ============================================================

stage_summary = workflow_df.groupby("stage").agg(
    total_events=("visit_id", "count"),
    average_wait_minutes=("waiting_time", "mean"),
    median_wait_minutes=("waiting_time", "median"),
    maximum_wait_minutes=("waiting_time", "max"),
    average_service_minutes=("service_time", "mean"),
    high_wait_cases=(
        "waiting_time",
        lambda values: (values >= 20).sum()
    )
).round(2)

stage_summary["high_wait_percentage"] = (
    stage_summary["high_wait_cases"]
    / stage_summary["total_events"]
    * 100
).round(2)

print("\nStage summary:")
print(stage_summary)


# ============================================================
# HOURLY OP SUMMARY
# ============================================================

op_events = workflow_df[
    workflow_df["stage"] == "OP"
].copy()

op_events["arrival_hour"] = (
    op_events["queue_entry_time"].dt.hour
)

hourly_op_summary = op_events.groupby(
    "arrival_hour"
).agg(
    total_events=("waiting_time", "count"),
    average_wait_minutes=("waiting_time", "mean"),
    high_wait_cases=(
        "waiting_time",
        lambda values: (values >= 20).sum()
    )
).round(2)

hourly_op_summary["high_wait_percentage"] = (
    hourly_op_summary["high_wait_cases"]
    / hourly_op_summary["total_events"]
    * 100
).round(2)

print("\nHourly OP summary:")
print(hourly_op_summary)


# ============================================================
# SAVE V3 OUTPUTS SEPARATELY
# ============================================================

output_folder = Path(__file__).resolve().parent

visits_path = output_folder / "des_visits_v3.csv"
workflow_path = output_folder / "des_workflow_events_v3.csv"
stage_summary_path = output_folder / "des_stage_summary_v3.csv"
hourly_summary_path = output_folder / "des_hourly_op_summary_v3.csv"

visits_df.to_csv(
    visits_path,
    index=False
)

workflow_df.to_csv(
    workflow_path,
    index=False
)

stage_summary.to_csv(
    stage_summary_path
)

hourly_op_summary.to_csv(
    hourly_summary_path
)

print("\n" + "=" * 60)
print("V3 FILES SAVED")
print("=" * 60)

print(visits_path)
print(workflow_path)
print(stage_summary_path)
print(hourly_summary_path)

print("\nMySQL was NOT modified.")
print("V2 output files were NOT overwritten.")
print("=" * 60)