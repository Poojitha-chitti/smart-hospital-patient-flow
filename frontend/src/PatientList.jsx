import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./PatientList.css";

function PatientList() {
    const [patients, setPatients] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadPatients = async () => {
        try {
            setLoading(true);

            const response = await fetch(`${API_URL}/api/patients`);

            if (!response.ok) {
                throw new Error("Failed to load patients");
            }

            const data = await response.json();

            setPatients(data);
            setError("");
        } catch (err) {
            console.error(err);
            setError("Unable to load patient list.");
        } finally {
            setLoading(false);
        }
    };
const updateStatus = async (patientId, newStatus) => {
    try {
        const response = await fetch(
            `${API_URL}/api/patients/${patientId}/status`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    status: newStatus
                })
            }
        );

        if (!response.ok) {
            throw new Error("Failed to update patient status");
        }

        // Reload the patient list so the new status appears immediately.
        loadPatients();

    } catch (err) {
        console.error(err);
        setError("Unable to update patient status.");
    }
};
const calculateDuration = (startTime, endTime) => {
    if (!startTime || !endTime) {
        return "-";
    }

    const start = new Date(startTime);
    const end = new Date(endTime);

    const differenceInSeconds = Math.floor(
        (end - start) / 1000
    );

    if (differenceInSeconds < 0) {
        return "-";
    }

    const hours = Math.floor(differenceInSeconds / 3600);
    const minutes = Math.floor((differenceInSeconds % 3600) / 60);
    const seconds = differenceInSeconds % 60;

    if (hours > 0) {
        return `${hours}h ${minutes}m ${seconds}s`;
    }

    if (minutes > 0) {
        return `${minutes}m ${seconds}s`;
    }

    return `${seconds}s`;
};
    useEffect(() => {
        loadPatients();
    }, []);

    return (
        <div className="patient-list-page">

            <div className="patient-list-header">
                <div>
                    <h1>Patient List</h1>
                    <p>Registered patients in the hospital system</p>
                </div>

                <button
                    className="back-button"
                    onClick={() => window.location.href = "/home"}
                >
                    ← Back to Home
                </button>
            </div>

            <div className="patient-summary">
                <span>Total Registered Patients</span>
                <strong>{patients.length}</strong>
            </div>

            {loading && (
                <p className="patient-message">
                    Loading patients...
                </p>
            )}

            {error && (
                <p className="patient-error">
                    {error}
                </p>
            )}

            {!loading && !error && patients.length === 0 && (
                <div className="empty-patients">
                    <h3>No patients registered yet</h3>
                    <p>Registered patients will appear here.</p>
                </div>
            )}

            {!loading && patients.length > 0 && (
                <div className="patient-table-container">
                    <table className="patient-table">
                        <thead>
                            <tr>
                                <th>Patient ID</th>
                                <th>Visit ID</th>
                                <th>Patient Name</th>
                                <th>Age</th>
                                <th>Gender</th>
                                <th>Phone</th>
                                <th>Department</th>
                                <th>Registration Time</th>
                                <th>Waiting Time</th>
                                <th>Consultation Time</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>

                        <tbody>
                            {patients.map((patient) => (
                                <tr key={patient.patient_id}>
                                    <td>{patient.patient_id}</td>
                                    <td>{patient.visit_id}</td>
                                    <td>{patient.patient_name}</td>
                                    <td>{patient.age}</td>
                                    <td>{patient.gender || "-"}</td>
                                    <td>{patient.phone || "-"}</td>
                                    <td>{patient.department}</td>
                                    <td>{patient.registration_time}</td>
                                    <td>
    {calculateDuration(
        patient.registration_time,
        patient.consultation_start_time
    )}
</td>

<td>
    {calculateDuration(
        patient.consultation_start_time,
        patient.consultation_end_time
    )}
</td>
                                    <td>
                                        <span className="patient-status">
                                            {patient.status}
                                        </span>    
                                    </td>
                                    <td>
    {patient.status === "IN CONSULTATION" && (
        <button
            className="complete-button"
            onClick={() =>
                updateStatus(
                    patient.patient_id,
                    "COMPLETED"
                )
            }
        >
            Complete Consultation
        </button>
    )}
</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

        </div>
    );
}

export default PatientList;