import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./WaitingList.css";

function WaitingList() {
  const [patients, setPatients] = useState([]);
  const [workArea, setWorkArea] = useState("");
  const [loading, setLoading] = useState(true);
  const [workAreaLoading, setWorkAreaLoading] = useState(true);
  const [error, setError] = useState("");

  const username = localStorage.getItem("username") || "";
  const role = (localStorage.getItem("role") || "").trim().toUpperCase();

  const normalizedStatus = (patient) =>
    (patient.status || "").trim().toUpperCase();

  const normalizedWorkArea = workArea.trim().toUpperCase();

  const isEmergencyPatient = (patient) =>
    (patient.department || "").trim().toUpperCase() === "EMERGENCY";

  const loadWorkArea = async () => {
    try {
      setWorkAreaLoading(true);

      if (role === "ADMIN") {
        setWorkArea("ALL");
        return;
      }

      const response = await fetch(`${API_URL}/api/admin/users/staff`);

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
        throw new Error("Unable to load patients.");
      }

      const data = await response.json();
      setPatients(Array.isArray(data) ? data : []);
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

  const updateStatus = async (patientId, status, nextStage) => {
    try {
      setError("");

      const requestBody = { status, username };

      if (nextStage) {
        requestBody.nextStage = nextStage;
      }

      const response = await fetch(
        `${API_URL}/api/patients/${patientId}/status`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(requestBody),
        }
      );

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(
          data.message || "Unable to update patient status."
        );
      }

      await loadPatients();
    } catch (err) {
      setError(err.message || "Unable to update patient status.");
    }
  };

  const isAdmin = role === "ADMIN";
  const isOPStaff = role === "STAFF" && normalizedWorkArea === "OP";
  const isRegistrationStaff =
    role === "STAFF" && normalizedWorkArea === "REGISTRATION";
  const isPharmacyStaff =
    role === "STAFF" && normalizedWorkArea === "PHARMACY";
  const isDiagnosticsStaff =
    role === "STAFF" && normalizedWorkArea === "DIAGNOSTICS";
  const isEmergencyStaff =
    role === "STAFF" &&
    ["EMERGENCY", "CASUALTY"].includes(normalizedWorkArea);

  const sortNewestFirst = (list) =>
    [...list].sort((a, b) => (b.patient_id || 0) - (a.patient_id || 0));

  const emergencyStatuses = [
    "TRIAGE WAITING",
    "IN TRIAGE",
    "EMERGENCY TREATMENT WAITING",
    "IN EMERGENCY TREATMENT",
  ];

  const emergencyActivePatients = sortNewestFirst(
    patients.filter(
      (patient) =>
        isEmergencyPatient(patient) &&
        emergencyStatuses.includes(normalizedStatus(patient))
    )
  );

  const opWaitingPatients = sortNewestFirst(
    patients.filter(
      (patient) =>
        normalizedStatus(patient) === "WAITING" &&
        !isEmergencyPatient(patient)
    )
  );

  // Include both waiting and in-service patients so they remain
  // visible after the Start button is clicked.
  const pharmacyPatients = sortNewestFirst(
    patients.filter((patient) =>
      ["PHARMACY WAITING", "IN PHARMACY"].includes(
        normalizedStatus(patient)
      )
    )
  );

  const diagnosticsPatients = sortNewestFirst(
    patients.filter((patient) =>
      ["DIAGNOSTICS WAITING", "IN DIAGNOSTICS"].includes(
        normalizedStatus(patient)
      )
    )
  );

  let visiblePatients = opWaitingPatients;
  let showPharmacy = false;
  let showDiagnostics = false;
  let showEmergency = false;

  if (isAdmin) {
    visiblePatients = sortNewestFirst([
      ...patients.filter(
        (patient) =>
          isEmergencyPatient(patient) &&
          emergencyStatuses.includes(normalizedStatus(patient))
      ),
      ...opWaitingPatients,
      ...diagnosticsPatients,
      ...pharmacyPatients,
    ]);

    // Remove duplicates if a patient happens to match more than one list.
    visiblePatients = visiblePatients.filter(
      (patient, index, list) =>
        list.findIndex(
          (item) => item.patient_id === patient.patient_id
        ) === index
    );

    showEmergency = true;
  } else if (isEmergencyStaff) {
    visiblePatients = emergencyActivePatients;
    showEmergency = true;
  } else if (isPharmacyStaff) {
    visiblePatients = pharmacyPatients;
    showPharmacy = true;
  } else if (isDiagnosticsStaff) {
    visiblePatients = diagnosticsPatients;
    showDiagnostics = true;
  }

  const renderAction = (patient) => {
    const status = normalizedStatus(patient);
    const emergency = isEmergencyPatient(patient);

    // Pharmacy actions are available to Pharmacy staff and Admin,
    // including for Emergency patients.
    if (status === "PHARMACY WAITING") {
      if (isAdmin || isPharmacyStaff) {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "IN PHARMACY")
            }
          >
            Start Pharmacy Service
          </button>
        );
      }

      return (
        <span className="waiting-stage-text">
          Waiting for Pharmacy staff
        </span>
      );
    }

    if (status === "IN PHARMACY") {
      if (isAdmin || isPharmacyStaff) {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "COMPLETED")
            }
          >
            Complete Pharmacy Service
          </button>
        );
      }

      return (
        <span className="waiting-stage-text">
          Pharmacy service in progress
        </span>
      );
    }

    // Diagnostics actions are available to Diagnostics staff and Admin.
    if (status === "DIAGNOSTICS WAITING") {
      if (isAdmin || isDiagnosticsStaff) {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "IN DIAGNOSTICS")
            }
          >
            Start Diagnostics
          </button>
        );
      }

      return (
        <span className="waiting-stage-text">
          Waiting for Diagnostics staff
        </span>
      );
    }

    if (status === "IN DIAGNOSTICS") {
      if (isAdmin || isDiagnosticsStaff) {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "COMPLETED")
            }
          >
            Complete Diagnostics
          </button>
        );
      }

      return (
        <span className="waiting-stage-text">
          Diagnostics service in progress
        </span>
      );
    }

    // Emergency-specific triage and treatment actions.
    if (emergency) {
      if (!isAdmin && !isEmergencyStaff) {
        return (
          <span className="waiting-stage-text">
            Emergency staff handling
          </span>
        );
      }

      if (status === "TRIAGE WAITING") {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "IN TRIAGE")
            }
          >
            Start Triage
          </button>
        );
      }

      if (status === "IN TRIAGE") {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "COMPLETED")
            }
          >
            Complete Triage
          </button>
        );
      }

      if (status === "EMERGENCY TREATMENT WAITING") {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "IN EMERGENCY TREATMENT")
            }
          >
            Start Emergency Treatment
          </button>
        );
      }

      if (status === "IN EMERGENCY TREATMENT") {
        return (
          <div className="emergency-actions">
            <button
              className="waiting-start-btn"
              onClick={() =>
                updateStatus(
                  patient.patient_id,
                  "DIAGNOSTICS WAITING"
                )
              }
            >
              Send to Diagnostics
            </button>

            <button
              className="waiting-start-btn"
              onClick={() =>
                updateStatus(patient.patient_id, "PHARMACY WAITING")
              }
            >
              Send to Pharmacy
            </button>

            <button
              className="waiting-start-btn"
              onClick={() =>
                updateStatus(patient.patient_id, "ADMITTED")
              }
            >
              Admit
            </button>

            <button
              className="waiting-start-btn"
              onClick={() =>
                updateStatus(patient.patient_id, "DISCHARGED")
              }
            >
              Discharge
            </button>

            <button
              className="waiting-start-btn"
              onClick={() =>
                updateStatus(patient.patient_id, "REFERRED")
              }
            >
              Refer
            </button>

            <button
              className="waiting-start-btn"
              onClick={() =>
                updateStatus(patient.patient_id, "TRANSFERRED")
              }
            >
              Transfer
            </button>
          </div>
        );
      }

      return (
        <span className="waiting-stage-text">{patient.status}</span>
      );
    }

    // Normal OP workflow.
    if (isAdmin) {
      if (status === "WAITING") {
        return (
          <button
            className="waiting-start-btn"
            onClick={() =>
              updateStatus(patient.patient_id, "IN CONSULTATION")
            }
          >
            Start Consultation
          </button>
        );
      }

      return (
        <span className="waiting-stage-text">{patient.status}</span>
      );
    }

    if (isOPStaff && status === "WAITING") {
      return (
        <button
          className="waiting-start-btn"
          onClick={() =>
            updateStatus(patient.patient_id, "IN CONSULTATION")
          }
        >
          Start Consultation
        </button>
      );
    }

    return (
      <span className="waiting-stage-text">
        Waiting for OP staff
      </span>
    );
  };

  if (loading || workAreaLoading) {
    return (
      <div className="waiting-page">
        <div className="waiting-container">
          <div className="waiting-empty">Loading waiting list...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="waiting-page">
        <div className="waiting-container">
          <div className="waiting-empty waiting-error">{error}</div>
          <button
            className="waiting-start-btn"
            onClick={() => {
              loadWorkArea();
              loadPatients();
            }}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="waiting-page">
      <div className="waiting-container">
        <div className="waiting-header">
          <div>
            <h1>
              {isAdmin
                ? "Hospital Waiting List"
                : showEmergency
                ? "Emergency Waiting List"
                : showPharmacy
                ? "Pharmacy Waiting List"
                : showDiagnostics
                ? "Diagnostics Waiting List"
                : "Waiting List"}
            </h1>

            <p>
              {isAdmin
                ? "Monitor active patients across hospital workflow stages."
                : showEmergency
                ? "Manage triage and emergency treatment stages."
                : showPharmacy
                ? "Manage patients waiting for or receiving Pharmacy service."
                : showDiagnostics
                ? "Manage patients waiting for or receiving Diagnostics service."
                : "Patients waiting for OP consultation."}
            </p>
          </div>

          <button
  className="waiting-home-btn"
  onClick={() => (window.location.href = "/home?view=patients")}
>
  Back
</button>
        </div>

        <div className="waiting-work-area">
          <strong>Your Work Area:</strong>{" "}
          {isAdmin
            ? "Administrator"
            : isEmergencyStaff
            ? "Emergency / Casualty"
            : isOPStaff
            ? "OP Consultation"
            : isRegistrationStaff
            ? "Registration"
            : isPharmacyStaff
            ? "Pharmacy"
            : isDiagnosticsStaff
            ? "Diagnostics"
            : "Not Assigned"}
        </div>

        {isRegistrationStaff && (
          <div className="waiting-info">
            <strong>Registration staff:</strong> You can monitor patients
            waiting for regular OP consultation. Emergency patients follow
            the separate Emergency workflow.
          </div>
        )}

        {isEmergencyStaff && (
          <div className="waiting-info emergency-info">
            <strong>Emergency workflow:</strong> Record triage and treatment
            timestamps using the action buttons. Clinical priority and
            treatment decisions remain with authorized clinical staff.
          </div>
        )}

        {isPharmacyStaff && (
          <div className="waiting-info">
            <strong>Pharmacy staff:</strong> Patients remain visible while
            waiting for or receiving Pharmacy service. Complete the service
            after it has finished.
          </div>
        )}

        {isDiagnosticsStaff && (
          <div className="waiting-info">
            <strong>Diagnostics staff:</strong> Patients remain visible while
            waiting for or receiving Diagnostics service.
          </div>
        )}

        {isAdmin && emergencyActivePatients.length > 0 && (
          <div className="waiting-info emergency-info">
            <strong>Active Emergency patients:</strong>{" "}
            {emergencyActivePatients.length} patient(s) are in the separate
            Emergency workflow.
          </div>
        )}

        {visiblePatients.length === 0 ? (
          <div className="waiting-empty">
            {isAdmin
              ? "No patients are currently waiting at an active workflow stage."
              : showEmergency
              ? "No active Emergency patients."
              : showPharmacy
              ? "No patients are waiting for or receiving Pharmacy service."
              : showDiagnostics
              ? "No patients are waiting for or receiving Diagnostics service."
              : "No patients are currently waiting for OP consultation."}
          </div>
        ) : (
          <div className="waiting-table-wrapper">
            <table className="waiting-table">
              <thead>
                <tr>
                  <th>Visit ID</th>
                  <th>Patient</th>
                  <th>Age</th>
                  <th>Gender</th>
                  <th>Department</th>
                  <th>Status</th>
                  <th>Registered</th>
                  <th>Waiting Time (min)</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {visiblePatients.map((patient) => (
                  <tr key={patient.patient_id}>
                    <td>
                      {patient.display_visit_id || patient.visit_id}
                    </td>
                    <td>
                      <strong>{patient.patient_name}</strong>
                    </td>
                    <td>{patient.age}</td>
                    <td>{patient.gender || "-"}</td>
                    <td>
                      {isEmergencyPatient(patient) ? (
                        <span className="emergency-label">EMERGENCY</span>
                      ) : (
                        patient.department || "-"
                      )}
                    </td>
                    <td>{patient.status || "-"}</td>
                    <td>
                      {patient.registration_time
                        ? new Date(
                            patient.registration_time
                          ).toLocaleString()
                        : "-"}
                    </td>
                    <td>
                      {patient.waiting_time_minutes != null
                        ? `${patient.waiting_time_minutes} min`
                        : "-"}
                    </td>
                    <td>{renderAction(patient)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default WaitingList;