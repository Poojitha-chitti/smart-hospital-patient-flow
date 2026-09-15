import { useEffect, useState } from "react";
import "./PatientFlow.css";
import { API_URL } from "./config";

function PatientFlow() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${API_URL}/api/workflow-events`)
      .then((response) => response.json())
      .then((data) => {
        setEvents(data);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Workflow events error:", error);
        setLoading(false);
      });
  }, []);

  return (
    <div className="patient-flow-page">

      <div className="flow-header">
        <div>
          <h1>Patient Flow</h1>
          <p>
            Monitor patient movement and workflow events across hospital
            stages.
          </p>
        </div>

        <button
          className="back-button"
          onClick={() => {
            window.location.href = "/dashboard";
          }}
        >
          ← Dashboard
        </button>
      </div>

      <div className="flow-path">
        <div className="flow-stage">
          <span>1</span>
          <strong>Registration</strong>
        </div>

        <div className="flow-arrow">→</div>

        <div className="flow-stage">
          <span>2</span>
          <strong>OP Consultation</strong>
        </div>

        <div className="flow-arrow">→</div>

        <div className="flow-stage">
          <span>3</span>
          <strong>Pharmacy</strong>
        </div>

        <div className="flow-arrow">→</div>

        <div className="flow-stage">
          <span>4</span>
          <strong>Exit</strong>
        </div>
      </div>

      <div className="flow-summary">

        <div className="flow-card">
          <span>Total Workflow Events</span>
          <strong>{events.length}</strong>
        </div>

        <div className="flow-card">
          <span>Registration Events</span>
          <strong>
            {events.filter((event) => event.stage === "REGISTRATION").length}
          </strong>
        </div>

        <div className="flow-card">
          <span>OP Events</span>
          <strong>
            {events.filter((event) => event.stage === "OP").length}
          </strong>
        </div>

        <div className="flow-card">
          <span>Pharmacy Events</span>
          <strong>
            {events.filter((event) => event.stage === "PHARMACY").length}
          </strong>
        </div>

      </div>

      <div className="events-section">

        <div className="events-heading">
          <h2>Workflow Events</h2>
          <span>Data from hospital database</span>
        </div>

        {loading ? (
          <p className="loading-text">Loading workflow data...</p>
        ) : events.length === 0 ? (
          <p className="loading-text">No workflow events found.</p>
        ) : (
          <div className="events-table">

            <table>
              <thead>
                <tr>
                  <th>Event ID</th>
                  <th>Visit ID</th>
                  <th>Stage</th>
                  <th>Queue Entry</th>
                  <th>Service Start</th>
                  <th>Service End</th>
                  <th>Staff ID</th>
                </tr>
              </thead>

              <tbody>
                {events.slice(0, 50).map((event) => (
                  <tr key={event.event_id}>
                    <td>{event.event_id}</td>
                    <td>{event.visit_id}</td>
                    <td>
                      <span className="stage-badge">
                        {event.stage}
                      </span>
                    </td>
                    <td>{event.queue_entry_time}</td>
                    <td>{event.service_start_time || "-"}</td>
                    <td>{event.service_end_time || "-"}</td>
                    <td>{event.staff_id || "-"}</td>
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

export default PatientFlow;
