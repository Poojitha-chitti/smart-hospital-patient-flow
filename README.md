Smart Hospital Patient Flow and Bottleneck Analysis

1. Project Overview

Smart Hospital Patient Flow and Bottleneck Analysis is a web-based application designed to help hospital staff monitor patient flow, waiting times, hospital resources, and service bottlenecks.

The system helps hospital administrators and staff understand delays in hospital operations and supports data-driven decision-making using data mining and machine learning.

Main Objectives

- Manage patient registration and patient records.
- Monitor patient waiting lists and consultation status.
- Analyze patient flow across hospital service stages.
- Identify potential bottlenecks and long waiting times.
- Analyze hospital resources.
- Provide decision support using data mining and machine learning.

2. Technology Stack

Component| Technology
Frontend| React.js, Vite, HTML, CSS, JavaScript
Backend| Java, Spring Boot
Application Database| MySQL
Research Database| MySQL
Machine Learning| Python, Scikit-learn
Data Analysis| Python, Data Mining
Version Control| Git and GitHub

3. System Requirements

Install the following software before running the project:

1. Git
2. Java JDK 25
3. Apache Maven 3.9.11
4. Node.js and npm
5. MySQL Server 8.0
6. MySQL Workbench
7. Python and the compatible Python packages required by the ML service
8. Visual Studio Code (VS Code)

Note: Python 3.14.5 was used in the development environment. If a required machine-learning library does not support that version, use a compatible Python version.

4. Download the Project

Open PowerShell and run:

git clone https://github.com/Poojitha-chitti/smart-hospital-patient-flow.git

Navigate into the project folder:

cd smart-hospital-patient-flow

The repository's "main" branch contains the latest merged project updates.

5. Database Setup

The application uses two databases:

- "hospital_db" — stores application data.
- "hospital_research_db" — stores research and analytics data.

Step 1: Start MySQL

Open MySQL Workbench and connect to your local MySQL Server.

Step 2: Create the databases

Execute:

CREATE DATABASE hospital_db;
CREATE DATABASE hospital_research_db;

Step 3: Create the required tables

The databases must contain the tables, relationships, and initial records required by the application.

Use the project's prepared database setup SQL files when they are available.

Important: Creating the databases alone is not sufficient. The required tables and any necessary initial data must also be imported or created before running the application.

6. Backend Setup

The backend is developed using Java and Spring Boot.

Step 1: Open the backend folder

From the project root:

cd backend

Step 2: Configure the application

Configure the backend to connect to your local MySQL databases.

Set the required database connection details, including:

- Database URL
- Database username
- Database password
- Required email settings
- Machine-learning service URL

Use the existing project configuration as a reference.

Do not upload passwords, email app passwords, or other private credentials to GitHub.

Step 3: Start the backend

Run:

mvn spring-boot:run

Wait until Spring Boot starts successfully.

Keep this terminal open while using the application.

7. Frontend Setup

The frontend is developed using React and Vite.

Open a second PowerShell terminal.

From the project root, run:

cd frontend

Install the required dependencies:

npm install

Start the frontend:

npm run dev

Vite will display a local URL, usually:

http://localhost:5173

Open the displayed URL in your web browser.

Keep the frontend and backend running while using the application.

8. Machine-Learning Service Setup

The machine-learning service is implemented using Python.

Open a third PowerShell terminal and navigate to the project root.

Then run:

cd ml-service

Create a virtual environment:

python -m venv .venv

Activate it on Windows:

.\.venv\Scripts\Activate.ps1

Install the dependencies listed in the project's "requirements.txt" file:

python -m pip install -r requirements.txt

Follow the project's ML service configuration and startup instructions to start the Python service.

The backend must be configured to communicate with the running ML service.

Note: If "requirements.txt" is not present, prepare the dependency list before attempting installation. Do not guess package versions.

9. Application Features

The application includes the following modules:

- Home Dashboard
- Patient Registration
- Patient List
- Waiting List
- Doctor List
- Room List
- Patient Flow Analysis
- Data Mining
- Resource Analysis
- Decision Support

Access to certain modules depends on the user's role and permissions.

10. Running the Complete Application

To use the application locally:

1. Start MySQL Server.
2. Ensure both databases and their required tables are configured.
3. Start the Spring Boot backend.
4. Start the React frontend.
5. Start the Python ML service if ML functionality is required.
6. Open the frontend URL in your browser.
7. Log in using a valid account configured for your local environment.

Test the main patient workflow and analysis features.

11. Troubleshooting

Backend does not start

- Check the installed Java and Maven versions.
- Verify the database connection settings.
- Confirm that MySQL Server is running.
- Check the terminal for error messages.

Frontend does not start

- Verify that Node.js and npm are installed.
- Run "npm install" inside the "frontend" folder.
- Check the terminal for missing dependencies or errors.

Database connection fails

- Verify that both databases exist.
- Check the database username and password.
- Confirm that the required tables have been created.
- Ensure the configured database port and URL are correct.

Machine-learning service fails

- Check the Python version.
- Install the required Python dependencies.
- Verify the ML service configuration and model files.
- Confirm that the backend points to the correct ML service URL.

12. Important Notes

- Do not commit passwords or private configuration files to GitHub.
- Use synthetic or test data for demonstrations.
- Each teammate running the application with local databases will initially have separate data.
- Sharing the same data between teammates at different locations requires a shared database and an accessible backend. This will be configured during the deployment phase.

13. GitHub Repository

Repository:

https://github.com/Poojitha-chitti/smart-hospital-patient-flow

14. Project Purpose

This project aims to improve hospital operational efficiency by monitoring patient flow, analyzing waiting times, identifying bottlenecks, and supporting hospital staff with data-driven insights.