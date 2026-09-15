import { useEffect, useState } from "react";
import "./Dashboard.css";
import { API_URL } from "./config";

function Dashboard() {
  const [bottlenecks, setBottlenecks] = useState([]);
  const [visitCount, setVisitCount] = useState(0);
  const [mlPrediction, setMlPrediction] = useState(null);
  const [mlCondition, setMlCondition] = useState("");

  const [mlInput, setMlInput] = useState({
    stage: "OP",
    patient_type: "OP",
    staff_available: 1,
    staff_capacity: 2,
    resource_capacity: 3,
    resource_available: 1,
  });

  const username = localStorage.getItem("username");
  const role = localStorage.getItem("role");

  useEffect(() => {
    fetch(`${API_URL}/api/visits/count`)
  .then((response) => response.json())
  .then((data) => setVisitCount(data))
  .catch((error) =>
    console.error("Visit count error:", error)
  );

    fetch(`${API_URL}/api/bottleneck-analysis`)
      .then((response) => response.json())
      .then((data) => setBottlenecks(data))
      .catch((error) =>
        console.error("Bottleneck error:", error)
      );

    fetch(`${API_URL}/api/ml/current-input`)
      .then((response) => response.json())
      .then((data) => {
        setMlInput(data);
      })
      .catch((error) =>
        console.error("ML input error:", error)
      );
  }, []);

  const runPrediction = async () => {
    try {
      setMlPrediction(null);
      setMlCondition("Checking...");

      const response = await fetch(
  `${API_URL}/api/ml/predict-current`
);

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.message || "Prediction failed"
        );
      }

      setMlPrediction(data.prediction);
      setMlCondition(data.condition);
    } catch (error) {
      console.error("ML prediction error:", error);

      setMlPrediction(null);
      setMlCondition(
        "Prediction service unavailable"
      );
    }
  };

  const averageWaiting =
    bottlenecks.length > 0
      ? (
          bottlenecks.reduce(
            (sum, item) =>
              sum + item.averageWaitingTime,
            0
          ) / bottlenecks.length
        ).toFixed(2)
      : "0.00";

  const highestBottleneck =
    bottlenecks.length > 0
      ? bottlenecks[0]
      : null;

  const logout = () => {
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    window.location.href = "/";
  };

  // Navigation functions
  const openPatientFlow = () => {
    window.location.href = "/patient-flow";
  };

  const openDataMining = () => {
    window.location.href = "/data-mining";
  };

  const openResourceAnalysis = () => {
    window.location.href = "/resource-analysis";
  };

  const openDecisionSupport = () => {
    window.location.href = "/decision-support";
  };

  return (
    <div className="dashboard">

      {/* TOP BAR */}
      <header className="topbar">

        <div className="dashboard-brand">

          <div className="dashboard-icon">
            +
          </div>

          <div>
            <h2>Smart Hospital</h2>

            <span>
              Patient Flow Decision Support
            </span>
          </div>

        </div>

        <div className="user-section">

          <div>
            <strong>{username}</strong>
            <span>{role}</span>
          </div>

          <button onClick={logout}>
            Logout
          </button>

        </div>

      </header>


      {/* MAIN CONTENT */}
      <main className="dashboard-content">

        {/* WELCOME */}
        <div className="welcome">

          <div>

            <h1>
              Hospital Dashboard
            </h1>

            <p>
              Monitor patient flow, identify
              bottlenecks and support
              operational decisions.
            </p>

          </div>

        </div>


        {/* SUMMARY CARDS */}
        <div className="cards">

          <div className="card">

            <div className="card-title">
              Total Visits
            </div>

            <div className="card-value">
              {visitCount}
            </div>

            <div className="card-info">
              Recorded patient visits
            </div>

          </div>


          <div className="card">

            <div className="card-title">
              Average Waiting
            </div>

            <div className="card-value">
              {averageWaiting} min
            </div>

            <div className="card-info">
              Across workflow stages
            </div>

          </div>


          <div className="card">

            <div className="card-title">
              Bottleneck Stage
            </div>

            <div className="card-value">

              {highestBottleneck
                ? highestBottleneck.stage
                : "Loading..."}

            </div>

            <div className="card-info">

              {highestBottleneck
                ? `${highestBottleneck.severity} severity`
                : ""}

            </div>

          </div>


          <div className="card">

            <div className="card-title">
              System Status
            </div>

            <div className="card-value">
              Active
            </div>

            <div className="card-info">
              Backend connected
            </div>

          </div>

        </div>


        {/* QUICK ACCESS MODULES */}
        <section className="modules-section">

          <div className="section-header">

            <h2>
              Project Modules
            </h2>

            <span>
              Open project modules
            </span>

          </div>


          <div className="module-grid">

            {/* PATIENT FLOW */}
            <button
              className="module-card"
              onClick={openPatientFlow}
            >

              <div className="module-number">
                01
              </div>

              <div className="module-icon">
                👥
              </div>

              <h3>
                Patient Flow
              </h3>

              <p>
                View patient workflow events,
                timestamps and hospital stages.
              </p>

              <span className="module-link">
                Open module →
              </span>

            </button>


            {/* DATA MINING */}
            <button
              className="module-card"
              onClick={openDataMining}
            >

              <div className="module-number">
                02
              </div>

              <div className="module-icon">
                📊
              </div>

              <h3>
                Data Mining
              </h3>

              <p>
                Discover waiting-time and
                patient-arrival patterns.
              </p>

              <span className="module-link">
                Open module →
              </span>

            </button>


            {/* RESOURCE ANALYSIS */}
            {role === "ADMIN" && (
              <button
                className="module-card"
                onClick={openResourceAnalysis}
              >

                <div className="module-number">
                  03
                </div>

                <div className="module-icon">
                  👨‍⚕️
                </div>

                <h3>
                  Staff & Resources
                </h3>

                <p>
                  Monitor staff availability,
                  capacity and hospital resources.
                </p>

                <span className="module-link">
                  Open module →
                </span>

              </button>
            )}


            {/* DECISION SUPPORT */}
            <button
              className="module-card"
              onClick={openDecisionSupport}
            >

              <div className="module-number">
                04
              </div>

              <div className="module-icon">
                💡
              </div>

              <h3>
                Decision Support
              </h3>

              <p>
                Combine bottleneck analysis,
                ML prediction and operational insights.
              </p>

              <span className="module-link">
                Open module →
              </span>

            </button>

          </div>

        </section>


        {/* BOTTLENECK ANALYSIS */}
        <section className="analysis-section">

          <div className="section-header">

            <h2>
              Bottleneck Analysis
            </h2>

            <span>
              Live from hospital database
            </span>

          </div>


          <div className="table-container">

            <table>

              <thead>

                <tr>

                  <th>Stage</th>

                  <th>Average Waiting</th>

                  <th>Average Service</th>

                  <th>Severity</th>

                </tr>

              </thead>


              <tbody>

                {bottlenecks.map((item) => (

                  <tr key={item.stage}>

                    <td>
                      {item.stage}
                    </td>

                    <td>
                      {item.averageWaitingTime.toFixed(2)}
                      {" "}min
                    </td>

                    <td>
                      {item.averageServiceTime.toFixed(2)}
                      {" "}min
                    </td>

                    <td>

                      <span className="severity">
                        {item.severity}
                      </span>

                    </td>

                  </tr>

                ))}

              </tbody>

            </table>

          </div>

        </section>


        {/* DECISION SUPPORT */}
        <section className="decision-section">

          <div>

            <h2>
              Decision Support
            </h2>


            {highestBottleneck ? (

              <p>

                {mlCondition === "" ? (

                  <>
                    Current hospital staff and
                    resource conditions are loaded
                    automatically. Run the ML
                    prediction to generate
                    decision-support information.
                  </>

                ) : mlPrediction === 1 ? (

                  <>
                    The ML model predicts a{" "}
                    <strong>
                      HIGH WAITING CONDITION
                    </strong>{" "}
                    for{" "}
                    <strong>
                      {mlInput.stage}
                    </strong>.
                    Consider monitoring this stage
                    and reviewing staff and resource
                    allocation.
                  </>

                ) : (

                  <>
                    The ML model predicts a{" "}
                    <strong>
                      NORMAL WAITING CONDITION
                    </strong>{" "}
                    for{" "}
                    <strong>
                      {mlInput.stage}
                    </strong>.
                    Current conditions do not
                    indicate a high waiting risk.
                  </>

                )}

              </p>

            ) : (

              <p>
                Loading decision-support
                information...
              </p>

            )}

          </div>


          {/* ML BOX */}
          <div className="ml-box">

            <h3>
              ML Prediction
            </h3>

            <div className="current-condition">
              <span>
                Current conditions from hospital database
              </span>
            </div>


            <label>
              Stage
            </label>

            <input
              type="text"
              value={
                mlInput.stage === "OP"
                  ? "OP Consultation"
                  : mlInput.stage
              }
              readOnly
            />


            <label>
              Staff Available
            </label>

            <input
              type="number"
              value={mlInput.staff_available}
              readOnly
            />


            <label>
              Staff Capacity
            </label>

            <input
              type="number"
              value={mlInput.staff_capacity}
              readOnly
            />


            <label>
              Resource Capacity
            </label>

            <input
              type="number"
              value={mlInput.resource_capacity}
              readOnly
            />


            <label>
              Resource Available
            </label>

            <input
              type="number"
              value={mlInput.resource_available}
              readOnly
            />


            <button
              className="predict-button"
              onClick={runPrediction}
            >
              Predict Waiting Condition
            </button>


            {mlCondition && (

              <div className="prediction-result">

                <span>
                  Prediction Result
                </span>

                <strong>
                  {mlCondition}
                </strong>

              </div>

            )}

          </div>

        </section>

      </main>

    </div>
  );
}

export default Dashboard;
