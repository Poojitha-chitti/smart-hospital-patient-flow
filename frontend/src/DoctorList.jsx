import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./DoctorList.css";

function DoctorList() {
    const [doctors, setDoctors] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadDoctors = async () => {
        try {
            setLoading(true);

            const response = await fetch(`${API_URL}/api/doctors`);

            if (!response.ok) {
                throw new Error("Failed to load doctors");
            }

            const data = await response.json();

            setDoctors(data);
            setError("");
        } catch (err) {
            console.error(err);
            setError("Unable to load doctor list.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadDoctors();
    }, []);

    return (
        <div className="doctor-list-page">

            <div className="doctor-list-header">
                <div>
                    <h1>Doctor List</h1>
                    <p>Doctors available in the hospital</p>
                </div>

                <button
                    className="back-button"
                    onClick={() => window.location.href = "/home"}
                >
                    ← Back to Home
                </button>
            </div>

            <div className="doctor-summary">
                <span>Total Doctors</span>
                <strong>{doctors.length}</strong>
            </div>

            {loading && (
                <p className="doctor-message">
                    Loading doctors...
                </p>
            )}

            {error && (
                <p className="doctor-error">
                    {error}
                </p>
            )}

            {!loading && !error && doctors.length === 0 && (
                <div className="empty-doctors">
                    <h3>No doctors found</h3>
                    <p>Doctor information will appear here.</p>
                </div>
            )}

            {!loading && doctors.length > 0 && (
                <div className="doctor-table-container">
                    <table className="doctor-table">
                        <thead>
                            <tr>
                                <th>Doctor ID</th>
                                <th>Doctor Name</th>
                                <th>Specialization</th>
                                <th>Department</th>
                                <th>Room No.</th>
                                <th>Status</th>
                            </tr>
                        </thead>

                        <tbody>
                            {doctors.map((doctor) => (
                                <tr key={doctor.doctor_id}>
                                    <td>{doctor.doctor_id}</td>
                                    <td>{doctor.doctor_name}</td>
                                    <td>{doctor.specialization}</td>
                                    <td>{doctor.department}</td>
                                    <td>{doctor.room_no}</td>
                                    <td>
                                        <span
                                            className={
                                                doctor.status === "AVAILABLE"
                                                    ? "doctor-status available"
                                                    : "doctor-status busy"
                                            }
                                        >
                                            {doctor.status}
                                        </span>
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

export default DoctorList;