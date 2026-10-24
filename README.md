Smart Hospital Patient Flow Bottleneck Analysis
A final-year B.Tech CSE project that analyzes hospital patient flow, identifies potential bottlenecks, and provides machine-learning-based decision support.
1. Project Overview
The application helps hospital staff manage patient registration, waiting lists, consultations, patient flow, hospital resources, and analytics.
Main Features
Patient registration and management
Patient waiting-list management
Doctor and room management
Patient consultation workflow
Patient flow analysis
Data mining and bottleneck analysis
Resource analysis
Decision support
Machine-learning-based waiting-time predictions
User authentication and role-based access
2. Technology Stack
Frontend: React and Vite
Backend: Java and Spring Boot
Database: MySQL
Machine Learning: Python and scikit-learn
Version Control: Git and GitHub
3. Prerequisites
Install the following software before running the application:
Git
Java JDK 25
Maven 3.9.11
Node.js and npm
MySQL Server 8.0
MySQL Workbench
Python and the required Python packages
Visual Studio Code (recommended)
Check the installations in PowerShell:
git --version
java -version
mvn -version
node --version
npm --version
python --version

The Python version must be compatible with the project's dependencies.
4. Download the Project
Open PowerShell and run:
git clone https://github.com/Poojitha-chitti/smart-hospital-patient-flow.git
cd smart-hospital-patient-flow

Open the project in Visual Studio Code:
code .

5. Database Setup
The project uses two databases:
hospital_db
hospital_research_db
Start MySQL Server and connect through MySQL Workbench.
Create the databases:
CREATE DATABASE hospital_db;
CREATE DATABASE hospital_research_db;

Creating the databases alone is not sufficient. The required tables and any essential initial data must also be imported.
Database setup files must be provided separately when available. Import each SQL file into its intended database, following the project maintainer's instructions.
Configure the backend to connect to the correct local MySQL databases. Use your own local database credentials; do not upload passwords to GitHub.
6. Configure the Backend
Open the backend folder:
cd backend

Configure the required database connections and other application settings in the appropriate local configuration file.
Do not share passwords, email app passwords, API keys, or other secrets.
Run the backend:
mvn spring-boot:run

Wait until Spring Boot reports that the application has started successfully.
Keep the terminal open.
7. Configure and Run the Frontend
Open a second PowerShell terminal.
Navigate to the frontend folder from the project root:
cd frontend

Install dependencies:
npm install

Start the development server:
npm run dev

Open the local URL displayed in the terminal, usually:
http://localhost:5173
Keep the frontend terminal open.
8. Run the Machine Learning Service
Open a third PowerShell terminal.
Navigate to the ML service folder:
cd ml-service

If a requirements.txt file is available, create a virtual environment:
python -m venv .venv

Activate it on Windows:
.\.venv\Scripts\Activate.ps1

Install the required packages:
python -m pip install -r requirements.txt

Follow the project's ML service startup instructions to run the Python application.
Configure the backend to use the correct ML service address. The backend, frontend, and ML service must all be running for features that depend on them to work.
9. Test the Application
After completing the setup:
Open the frontend in your browser.
Log in using a valid account configured for your local setup.
Register a test patient.
Check the patient list and waiting list.
Test the consultation workflow.
Open Patient Flow Analysis.
Test Data Mining and Decision Support.
Verify ML predictions if the ML service is configured.
Use synthetic or test data only.
10. Important Notes
Each teammate's local MySQL database is separate.
Local changes and patient records are not automatically shared between laptops.
Shared access from different locations requires the application and database to be deployed and configured for secure online access.
Never commit credentials or private configuration files to GitHub.
Database setup files and ML dependencies must be available before a complete local setup can be considered ready.
11. Project Repository
GitHub: https://github.com/Poojitha-chitti/smart-hospital-patient-flow