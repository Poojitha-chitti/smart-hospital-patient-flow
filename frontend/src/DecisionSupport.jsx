import { useEffect, useState } from "react";
import "./DecisionSupport.css";
import { API_URL } from "./config";

function DecisionSupport() {
  const [bottlenecks, setBottlenecks] = useState([]);
  const [mlResult, setMlResult] = useState(null);
  const [mlInput, setMlInput] = useState(null);

  const [selectedStage, setSelectedStage] = useState("OP");
  const [selectedPatientType, setSelectedPatientType] =
    useState("NORMAL");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    const loadDecisionData = async () => {
      setLoading(true);
      setError("");
      setMlResult(null);

      try {
        const [
          bottleneckResponse,
          inputResponse,
        ] = await Promise.all([
          fetch(`${API_URL}/api/bottleneck-analysis`),

          fetch(
            `${API_URL}/api/ml/current-input?stage=${encodeURIComponent(
              selectedStage
            )}&patientType=${encodeURIComponent(
              selectedPatientType
            )}`
          ),
        ]);

        if (!bottleneckResponse.ok || !inputResponse.ok) {
          throw new Error(
            "Unable to load decision-support data."
          );
        }

        const bottleneckData =
          await bottleneckResponse.json();

        const inputData = await inputResponse.json();

        if (cancelled) return;

        setBottlenecks(
          Array.isArray(bottleneckData)
            ? bottleneckData
            : []
        );

        setMlInput(inputData);

        // Request a prediction using the selected
        // stage and patient type conditions.
        const mlResponse = await fetch(
          `${API_URL}/api/ml/predict`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify(inputData),
          }
        );

        if (!mlResponse.ok) {
          throw new Error("ML prediction failed.");
        }

        const mlData = await mlResponse.json();

        if (cancelled) return;

        setMlResult(mlData);
      } catch (err) {
        if (cancelled) return;

        console.error("Decision support error:", err);

        setError(
          "Unable to prepare decision-support information. Please check the backend and ML service."
        );
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadDecisionData();

    return () => {
      cancelled = true;
    };
  }, [selectedStage, selectedPatientType]);

  const mainBottleneck =
    bottlenecks.length > 0 ? bottlenecks[0] : null;

  const isHighWaiting = mlResult?.prediction === 1;

  const handleStageChange = (event) => {
    setSelectedStage(event.target.value);
  };

  const handlePatientTypeChange = (event) => {
    setSelectedPatientType(event.target.value);
  };

  if (loading) {
    return (
      <div className="decision-page">
        <button
          className="back-home-button"
          onClick={() => {
            window.location.href = "/home";
          }}
        >
          ← Back to Home
        </button>

        <div className="decision-loading">
          Preparing decision-support information...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="decision-page">
        <button
          className="back-home-button"
          onClick={() => {
            window.location.href = "/home";
          }}
        >
          ← Back to Home
        </button>

        <div className="decision-loading">
          {error}
        </div>

        <button
          className="decision-back"
          onClick={() => {
            setError("");
            setLoading(true);
            setSelectedStage((current) => current);
          }}
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="decision-page">
      <button
        className="back-home-button"
        onClick={() => {
          window.location.href = "/home";
        }}
      >
        ← Back to Home
      </button>

      {/* HEADER */}

      <header className="decision-header">
        <div>
          <div className="decision-brand">
            SMART HOSPITAL
          </div>

          <h1>Decision Support</h1>

          <p>
            Convert patient-flow analysis and ML predictions
            into actionable operational information.
          </p>
        </div>

        <button
          className="decision-back"
          onClick={() => {
            window.location.href = "/dashboard";
          }}
        >
          ← Dashboard
        </button>
      </header>

      {/* STAGE AND PATIENT TYPE SELECTION */}

      <section className="stage-selection">
        <div>
          <span className="decision-label">
            ANALYSIS SETTINGS
          </span>

          <h2>Select Workflow Conditions</h2>

          <p>
            Choose a workflow stage and patient type for
            the ML waiting-condition prediction.
          </p>
        </div>

        <select
          aria-label="Workflow stage"
          value={selectedStage}
          onChange={handleStageChange}
        >
          <option value="REGISTRATION">
            Registration
          </option>

          <option value="OP">
            OP Consultation
          </option>

          <option value="DIAGNOSTICS">
            Diagnostics
          </option>

          <option value="PHARMACY">
            Pharmacy
          </option>
        </select>

        <select
          aria-label="Patient type"
          value={selectedPatientType}
          onChange={handlePatientTypeChange}
        >
          <option value="NORMAL">
            Normal Patient
          </option>

          <option value="EMERGENCY">
            Emergency Patient
          </option>
        </select>
      </section>

      {/* PRIMARY BOTTLENECK */}

      {mainBottleneck && (
        <section className="decision-main">
          <div className="decision-main-heading">
            <div>
              <span className="decision-label">
                PRIMARY BOTTLENECK
              </span>

              <h2>{mainBottleneck.stage}</h2>

              <p>
                This stage currently has the highest average
                waiting time. EABDA state:{" "}
                <strong>
                  {mainBottleneck.eabdaState || "NORMAL"}
                </strong>.
              </p>
            </div>

            <span
              className={`severity ${
                (
                  mainBottleneck.severity || "low"
                ).toLowerCase()
              }`}
            >
              {mainBottleneck.severity || "LOW"}
            </span>
          </div>

          <div className="decision-metrics">
            <div>
              <span>Average Waiting</span>

              <strong>
                {Number(
                  mainBottleneck.averageWaitingTime || 0
                ).toFixed(2)}{" "}
                min
              </strong>
            </div>

            <div>
              <span>Average Service</span>

              <strong>
                {Number(
                  mainBottleneck.averageServiceTime || 0
                ).toFixed(2)}{" "}
                min
              </strong>
            </div>

            <div>
              <span>EABDA State</span>

              <strong>
                {mainBottleneck.eabdaState || "NORMAL"}
              </strong>
            </div>

            <div>
              <span>ML Prediction</span>

              <strong>
                {mlResult?.condition || "Unavailable"}
              </strong>
            </div>
          </div>
        </section>
      )}

      {/* DECISION CARDS */}

      <section className="decision-grid">
        {/* OBSERVED BOTTLENECK */}

        <div className="decision-card">
          <div className="card-number">01</div>

          <div>
            <h2>Observed Bottleneck</h2>

            <p>
              {mainBottleneck ? (
                <>
                  <strong>
                    {mainBottleneck.stage}
                  </strong>{" "}
                  has the highest average waiting time.
                  EABDA currently classifies this stage as{" "}
                  <strong>
                    {mainBottleneck.eabdaState || "NORMAL"}
                  </strong>.
                </>
              ) : (
                "No bottleneck information available."
              )}
            </p>
          </div>
        </div>

        {/* PREDICTED CONDITION */}

        <div className="decision-card">
          <div className="card-number">02</div>

          <div>
            <h2>Predicted Condition</h2>

            <p>
              {mlResult ? (
                <>
                  The ML model predicts a{" "}
                  <strong>
                    {mlResult.condition ||
                      (
                        mlResult.prediction === 1
                          ? "HIGH WAITING CONDITION"
                          : "NORMAL WAITING CONDITION"
                      )}
                  </strong>{" "}
                  for the selected{" "}
                  <strong>{selectedPatientType}</strong>{" "}
                  patient type at{" "}
                  <strong>{selectedStage}</strong>.
                </>
              ) : (
                "ML prediction information is unavailable."
              )}
            </p>
          </div>
        </div>

        {/* OPERATIONAL RECOMMENDATION */}

        <div className="decision-card">
          <div className="card-number">03</div>

          <div>
            <h2>Operational Recommendation</h2>

            <p>
              {isHighWaiting ? (
                <>
                  Review the{" "}
                  <strong>{selectedStage}</strong> queue,
                  staff availability, and resource allocation
                  to help manage the predicted high waiting
                  condition for the selected patient type.
                  Authorized hospital staff should determine
                  the appropriate action.
                </>
              ) : mlResult ? (
                <>
                  Continue monitoring the{" "}
                  <strong>{selectedStage}</strong> workflow.
                  The model does not currently predict a high
                  waiting condition for this input. Continue
                  following hospital triage and priority
                  procedures where applicable.
                </>
              ) : (
                "A recommendation is unavailable because no ML prediction was returned."
              )}
            </p>
          </div>
        </div>

        {/* DECISION BASIS */}

        <div className="decision-card">
          <div className="card-number">04</div>

          <div>
            <h2>Decision Basis</h2>

            <p>
              The page combines bottleneck analysis, EABDA
              temporal diagnosis, and the ML prediction based
              on the selected stage and patient type.
              Staff and resource information is displayed
              below when supplied by the backend.
            </p>
          </div>
        </div>
      </section>

      {/* EABDA DIAGNOSIS */}

      {mainBottleneck && (
        <section className="decision-note">
          <div className="note-icon">E</div>

          <div>
            <h2>EABDA Bottleneck Diagnosis</h2>

            <p>
              State:{" "}
              <strong>
                {mainBottleneck.eabdaState || "NORMAL"}
              </strong>
              {" • "}
              Baseline:{" "}
              <strong>
                {Number(
                  mainBottleneck.baselineWaitingTime || 0
                ).toFixed(2)}{" "}
                min
              </strong>
              {" • "}
              Waiting pressure:{" "}
              <strong>
                {Number(
                  mainBottleneck.waitingPressure || 1
                ).toFixed(2)}
                ×
              </strong>
              {" • "}
              Persistence:{" "}
              <strong>
                {mainBottleneck.persistenceCount || 0}
              </strong>
            </p>

            <p>
              <strong>Evidence:</strong>{" "}
              {mainBottleneck.evidence ||
                "No additional evidence available."}
            </p>
          </div>
        </section>
      )}

      {/* CURRENT CONDITIONS */}

      {mlInput && (
        <section className="decision-note">
          <div className="note-icon">i</div>

          <div>
            <h2>Current Conditions Used</h2>

            <p>
              Stage:{" "}
              <strong>
                {mlInput.stage || selectedStage}
              </strong>
              {" • "}
              Patient type:{" "}
              <strong>
                {mlInput.patient_type ||
                  mlInput.patientType ||
                  selectedPatientType}
              </strong>
              {" • "}
              Staff available:{" "}
              <strong>
                {mlInput.staff_available ?? "N/A"}
              </strong>
              {" • "}
              Staff capacity:{" "}
              <strong>
                {mlInput.staff_capacity ?? "N/A"}
              </strong>
              {" • "}
              Resource capacity:{" "}
              <strong>
                {mlInput.resource_capacity ?? "N/A"}
              </strong>
              {" • "}
              Resource available:{" "}
              <strong>
                {mlInput.resource_available ?? "N/A"}
              </strong>
            </p>

            <p>
              <strong>Note:</strong> Selecting Emergency
              changes the ML input only. It does not
              automatically change a patient's queue position
              or replace the hospital's emergency triage
              procedures.
            </p>
          </div>
        </section>
      )}

      {/* DECISION FLOW */}

      <section className="decision-flow">
        <span>PATIENT FLOW DATA</span>
        <b>→</b>
        <span>DATA MINING</span>
        <b>→</b>
        <span>BOTTLENECK ANALYSIS</span>
        <b>→</b>
        <span>ML PREDICTION</span>
        <b>→</b>
        <span>DECISION SUPPORT</span>
      </section>

      {/* PRINCIPLE */}

      <section className="decision-note">
        <div className="note-icon">i</div>

        <div>
          <h2>Decision-support principle</h2>

          <p>
            The system does not automatically control
            hospital operations. It provides analytical
            evidence and recommendations to help authorized
            hospital staff make operational decisions.
            Emergency patient care and triage must follow
            hospital procedures.
          </p>
        </div>
      </section>
    </div>
  );
}

export default DecisionSupport;