import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./WaitingList.css";

function WaitingList() {
    const [patients, setPatients] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadWaitingPatients = async () => {
        try {
            setLoading(true);

            const response = await fetch(`${API_URL}/api/patients`);

            if (!response.ok) {
                throw new Error("Failed to load patients");
            }

            const data = await response.json();

            const waitingPatients = data
                .filter(patient => patient.status === "WAITING")
                .sort((a, b) => b.patient_id - a.patient_id);

            setPatients(waitingPatients);
            setError("");
        } catch (err) {
            console.error(err);
            setError("Unable to load waiting list.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadWaitingPatients();
    }, []);

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

            // Reload the waiting list.
            // The patient will disappear from this page
            // because it is no longer WAITING.
            loadWaitingPatients();

        } catch (err) {
            console.error(err);
            setError("Unable to update patient status.");
        }
    };

    return (
        <div className="waiting-page">

            <div className="waiting-header">
                <div>
                    <h1>Waiting List</h1>
                    <p>Patients currently waiting in the hospital workflow</p>
                </div>

                <button
                    className="back-button"
                    onClick={() => window.location.href = "/home"}
                >
                    ← Back to Home
                </button>
            </div>

            <div className="waiting-summary">
                <div>
                    <span>Total Waiting</span>
                    <strong>{patients.length}</strong>
                </div>
            </div>

            {loading && (
                <p className="waiting-message">Loading waiting patients...</p>
            )}

            {error && (
                <p className="waiting-error">{error}</p>
            )}

            {!loading && !error && patients.length === 0 && (
                <div className="empty-waiting">
                    <h3>No patients are currently waiting</h3>
                    <p>
                        New patients with WAITING status will appear here.
                    </p>
                </div>
            )}

            {!loading && patients.length > 0 && (
                <div className="waiting-table-container">
                    <table className="waiting-table">
                        <thead>
                            <tr>
                                <th>Visit ID</th>
                                <th>Patient Name</th>
                                <th>Age</th>
                                <th>Gender</th>
                                <th>Department</th>
                                <th>Registration Time</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>

                        <tbody>
                            {patients.map((patient) => (
                                <tr key={patient.patient_id}>
                                    <td>{patient.visit_id}</td>
                                    <td>{patient.patient_name}</td>
                                    <td>{patient.age}</td>
                                    <td>{patient.gender || "-"}</td>
                                    <td>{patient.department}</td>
                                    <td>{patient.registration_time}</td>

                                    <td>
                                        <span className="waiting-status">
                                            {patient.status}
                                        </span>
                                    </td>

                                    <td>
                                        <button
                                            className="consultation-button"
                                            onClick={() =>
                                                updateStatus(
                                                    patient.patient_id,
                                                    "IN CONSULTATION"
                                                )
                                            }
                                        >
                                            Start Consultation
                                        </button>
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

export default WaitingList;