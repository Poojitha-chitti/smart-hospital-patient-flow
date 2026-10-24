import simpy
import pandas as pd
import numpy as np
import random
from datetime import datetime, timedelta

# ============================================================
# SMART HOSPITAL - DES DATASET GENERATOR V2
# ============================================================

NUM_VISITS = 10032

START_DATE = datetime(2026, 1, 1, 7, 0, 0)

HOSPITAL_OPEN_HOUR = 7
HOSPITAL_CLOSE_HOUR = 19

RANDOM_SEED = 42

random.seed(RANDOM_SEED)
np.random.seed(RANDOM_SEED)


# ============================================================
# HOSPITAL RESOURCES
# ============================================================

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
diagnostics = simpy.Resource(env, capacity=DIAGNOSTICS_CAPACITY)
pharmacy = simpy.Resource(env, capacity=PHARMACY_CAPACITY)

triage = simpy.Resource(env, capacity=TRIAGE_CAPACITY)
emergency_treatment = simpy.Resource(
    env,
    capacity=EMERGENCY_TREATMENT_CAPACITY
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
    return START_DATE + timedelta(minutes=sim_time)


# ============================================================
# SERVICE TIME DISTRIBUTIONS
# ============================================================

def gamma_service(shape, scale):
    return max(
        1.0,
        np.random.gamma(shape, scale)
    )


def lognormal_service(mean, sigma):
    return max(
        1.0,
        np.random.lognormal(mean, sigma)
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
# NORMAL PATIENT
# ============================================================

def normal_patient(
    env,
    visit_id
):

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
    # DIAGNOSTICS
    # --------------------------------------------------------

    # Around 45% of patients need diagnostics.

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
# EMERGENCY PATIENT
# ============================================================

def emergency_patient(
    env,
    visit_id
):

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
    # DIAGNOSTICS FOR SOME EMERGENCY PATIENTS
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
    # PHARMACY FOR SOME EMERGENCY PATIENTS
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
# ARRIVAL PROCESS
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
        # MOVE TO NEXT HOSPITAL DAY
        # ----------------------------------------------------

        day_start = current_day

        minutes_from_start = (
            day_start - START_DATE
        ).total_seconds() / 60

        if env.now < minutes_from_start:
            yield env.timeout(
                minutes_from_start - env.now
            )


        # ----------------------------------------------------
        # ARRIVALS DURING ONE HOSPITAL DAY
        # ----------------------------------------------------

        day_end_datetime = (
            current_day +
            timedelta(
                hours=HOSPITAL_CLOSE_HOUR -
                HOSPITAL_OPEN_HOUR
            )
        )

        day_end_sim_time = (
            day_end_datetime - START_DATE
        ).total_seconds() / 60


        while (
            visit_id <= NUM_VISITS
            and env.now < day_end_sim_time
        ):

            current_datetime = to_datetime(env.now)

            hour = current_datetime.hour


            # ------------------------------------------------
            # ARRIVAL RATE
            # ------------------------------------------------

            if 9 <= hour < 12:

                # Morning peak
                mean_gap = 6.0

            elif 16 <= hour < 18:

                # Evening peak
                mean_gap = 7.0

            elif 12 <= hour < 14:

                # Lunch period
                mean_gap = 12.0

            else:

                # Normal period
                mean_gap = 9.0


            interarrival = np.random.exponential(
                mean_gap
            )


            # ------------------------------------------------
            # CHECK WHETHER NEXT PATIENT ARRIVES BEFORE
            # HOSPITAL CLOSING
            # ------------------------------------------------

            if env.now + interarrival > day_end_sim_time:

                yield env.timeout(
                    day_end_sim_time - env.now
                )

                break


            yield env.timeout(interarrival)


            arrival_time = env.now

            arrival_datetime = to_datetime(
                arrival_time
            )


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
        # NEXT DAY
        # ----------------------------------------------------

        current_day += timedelta(days=1)


# ============================================================
# RUN SIMULATION
# ============================================================

print("========================================")
print("SMART HOSPITAL DES SIMULATION V2")
print("========================================")

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
# DATAFRAMES
# ============================================================

visits_df = pd.DataFrame(visits)

workflow_df = pd.DataFrame(workflow_events)


# ============================================================
# SORT
# ============================================================

visits_df = visits_df.sort_values(
    "visit_id"
).reset_index(drop=True)

workflow_df = workflow_df.sort_values(
    ["visit_id", "queue_entry_time"]
).reset_index(drop=True)


# ============================================================
# WAITING TIME
# ============================================================

workflow_df["waiting_time"] = (
    workflow_df["service_start_time"]
    - workflow_df["queue_entry_time"]
).dt.total_seconds() / 60


# ============================================================
# SERVICE TIME
# ============================================================

workflow_df["service_time"] = (
    workflow_df["service_end_time"]
    - workflow_df["service_start_time"]
).dt.total_seconds() / 60


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
# SAVE FILES
# ============================================================

from pathlib import Path

output_folder = Path(__file__).parent

visits_df.to_csv(output_folder / "des_visits.csv", index=False)
workflow_df.to_csv(output_folder / "des_workflow_events.csv", index=False)

print("\nCSV files saved successfully!")
print("Folder:", output_folder)
# SUMMARY
# ============================================================

print()
print("========================================")
print("DATASET SUMMARY")
print("========================================")

print(
    f"Visits generated: {len(visits_df)}"
)

print(
    f"Workflow events generated: "
    f"{len(workflow_df)}"
)

print()
print("Patient types:")
print(
    visits_df["patient_type"].value_counts()
)

print()
print("Workflow stages:")
print(
    workflow_df["stage"].value_counts()
)

print()
print("Average waiting time by stage:")
print(
    workflow_df
    .groupby("stage")["waiting_time"]
    .mean()
    .round(2)
)

print()
print("Average service time by stage:")
print(
    workflow_df
    .groupby("stage")["service_time"]
    .mean()
    .round(2)
)

print()
print("Maximum waiting time by stage:")
print(
    workflow_df
    .groupby("stage")["waiting_time"]
    .max()
    .round(2)
)

print()
print(
    "High waiting events (>= 20 minutes):"
)

high_wait = (
    workflow_df["waiting_time"] >= 20
).sum()

print(high_wait)

print()
print("Files created:")
print("des_visits.csv")
print("des_workflow_events.csv")

print()
print("========================================")
print("MySQL was NOT modified.")
print("========================================")

# ============================================================
# READ-ONLY WAITING TIME VALIDATION
# ============================================================

print("\n========================================")
print("WAITING TIME VALIDATION")
print("========================================")

validation = workflow_df.groupby("stage").agg(
    total_events=("waiting_time", "count"),
    zero_wait_events=("waiting_time", lambda x: (x == 0).sum()),
    average_wait=("waiting_time", "mean"),
    median_wait=("waiting_time", "median"),
    maximum_wait=("waiting_time", "max"),
    average_service_time=("service_time", "mean")
)

validation["zero_wait_percentage"] = (
    validation["zero_wait_events"]
    / validation["total_events"]
    * 100
).round(2)

print(validation.round(2))