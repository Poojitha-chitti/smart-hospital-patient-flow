import { useEffect, useState } from "react";
import "./Dashboard.css";
import { API_URL } from "./config";

function Dashboard() {
  const [bottlenecks, setBottlenecks] = useState([]);
  const [visitCount, setVisitCount] = useState(0);
  const [patients, setPatients] = useState([]);
  const [dataMining, setDataMining] = useState(null);
  const [mlPrediction, setMlPrediction] = useState(null);
  const [mlCondition, setMlCondition] = useState("");

  const [mlInput, setMlInput] = useState({
    stage: "OP",
    patient_type: "NORMAL",
    staff_available: 1,
    staff_capacity: 2,
    resource_capacity: 3,
    resource_available: 1,
  });

  const username = localStorage.getItem("username") || "";
  const role = localStorage.getItem("role") || "";

  useEffect(() => {
    loadDashboardData();

    const interval = setInterval(() => {
      loadPatientFlow();
      loadBottleneckAnalysis();
      loadDataMining();
    }, 10000);

    return () => clearInterval(interval);
  }, []);

  const loadDashboardData = () => {
    fetch(`${API_URL}/api/visits/count`)
      .then((response) => response.json())
      .then((data) => setVisitCount(data))
      .catch((error) =>
        console.error("Visit count error:", error)
      );

    loadBottleneckAnalysis();
    loadMLInput();
    loadPatientFlow();
    loadDataMining();
  };

  const loadBottleneckAnalysis = () => {
    fetch(`${API_URL}/api/bottleneck-analysis`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load bottleneck analysis");
        }
        return response.json();
      })
      .then((data) => setBottlenecks(data))
      .catch((error) =>
        console.error("Bottleneck error:", error)
      );
  };

  /* =====================================================
     ML INPUT
  ===================================================== */

  const loadMLInput = (
    stage = "OP",
    patientType = "NORMAL"
  ) => {
    fetch(
      `${API_URL}/api/ml/current-input?stage=${encodeURIComponent(
        stage
      )}&patientType=${encodeURIComponent(patientType)}`
    )
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load ML input");
        }
        return response.json();
      })
      .then((data) => {
        setMlInput(data);
      })
      .catch((error) =>
        console.error("ML input error:", error)
      );
  };

  const handleMLPatientTypeChange = (event) => {
    const patientType = event.target.value;

    setMlPrediction(null);
    setMlCondition("");

    setMlInput((previous) => ({
      ...previous,
      patient_type: patientType,
    }));

    loadMLInput(mlInput.stage, patientType);
  };

  const handleMLStageChange = (event) => {
    const stage = event.target.value;

    setMlPrediction(null);
    setMlCondition("");

    setMlInput((previous) => ({
      ...previous,
      stage,
    }));

    loadMLInput(stage, mlInput.patient_type);
  };

  const loadPatientFlow = () => {
    fetch(`${API_URL}/api/patients`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load patients");
        }
        return response.json();
      })
      .then((data) => {
        const patientData = Array.isArray(data)
          ? data
          : Array.isArray(data.content)
          ? data.content
          : [];

        setPatients(patientData);
      })
      .catch((error) =>
        console.error("Patient flow error:", error)
      );
  };

  const loadDataMining = () => {
    fetch(`${API_URL}/api/data-mining/patterns`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load data mining results");
        }
        return response.json();
      })
      .then((data) => setDataMining(data))
      .catch((error) =>
        console.error("Data mining error:", error)
      );
  };

  /* =====================================================
     ML PREDICTION
  ===================================================== */

  const runPrediction = async () => {
    try {
      if (!mlInput) {
        setMlCondition("Prediction input is not available");
        return;
      }

      const inputData = {
        ...mlInput,
        staff_available: Number(mlInput.staff_available),
        staff_capacity: Number(mlInput.staff_capacity),
        resource_capacity: Number(mlInput.resource_capacity),
        resource_available: Number(mlInput.resource_available),
      };

      const response = await fetch(`${API_URL}/api/ml/predict`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(inputData),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.message || data.error || "ML prediction failed"
        );
      }

      if (
        data.prediction === undefined ||
        data.condition === undefined
      ) {
        throw new Error("Invalid prediction response");
      }

      setMlPrediction(data);
      setMlCondition(data.condition);
    } catch (error) {
      console.error("ML prediction error:", error);
      setMlPrediction(null);
      setMlCondition("Prediction service unavailable");
    }
  };

  /* =====================================================
     LIVE PATIENT FLOW
  ===================================================== */

  const emergencyWaitingCount = patients.filter(
    (p) =>
      p.department?.toUpperCase() === "EMERGENCY" &&
      ["WAITING", "REGISTERED"].includes(p.status)
  ).length;

  const registrationCount = patients.filter(
    (p) =>
      ["WAITING", "REGISTERED"].includes(p.status) &&
      p.department?.toUpperCase() !== "EMERGENCY"
  ).length;

  const opCount = patients.filter(
    (p) => p.status === "IN CONSULTATION"
  ).length;

  const diagnosticsCount = patients.filter(
    (p) =>
      p.status === "DIAGNOSTICS WAITING" ||
      p.status === "IN DIAGNOSTICS"
  ).length;

  const pharmacyCount = patients.filter(
    (p) =>
      p.status === "PHARMACY WAITING" ||
      p.status === "IN PHARMACY"
  ).length;

 const completedCount = patients.filter(
  (p) => p.status === "COMPLETED" || p.status === "DISCHARGED"
).length;

  const waitingCount = patients.filter(
    (p) =>
      p.status === "WAITING" ||
      p.status === "DIAGNOSTICS WAITING" ||
      p.status === "PHARMACY WAITING"
  ).length;

  const activeCount = patients.filter(
    (p) =>
      p.status === "IN CONSULTATION" ||
      p.status === "IN DIAGNOSTICS" ||
      p.status === "IN PHARMACY"
  ).length;

  /* =====================================================
     BOTTLENECK ANALYSIS
  ===================================================== */

  const averageWaiting =
    bottlenecks.length > 0
      ? (
          bottlenecks.reduce(
            (sum, item) =>
              sum + Number(item.averageWaitingTime || 0),
            0
          ) / bottlenecks.length
        ).toFixed(2)
      : "0.00";

  const highestBottleneck =
    bottlenecks.length > 0 ? bottlenecks[0] : null;

  /* =====================================================
     DATA MINING
  ===================================================== */

  const stagePatterns = dataMining?.stagePatterns || [];
  const arrivalPatterns = dataMining?.arrivalPatterns || [];

  const apriori = dataMining?.apriori || {
    transactionCount: 0,
    minSupport: 0,
    minConfidence: 0,
    rules: [],
  };

  const aprioriRules = apriori.rules || [];

  const highestWaitingStage =
    stagePatterns.length > 0
      ? [...stagePatterns].sort(
          (a, b) =>
            Number(b.averageWaitingTime || 0) -
            Number(a.averageWaitingTime || 0)
        )[0]
      : null;

  const peakArrival =
    arrivalPatterns.length > 0
      ? [...arrivalPatterns].sort(
          (a, b) =>
            Number(b.patientCount || 0) -
            Number(a.patientCount || 0)
        )[0]
      : null;

  const highWaitingEvents = Number(
    dataMining?.highWaitingEvents || 0
  );

  /* =====================================================
     SMART BOTTLENECK RECOMMENDATION
  ===================================================== */

  const getBottleneckRecommendation = () => {
    if (!highestBottleneck) {
      return "Waiting for the latest hospital workflow analysis.";
    }

    const stage = String(
      highestBottleneck.stage || ""
    ).toUpperCase();

    const severity = String(
      highestBottleneck.severity || ""
    ).toUpperCase();

    if (severity === "HIGH") {
      if (stage === "OP") {
        return "Monitor the OP queue and check staff availability or consultation capacity.";
      }
      if (stage === "REGISTRATION") {
        return "Monitor the registration queue and check registration staff availability.";
      }
      if (stage === "PHARMACY") {
        return "Monitor the pharmacy queue and check pharmacist availability and service capacity.";
      }
      if (stage === "DIAGNOSTICS") {
        return "Monitor the diagnostics queue and check diagnostic staff and resource availability.";
      }

      return `Monitor the ${stage} stage and review staff and resource availability.`;
    }

    return `${stage} is currently being monitored based on the workflow analysis.`;
  };

  return (
    <div className="dashboard">
      {/* TOP BAR */}

      <header className="topbar">
        <div className="dashboard-brand">
          <div className="dashboard-icon">+</div>

          <div>
            <h2>Smart Hospital</h2>
            <span>Patient Flow &amp; Analysis</span>
          </div>
        </div>

        <div className="user-section">
          <div>
            <strong>{username}</strong>
            <span>{role}</span>
          </div>
        </div>
      </header>

      <main className="dashboard-content">
        <button
          className="back-home-button"
          onClick={() => {
            window.location.href = "/home";
          }}
        >
          ← Back to Home
        </button>

        {/* PAGE INTRO */}

        <section className="welcome">
          <h1>Hospital Intelligence Dashboard</h1>
          <p>
            One-page view of patient flow, bottlenecks,
            data-mining patterns, ML prediction and decision support.
          </p>
        </section>

        {/* LIVE PATIENT FLOW */}

        <section className="live-flow-section">
          <div className="live-flow-header">
            <div>
              <div className="live-title-row">
                <span className="live-dot"></span>
                <h2>Live Patient Flow</h2>
                <span className="live-badge">LIVE</span>
              </div>

              <p>
                Current patient movement across hospital workflow stages.
              </p>
            </div>

            <div className="live-refresh">
              Updates automatically
            </div>
          </div>

          <div className="flow-track">
            <div className="flow-stage">
              <div className="flow-icon">REG</div>
              <div className="flow-stage-name">Registration</div>
              <div className="flow-count">{registrationCount}</div>
              <div className="flow-label">Waiting</div>
            </div>

            <div className="flow-arrow">→</div>

            <div className="flow-stage">
              <div className="flow-icon">OP</div>
              <div className="flow-stage-name">OP Consultation</div>
              <div className="flow-count">{opCount}</div>
              <div className="flow-label">In Service</div>
            </div>

            <div className="flow-arrow">→</div>

            <div className="flow-stage">
              <div className="flow-icon">DX</div>
              <div className="flow-stage-name">Diagnostics</div>
              <div className="flow-count">{diagnosticsCount}</div>
              <div className="flow-label">Waiting / Active</div>
            </div>

            <div className="flow-arrow">→</div>

            <div className="flow-stage">
              <div className="flow-icon">RX</div>
              <div className="flow-stage-name">Pharmacy</div>
              <div className="flow-count">{pharmacyCount}</div>
              <div className="flow-label">Waiting / Active</div>
            </div>

            <div className="flow-arrow">→</div>

            <div className="flow-stage completed-stage">
              <div className="flow-icon">✓</div>
              <div className="flow-stage-name">Completed</div>
              <div className="flow-count">{completedCount}</div>
              <div className="flow-label">Finished</div>
            </div>
          </div>

          {/* LIVE SUMMARY */}

          <div className="live-summary">
            <div className="live-summary-item">
              <span className="summary-number">{completedCount}</span>
<span className="summary-text">Completed</span>
            </div>

            <div className="live-summary-item">
              <span className="summary-number">{waitingCount}</span>
              <span className="summary-text">Currently waiting</span>
            </div>

            <div className="live-summary-item">
              <span className="summary-number">{emergencyWaitingCount}</span>
              <span className="summary-text">Emergency waiting</span>
            </div>

            <div className="live-summary-item">
              <span className="summary-number">{activeCount}</span>
              <span className="summary-text">Currently in service</span>
            </div>

            <div className="live-summary-item">
              <span className="summary-number">
                {highestBottleneck ? highestBottleneck.stage : "—"}
              </span>
              <span className="summary-text">Analysis bottleneck</span>
            </div>
          </div>
        </section>
        {/* SMART BOTTLENECK ALERT */}

        <section className="summary-banner bottleneck-alert">
          <div className="alert-top">
            <div>
              <span className="alert-label">
                SMART BOTTLENECK ALERT
              </span>

              <strong>
                {highestBottleneck
                  ? `${highestBottleneck.stage} requires attention`
                  : "Waiting for workflow analysis"}
              </strong>
            </div>

            {highestBottleneck && (
              <span
                className={`alert-severity ${String(
                  highestBottleneck.severity || ""
                ).toLowerCase()}`}
              >
                {highestBottleneck.severity || "MONITOR"}
              </span>
            )}
          </div>

          <p>
            {highestBottleneck
              ? `${highestBottleneck.stage} currently has the highest average waiting time among the analyzed stages.`
              : "Waiting for the latest hospital workflow analysis."}
          </p>

          {highestBottleneck && (
            <div className="alert-details">
              <div>
                <span>Average Waiting</span>
                <strong>
                  {Number(
                    highestBottleneck.averageWaitingTime || 0
                  ).toFixed(2)}{" "}
                  min
                </strong>
              </div>

              <div>
                <span>Average Service</span>
                <strong>
                  {Number(
                    highestBottleneck.averageServiceTime || 0
                  ).toFixed(2)}{" "}
                  min
                </strong>
              </div>

              <div>
                <span>EABDA State</span>
                <strong>
                  {String(
                    highestBottleneck.eabdaState || "NORMAL"
                  ).toUpperCase()}
                </strong>
              </div>
            </div>
          )}

          <div className="alert-recommendation">
            <span>System Recommendation</span>
            <p>{getBottleneckRecommendation()}</p>
          </div>
        </section>

        {/* SUMMARY CARDS */}

        <section className="cards">
          <div className="card">
            <div className="card-title">Total Visits</div>
            <div className="card-value">{visitCount}</div>
            <div className="card-info">Recorded patient visits</div>
          </div>

          <div className="card">
            <div className="card-title">Average Waiting</div>
            <div className="card-value">{averageWaiting} min</div>
            <div className="card-info">
              Average across analyzed stages
            </div>
          </div>

          <div className="card">
            <div className="card-title">Bottleneck Stage</div>
            <div className="card-value">
              {highestBottleneck
                ? highestBottleneck.stage
                : "Loading..."}
            </div>
            <div className="card-info">
              {highestBottleneck
                ? `${highestBottleneck.severity || ""} severity`
                : "Analyzing workflow"}
            </div>
          </div>

          <div className="card">
            <div className="card-title">System Status</div>
            <div className="card-value">Active</div>
            <div className="card-info">Backend connected</div>
          </div>
        </section>

        {/* BOTTLENECK ANALYSIS */}

        <section className="analysis-section">
          <div className="section-header">
            <div>
              <h2>Bottleneck Analysis</h2>
              <p>
                Waiting time, service time and EABDA state across
                hospital stages.
              </p>
            </div>
          </div>

          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Stage</th>
                  <th>Average Wait</th>
                  <th>Average Service</th>
                  <th>Severity</th>
                  <th>EABDA State</th>
                  <th>Trend</th>
                </tr>
              </thead>

              <tbody>
                {bottlenecks.length > 0 ? (
                  bottlenecks.map((item) => {
                    const state = String(
                      item.eabdaState || "NORMAL"
                    ).toUpperCase();

                    return (
                      <tr key={item.stage}>
                        <td className="stage-name">
                          {item.stage}
                        </td>

                        <td>
                          {Number(
                            item.averageWaitingTime || 0
                          ).toFixed(2)}{" "}
                          min
                        </td>

                        <td>
                          {Number(
                            item.averageServiceTime || 0
                          ).toFixed(2)}{" "}
                          min
                        </td>

                        <td>
                          <span
                            className={`severity-pill ${String(
                              item.severity || ""
                            ).toLowerCase()}`}
                          >
                            {item.severity || "—"}
                          </span>
                        </td>

                        <td>
                          <span
                            className={`eabda-pill eabda-${state.toLowerCase()}`}
                          >
                            {state}
                          </span>
                        </td>

                        <td>
                          {Number(item.trend ?? 0).toFixed(2)} min
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan="6" style={{ textAlign: "center" }}>
                      Waiting for bottleneck analysis...
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        {/* DATA MINING INSIGHTS */}

        <section className="insights-section">
          <div className="section-header">
            <h2>Data Mining Insights</h2>
            <p>
              Important patterns discovered from hospital workflow data.
            </p>
          </div>

          <div className="insight-grid">
            <div className="insight-card">
              <div className="insight-label">HIGH-WAIT EVENTS</div>
              <div className="insight-value">{highWaitingEvents}</div>
              <p>
                Workflow events identified with high waiting time.
              </p>
            </div>

            <div className="insight-card">
              <div className="insight-label">
                HIGHEST WAITING STAGE
              </div>
              <div className="insight-value">
                {highestWaitingStage
                  ? highestWaitingStage.stage
                  : "—"}
              </div>
              <p>
                {highestWaitingStage
                  ? `${Number(
                      highestWaitingStage.averageWaitingTime || 0
                    ).toFixed(2)} min average waiting time.`
                  : "Waiting for stage pattern analysis."}
              </p>
            </div>

            <div className="insight-card">
              <div className="insight-label">PEAK ARRIVAL PERIOD</div>
              <div className="insight-value">
                {peakArrival ? `${peakArrival.hour}:00` : "—"}
              </div>
              <p>
                {peakArrival
                  ? `${peakArrival.patientCount} patient arrivals observed.`
                  : "Waiting for arrival pattern analysis."}
              </p>
            </div>

            <div className="insight-card">
              <div className="insight-label">APRIORI RULES</div>
              <div className="insight-value">
                {aprioriRules.length}
              </div>
              <p>
                Association rules discovered from workflow transactions.
              </p>
            </div>
          </div>

          {aprioriRules.length > 0 && (
            <div className="apriori-highlight">
              <div>
                <strong>Apriori Pattern</strong>
                <p>
                  {aprioriRules[0].antecedent
                    ? `${aprioriRules[0].antecedent} → ${aprioriRules[0].consequent}`
                    : "Association pattern detected from workflow data."}
                </p>
              </div>

              <div className="apriori-metrics">
                <span>
                  Support:{" "}
                  {Number(aprioriRules[0].support || 0).toFixed(2)}
                </span>
                <span>
                  Confidence:{" "}
                  {Number(
                    aprioriRules[0].confidence || 0
                  ).toFixed(2)}
                </span>
              </div>
            </div>
          )}
        </section>
        {/* ML PREDICTION */}

        <section className="prediction-section">
          <div className="prediction-intro">
            <h2>ML Waiting-Time Prediction</h2>
            <p>
              Select a hospital stage and patient type to check
              the current staff and resource conditions.
            </p>
          </div>

          <div className="ml-box">
            <div className="current-condition">
              Current conditions from hospital data
            </div>

            <div className="ml-grid">
              <div className="ml-field">
                <label>Select Stage</label>
                <select
                  value={mlInput.stage}
                  onChange={handleMLStageChange}
                >
                  <option value="REGISTRATION">Registration</option>
                  <option value="OP">OP Consultation</option>
                  <option value="DIAGNOSTICS">Diagnostics</option>
                  <option value="PHARMACY">Pharmacy</option>
                </select>
              </div>

              <div className="ml-field">
                <label>Patient Type</label>
                <select
                  value={mlInput.patient_type}
                  onChange={handleMLPatientTypeChange}
                >
                  <option value="NORMAL">Normal</option>
                  <option value="EMERGENCY">Emergency</option>
                </select>
              </div>

              <div className="ml-field">
                <label>Staff Available</label>
                <input
                  type="text"
                  value={`${mlInput.staff_available ?? 0} available`}
                  readOnly
                />
              </div>

              <div className="ml-field">
                <label>Staff Capacity</label>
                <input
                  type="text"
                  value={`${mlInput.staff_capacity ?? 0} total capacity`}
                  readOnly
                />
              </div>

              <div className="ml-field">
                <label>Resource Available</label>
                <input
                  type="text"
                  value={`${mlInput.resource_available ?? 0} available`}
                  readOnly
                />
              </div>

              <div className="ml-field">
                <label>Resource Capacity</label>
                <input
                  type="text"
                  value={`${mlInput.resource_capacity ?? 0} total capacity`}
                  readOnly
                />
              </div>
            </div>

            <div className="ml-explanation">
              <strong>What do these values mean?</strong>
              <p>
                Available means the staff or resource currently
                available for this stage. Capacity means the total
                available capacity of that stage.
              </p>
            </div>

            <button
              className="predict-button"
              onClick={runPrediction}
            >
              Predict Waiting Condition
            </button>

            {mlCondition && (
              <div className="prediction-result">
                <span>Prediction Result</span>
                <strong>{mlCondition}</strong>
              </div>
            )}
          </div>
        </section>

        {/* DECISION SUPPORT */}

        <section className="decision-section">
          <div className="section-header">
            <h2>Decision Support</h2>
            <p>
              Operational information generated from workflow analysis.
            </p>
          </div>

          <div className="decision-card">
            <div className="decision-main">
              <div className="decision-icon">!</div>
              <div>
                <span className="decision-label">
                  OBSERVED BOTTLENECK
                </span>
                <h3>
                  {highestBottleneck
                    ? highestBottleneck.stage
                    : "—"}
                </h3>
              </div>
            </div>

            <div className="decision-details">
              <div>
                <span>Severity</span>
                <strong>
                  {highestBottleneck?.severity || "—"}
                </strong>
              </div>

              <div>
                <span>EABDA State</span>
                <strong>
                  {String(
                    highestBottleneck?.eabdaState || "NORMAL"
                  ).toUpperCase()}
                </strong>
              </div>

              <div>
                <span>ML Condition</span>
                <strong>{mlCondition || "Not predicted"}</strong>
              </div>
            </div>

            <div className="decision-recommendation">
              <span>Recommended Action</span>
              <p>{getBottleneckRecommendation()}</p>
            </div>
          </div>
        </section>

        {/* FINAL SYSTEM SUMMARY */}

        <section className="final-summary">
          <div>
            <span className="final-summary-label">
              SMART HOSPITAL
            </span>

            <h2>
              From Patient Data to Operational Decision
            </h2>

            <p>
              The system monitors patient flow, analyzes bottlenecks,
              discovers workflow patterns, applies machine learning
              and presents decision-support information in one place.
            </p>
          </div>

          <div className="final-flow">
            <span>Patient Data</span>
            <b>→</b>
            <span>Analysis</span>
            <b>→</b>
            <span>ML</span>
            <b>→</b>
            <span>Decision</span>
          </div>
        </section>
      </main>
    </div>
  );
}

export default Dashboard;