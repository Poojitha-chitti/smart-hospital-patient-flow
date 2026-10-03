import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./PatientList.css";

function PatientList() {
  const [patients, setPatients] = useState([]);
  const [workArea, setWorkArea] = useState("");
  const [loading, setLoading] = useState(true);
  const [workAreaLoading, setWorkAreaLoading] = useState(true);
  const [error, setError] = useState("");
  const [currentTime, setCurrentTime] = useState(Date.now());

useEffect(() => {
  const timer = setInterval(() => {
    setCurrentTime(Date.now());
  }, 1000);

  return () => clearInterval(timer);
}, []);
  const [showNextStage, setShowNextStage] = useState(false);
  const [selectedPatientId, setSelectedPatientId] = useState(null);
  const [nextStageType, setNextStageType] = useState("");

  const username = localStorage.getItem("username") || "";
  const role = localStorage.getItem("role") || "";

  const loadWorkArea = async () => {
    try {
      setWorkAreaLoading(true);

      if (role === "ADMIN") {
        setWorkArea("ALL");
        return;
      }

      const response = await fetch(
        `${API_URL}/api/admin/users/staff`
      );

      if (!response.ok) {
        throw new Error("Unable to load staff information.");
      }

      const staffUsers = await response.json();

      const currentUser = staffUsers.find(
        (user) =>
          user.username &&
          user.username.toLowerCase() === username.toLowerCase()
      );

      setWorkArea(currentUser?.work_area || "");
    } catch {
      setError("Unable to determine your work area.");
    } finally {
      setWorkAreaLoading(false);
    }
  };

  const loadPatients = async () => {
    try {
      setLoading(true);

      const response = await fetch(`${API_URL}/api/patients`);

      if (!response.ok) {
        throw new Error("Unable to load patient list.");
      }

      const data = await response.json();
      setPatients(data);
      setError("");
    } catch {
      setError("Unable to load patient list.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkArea();
    loadPatients();
  }, []);

  /*
   * UPDATE PATIENT STATUS
   */
  const updateStatus = async (id, status, nextStage = null) => {
    try {
      setError("");

      const requestBody = {
        status,
        username
      };

      if (nextStage) {
        requestBody.nextStage = nextStage;
      }

      const response = await fetch(
        `${API_URL}/api/patients/${id}/status`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(requestBody)
        }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message || "Unable to update patient status."
        );
      }

      setShowNextStage(false);
      setSelectedPatientId(null);
      setNextStageType("");

      await loadPatients();
    } catch (err) {
      setError(err.message || "Unable to update patient status.");
    }
  };

  /*
   * NEXT-STAGE SELECTION
   */
  const openNextStageSelection = (patientId, type = "OP") => {
    setError("");
    setSelectedPatientId(patientId);
    setNextStageType(type);
    setShowNextStage(true);
  };

  const closeNextStageSelection = () => {
    setShowNextStage(false);
    setSelectedPatientId(null);
    setNextStageType("");
  };

  const handleNextStage = (nextStage) => {
    if (selectedPatientId === null) {
      return;
    }

    updateStatus(selectedPatientId, "COMPLETED", nextStage);
  };

  /*
   * DURATION
   */
  const duration = (a, b) => {
    if (!a || !b) return "—";

    const seconds = Math.floor(
      (new Date(b) - new Date(a)) / 1000
    );

    if (seconds < 0) return "—";

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const remainingSeconds = seconds % 60;

    if (hours) {
      return `${hours}h ${minutes}m ${remainingSeconds}s`;
    }

    if (minutes) {
      return `${minutes}m ${remainingSeconds}s`;
    }

    return `${remainingSeconds}s`;
  };
  const formatWaitingTime = (queueEntryTime, serviceStartTime) => {
  if (!queueEntryTime) return "—";

  const start = new Date(queueEntryTime);
  const end = serviceStartTime
    ? new Date(serviceStartTime)
    : new Date(currentTime);

  const totalSeconds = Math.max(
    0,
    Math.floor((end.getTime() - start.getTime()) / 1000)
  );

  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${minutes} min ${seconds} sec`;
};

  const stageWaitingTime = (queueEntryTime, serviceStartTime, status) => {
  if (!queueEntryTime) return "—";

  const start = new Date(queueEntryTime);
  const end = serviceStartTime
    ? new Date(serviceStartTime)
    : new Date();

  const minutes = Math.max(
    0,
    Math.floor((end.getTime() - start.getTime()) / 60000)
  );

  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
};

  /*
   * DATE / TIME
   */
  const dateTime = (value) => {
    if (!value) {
      return { date: "—", time: "" };
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return { date: value, time: "" };
    }

    return {
      date: date.toLocaleDateString("en-IN", {
        month: "short",
        day: "numeric"
      }),
      time: date.toLocaleTimeString("en-IN", {
        hour: "numeric",
        minute: "2-digit"
      })
    };
  };

  /*
   * LOADING
   */
  if (loading || workAreaLoading) {
    return (
      <div className="patient-list-page">
        <main className="patient-list-main">
          <div className="patient-list-empty">
            Loading patient list...
          </div>
        </main>
      </div>
    );
  }

  /*
   * ERROR
   */
  if (error && !patients.length) {
    return (
      <div className="patient-list-page">
        <main className="patient-list-main">
          <div className="patient-list-empty patient-list-error">
            {error}
          </div>
        </main>
      </div>
    );
  }

  /*
   * ACCESS CONTROL
   */
  const canCompleteOP = role === "ADMIN" || workArea === "OP";
  const canCompletePharmacy =
    role === "ADMIN" || workArea === "PHARMACY";
  const canCompleteDiagnostics =
    role === "ADMIN" || workArea === "DIAGNOSTICS";

  return (
    <div className="patient-list-page">
      <main className="patient-list-main">
        {/* PAGE HEADER */}
        <div className="patient-list-top-row">
          <div>
            <h1>Patient List</h1>
            <p>Registered patients in the hospital system.</p>
          </div>

          <button
  className="patient-list-home-btn"
  onClick={() =>
    (window.location.href = "/home?view=patients")
  }
>
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
  >
    <path d="M15 18l-6-6 6-6" />
  </svg>
  Back
</button>
        </div>

        {/* WORK AREA */}
        <div
          style={{
            marginBottom: "18px",
            padding: "13px 16px",
            background: "#EAF1EC",
            border: "1px solid #DFE5E0",
            borderRadius: "10px",
            color: "#123832",
            fontSize: "13.5px"
          }}
        >
          <strong>Your Work Area:</strong>{" "}
          {role === "ADMIN"
            ? "Administrator"
            : workArea === "OP"
            ? "OP Consultation"
            : workArea === "REGISTRATION"
            ? "Registration"
            : workArea === "PHARMACY"
            ? "Pharmacy"
            : workArea === "DIAGNOSTICS"
            ? "Diagnostics"
            : "Not Assigned"}
        </div>

        {/* TOTAL PATIENTS */}
        <div className="patient-list-stat">
          <span>Total registered patients</span>
          <strong>{patients.length}</strong>
        </div>

        {/* ERROR MESSAGE */}
        {error && (
          <div
            className="patient-list-empty patient-list-error"
            style={{ marginBottom: "16px" }}
          >
            {error}
          </div>
        )}

        {/* PATIENT TABLE */}
        {!patients.length ? (
          <div className="patient-list-empty">
            No patients registered yet.
            <br />
            Registered patients will appear here.
          </div>
        ) : (
          <div
  className="patient-list-table-card"
  style={{
    overflowX: "auto",
    maxWidth: "100%",
    WebkitOverflowScrolling: "touch"
  }}
>
            <table>
              <thead>
                <tr>
                  <th>Visit</th>
                  <th>Patient</th>
                  <th>Age</th>
                  <th>Gender</th>
                  <th>Phone</th>
                  <th>Dept.</th>
                  <th>Registered</th>
                  <th>Waiting</th>
                  <th style={{ minWidth: "180px" }}>Stage Waiting Times</th>
                  <th>Consultation</th>
                  <th>Diagnostics</th>
                  <th>Pharmacy</th>
                  <th className="patient-status-column">Status</th>
                </tr>
              </thead>

              <tbody>
                {patients.map((p) => {
                  const registered = dateTime(p.registration_time);

                  return (
                    <tr key={p.patient_id}>
                      {/* DISPLAY ID, NOT INTERNAL DATABASE ID */}
                      <td className="visit-id">
                        {p.display_visit_id || p.visit_id}
                      </td>

                      <td className="patient-name">
                        {p.patient_name}
                      </td>

                      <td>{p.age}</td>
                      <td>{p.gender || "-"}</td>
                      <td>{p.phone || "-"}</td>

                      <td>
                        {p.department?.toUpperCase() === "EMERGENCY" ? (
                          <span className="emergency-label">
                            EMERGENCY
                          </span>
                        ) : (
                          <span className="patient-list-dept-tag">
                            {p.department || "-"}
                          </span>
                        )}
                      </td>

                      <td className="datetime">
                        <div className="date">{registered.date}</div>
                        {registered.time && (
                          <div className="time">{registered.time}</div>
                        )}
                      </td>

                      <td>
  {p.department?.trim().toUpperCase() === "EMERGENCY"
    ? duration(
        p.registration_time,
        p.triage_start_time
      )
    : duration(
        p.registration_time,
        p.consultation_start_time
      )}
</td>
<td className="stage-waiting-cell">
  {p.department?.trim().toUpperCase() === "EMERGENCY" ? (
    <div className="stage-waiting-list">
      <div>
        <strong>Triage:</strong>{" "}
        {formatWaitingTime(
          p.triage_queue_entry_time,
          p.triage_start_time
        )}
      </div>
      <div>
        <strong>Emergency Tx:</strong>{" "}
        {formatWaitingTime(
          p.emergency_treatment_queue_entry_time,
          p.emergency_treatment_start_time
        )}
      </div>
      <div>
        <strong>Diagnostics:</strong>{" "}
        {formatWaitingTime(
          p.diagnostics_queue_entry_time,
          p.diagnostics_start_time
        )}
      </div>
      <div>
        <strong>Pharmacy:</strong>{" "}
        {formatWaitingTime(
          p.pharmacy_queue_entry_time,
          p.pharmacy_start_time
        )}
      </div>
    </div>
  ) : (
    "—"
  )}
</td>
                      <td>
                        {duration(
                          p.consultation_start_time,
                          p.consultation_end_time
                        )}
                      </td>

                      <td>
                        {duration(
                          p.diagnostics_start_time,
                          p.diagnostics_end_time
                        )}
                      </td>

                      <td>
                        {duration(
                          p.pharmacy_start_time,
                          p.pharmacy_end_time
                        )}
                      </td>

                      <td className="patient-status-column">
  <span
    className={`patient-list-status-pill ${
      ["COMPLETED", "DISCHARGED"].includes(
        (p.status || "").toUpperCase()
      )
        ? "completed"
        : "waiting"
    }`}
  >
    {p.status}
  </span>

                        {/* OP CONSULTATION */}
                        {p.status === "IN CONSULTATION" &&
                          canCompleteOP && (
                            <button
                              className="patient-list-complete"
                              onClick={() =>
                                openNextStageSelection(p.patient_id, "OP")
                              }
                            >
                              Complete
                            </button>
                          )}

                        {p.status === "IN CONSULTATION" &&
                          !canCompleteOP && (
                            <div
                              style={{
                                marginTop: "6px",
                                fontSize: "11.5px",
                                color: "#6B746F"
                              }}
                            >
                              OP staff handling
                            </div>
                          )}

                        {/* DIAGNOSTICS */}
                        {p.status === "IN DIAGNOSTICS" &&
                          canCompleteDiagnostics && (
                            <button
                              type="button"
                              className="patient-list-complete"
                              onClick={() =>
                                openNextStageSelection(
                                  p.patient_id,
                                  "DIAGNOSTICS"
                                )
                              }
                            >
                              Complete Diagnostics
                            </button>
                          )}

                        {p.status === "IN DIAGNOSTICS" &&
                          !canCompleteDiagnostics && (
                            <div
                              style={{
                                marginTop: "6px",
                                fontSize: "11.5px",
                                color: "#6B746F"
                              }}
                            >
                              Diagnostics staff handling
                            </div>
                          )}

                        {/* PHARMACY */}
                        {p.status === "IN PHARMACY" &&
                          canCompletePharmacy && (
                            <button
                              className="patient-list-complete"
                              onClick={() =>
                                updateStatus(p.patient_id, "COMPLETED")
                              }
                            >
                              Complete Pharmacy Service
                            </button>
                          )}

                        {p.status === "IN PHARMACY" &&
                          !canCompletePharmacy && (
                            <div
                              style={{
                                marginTop: "6px",
                                fontSize: "11.5px",
                                color: "#6B746F"
                              }}
                            >
                              Pharmacy staff handling
                            </div>
                          )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {/* NEXT-STAGE SELECTION MODAL */}
      {showNextStage && (
        <div
          className="patient-list-modal-overlay"
          onClick={closeNextStageSelection}
        >
          <div
            className="patient-list-modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="patient-list-modal-header">
              <div>
                <h2>What happens next?</h2>
                <p>Select the next step for this patient.</p>
              </div>

              <button
                className="patient-list-modal-close"
                onClick={closeNextStageSelection}
              >
                ×
              </button>
            </div>

            {/* OP OPTIONS */}
            {nextStageType === "OP" && (
              <div className="patient-list-next-options">
                <button
                  className="patient-list-next-option complete-option"
                  onClick={() => handleNextStage("NONE")}
                >
                  <div className="patient-list-option-title">
                    Complete Visit
                  </div>
                  <div className="patient-list-option-description">
                    The patient's hospital visit is completed.
                  </div>
                </button>

                <button
                  className="patient-list-next-option"
                  onClick={() => handleNextStage("DIAGNOSTICS")}
                >
                  <div className="patient-list-option-title">
                    Send to Diagnostics
                  </div>
                  <div className="patient-list-option-description">
                    Send the patient to the diagnostics waiting list.
                  </div>
                </button>

                <button
                  className="patient-list-next-option"
                  onClick={() => handleNextStage("PHARMACY")}
                >
                  <div className="patient-list-option-title">
                    Send to Pharmacy
                  </div>
                  <div className="patient-list-option-description">
                    Send the patient to the pharmacy waiting list.
                  </div>
                </button>
              </div>
            )}

            {/* DIAGNOSTICS OPTIONS */}
            {nextStageType === "DIAGNOSTICS" && (
              <div className="patient-list-next-options">
                <button
                  className="patient-list-next-option complete-option"
                  onClick={() => handleNextStage("NONE")}
                >
                  <div className="patient-list-option-title">
                    Complete Visit
                  </div>
                  <div className="patient-list-option-description">
                    Diagnostics is completed and the visit ends.
                  </div>
                </button>

                <button
                  className="patient-list-next-option"
                  onClick={() => handleNextStage("PHARMACY")}
                >
                  <div className="patient-list-option-title">
                    Send to Pharmacy
                  </div>
                  <div className="patient-list-option-description">
                    Send the patient to the pharmacy waiting list.
                  </div>
                </button>
              </div>
            )}

            <button
              className="patient-list-modal-cancel"
              onClick={closeNextStageSelection}
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default PatientList;