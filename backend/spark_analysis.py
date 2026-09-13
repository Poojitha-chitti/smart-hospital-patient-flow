from pyspark.sql import SparkSession
from pyspark.sql.functions import avg, count

# Create Spark session
spark = SparkSession.builder \
    .appName("SmartHospitalPatientFlow") \
    .master("local[1]") \
    .getOrCreate()

print("\n=== SMART HOSPITAL BIG DATA ANALYSIS ===\n")

# Read hospital workflow data from CSV
df = spark.read \
    .option("header", "true") \
    .option("inferSchema", "true") \
    .csv(r"C:\Users\Dell\Downloads\smart-hospital\hospital_workflow_data.csv")

# Count total workflow records
print("Total workflow records:", df.count())

# Display sample records
print("\n=== DATA SAMPLE ===")
df.show(10)

# Stage-wise analysis
print("\n=== STAGE-WISE BIG DATA ANALYSIS ===")

stage_analysis = df.groupBy("stage").agg(
    avg("waiting_time").alias("average_waiting_time"),
    avg("service_time").alias("average_service_time"),
    count("*").alias("event_count")
).orderBy(
    "average_waiting_time",
    ascending=False
)

# Display analysis result
stage_analysis.show()

print("\n=== SPARK BIG DATA PROCESSING COMPLETED ===")

# Stop Spark
spark.stop()