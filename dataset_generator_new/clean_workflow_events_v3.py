import csv
import math
from pathlib import Path

folder = Path(
    r"C:\Users\Dell\Downloads\smart-hospital-eabda-v1\smart-hospital-backup\dataset_generator_new"
)

input_file = folder / "des_workflow_events_v3.csv"
output_file = folder / "des_workflow_events_v3_clean.csv"

with input_file.open("r", newline="", encoding="utf-8-sig") as source:
    reader = csv.DictReader(source)

    with output_file.open("w", newline="", encoding="utf-8") as target:
        writer = csv.DictWriter(target, fieldnames=reader.fieldnames)
        writer.writeheader()

        for row in reader:
            staff_id = (row.get("staff_id") or "").strip()

            if staff_id and staff_id.lower() != "nan":
                number = float(staff_id)

                if not math.isfinite(number) or not number.is_integer():
                    raise ValueError(f"Invalid staff_id: {staff_id}")

                row["staff_id"] = str(int(number))
            else:
                row["staff_id"] = ""

            writer.writerow(row)

print("Cleaned CSV created:", output_file)