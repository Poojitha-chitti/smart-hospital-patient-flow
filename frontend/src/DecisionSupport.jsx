import { useEffect, useState } from "react";
import "./DecisionSupport.css";

function DecisionSupport() {
  const [bottlenecks, setBottlenecks] = useState([]);
  const [mlResult, setMlResult] = useState(null);
  const [mlInput, setMlInput] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDecisionData = async () => {
      try {
        const [
          bottleneckResponse,
          inputResponse,
        ] = await Promise.all([
          fetch(
            "http://192.168.43.177:8080/api/bottleneck-analysis"
          ),
          fetch(
            "http://192.168.43.177:8080/api/ml/current-input"
          ),
        ]);

        if (
          !bottleneckResponse.ok ||
          !inputResponse.ok
        ) {
          throw new Error(
            "Unable to load decision-support data"
          );
        }

        const bottleneckData =
          await bottleneckResponse.json();

        const inputData =
          await inputResponse.json();

        setBottlenecks(bottleneckData);
        setMlInput(inputData);

        // Send current database conditions to ML service
        const mlResponse = await fetch(
          "http://192.168.43.177:8080/api/ml/predict",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify(inputData),
          }
        );

        if (!mlResponse.ok) {
          throw new Error(
            "ML prediction failed"
          );
        }

        const mlData =
          await mlResponse.json();

        setMlResult(mlData);
        setLoading(false);
      } catch (error) {
        console.error(
          "Decision support error:",
          error
        );

        setError(
          "Unable to prepare decision-support information."
        );

        setLoading(false);
      }
    };

    loadDecisionData();
  }, []);

  if (loading) {
    return (
      <div className="decision-page">

        <div className="decision-loading">
          Preparing decision-support information...
        </div>

      </div>
    );
  }

  if (error) {
    return (
      <div className="decision-page">

        <div className="decision-loading">
          {error}
        </div>

      </div>
    );
  }

  const mainBottleneck =
    bottlenecks.length > 0
      ? bottlenecks[0]
      : null;

  const isHighWaiting =
    mlResult?.prediction === 1;

  return (
    <div className="decision-page">

      {/* HEADER */}

      <header className="decision-header">

        <div>

          <div className="decision-brand">
            SMART HOSPITAL
          </div>

          <h1>
            Decision Support
          </h1>

          <p>
            Convert patient-flow analysis and ML
            predictions into actionable
            operational information.
          </p>

        </div>


        <button
          className="decision-back"
          onClick={() => {
            window.location.href =
              "/dashboard";
          }}
        >
          ← Dashboard
        </button>

      </header>


      {/* PRIMARY BOTTLENECK */}

      {mainBottleneck && (

        <section className="decision-main">

          <div className="decision-main-heading">

            <div>

              <span className="decision-label">
                PRIMARY BOTTLENECK
              </span>

              <h2>
                {mainBottleneck.stage}
              </h2>

              <p>
                This stage currently has the
                highest average waiting time.
              </p>

            </div>


            <span
              className={`severity ${mainBottleneck.severity.toLowerCase()}`}
            >
              {mainBottleneck.severity}
            </span>

          </div>


          <div className="decision-metrics">

            <div>

              <span>
                Average Waiting
              </span>

              <strong>
                {Number(
                  mainBottleneck.averageWaitingTime
                ).toFixed(2)}{" "}
                min
              </strong>

            </div>


            <div>

              <span>
                Average Service
              </span>

              <strong>
                {Number(
                  mainBottleneck.averageServiceTime
                ).toFixed(2)}{" "}
                min
              </strong>

            </div>


            <div>

              <span>
                ML Prediction
              </span>

              <strong>
                {mlResult?.condition ||
                  "Unavailable"}
              </strong>

            </div>

          </div>

        </section>

      )}


      {/* DECISION CARDS */}

      <section className="decision-grid">


        {/* OBSERVED BOTTLENECK */}

        <div className="decision-card">

          <div className="card-number">
            01
          </div>

          <div>

            <h2>
              Observed Bottleneck
            </h2>

            <p>

              {mainBottleneck ? (
                <>
                  <strong>
                    {mainBottleneck.stage}
                  </strong>{" "}
                  has the highest average
                  waiting time in the analyzed
                  workflow data.
                </>
              ) : (
                "No bottleneck information available."
              )}

            </p>

          </div>

        </div>


        {/* PREDICTED CONDITION */}

        <div className="decision-card">

          <div className="card-number">
            02
          </div>

          <div>

            <h2>
              Predicted Condition
            </h2>

            <p>

              {mlResult ? (
                <>
                  The ML model predicts a{" "}
                  <strong>
                    {mlResult.condition}
                  </strong>{" "}
                  for the current{" "}
                  <strong>
                    {mlInput?.stage}
                  </strong>{" "}
                  conditions.
                </>
              ) : (
                "ML prediction information is unavailable."
              )}

            </p>

          </div>

        </div>


        {/* OPERATIONAL RECOMMENDATION */}

        <div className="decision-card">

          <div className="card-number">
            03
          </div>

          <div>

            <h2>
              Operational Recommendation
            </h2>

            <p>

              {isHighWaiting ? (
                <>
                  Monitor the{" "}
                  <strong>
                    {mlInput?.stage}
                  </strong>{" "}
                  queue and review staff and
                  resource allocation to help
                  manage the predicted high
                  waiting condition.
                </>
              ) : (
                <>
                  Continue monitoring the current
                  workflow conditions. No high
                  waiting condition is predicted
                  for the current input.
                </>
              )}

            </p>

          </div>

        </div>


        {/* DECISION BASIS */}

        <div className="decision-card">

          <div className="card-number">
            04
          </div>

          <div>

            <h2>
              Decision Basis
            </h2>

            <p>
              The recommendation is based on
              bottleneck analysis, historical
              workflow patterns, current
              staff/resource information, and
              ML prediction.
            </p>

          </div>

        </div>

      </section>


      {/* CURRENT CONDITIONS */}

      {mlInput && (

        <section className="decision-note">

          <div className="note-icon">
            i
          </div>

          <div>

            <h2>
              Current Conditions Used
            </h2>

            <p>

              Stage:{" "}
              <strong>
                {mlInput.stage}
              </strong>
              {" • "}
              Staff available:{" "}
              <strong>
                {mlInput.staff_available}
              </strong>
              {" • "}
              Staff capacity:{" "}
              <strong>
                {mlInput.staff_capacity}
              </strong>
              {" • "}
              Resource capacity:{" "}
              <strong>
                {mlInput.resource_capacity}
              </strong>
              {" • "}
              Resource available:{" "}
              <strong>
                {mlInput.resource_available}
              </strong>

            </p>

          </div>

        </section>

      )}


      {/* DECISION FLOW */}

      <section className="decision-flow">

        <span>
          PATIENT FLOW DATA
        </span>

        <b>
          →
        </b>

        <span>
          DATA MINING
        </span>

        <b>
          →
        </b>

        <span>
          BOTTLENECK ANALYSIS
        </span>

        <b>
          →
        </b>

        <span>
          ML PREDICTION
        </span>

        <b>
          →
        </b>

        <span>
          DECISION SUPPORT
        </span>

      </section>


      {/* PRINCIPLE */}

      <section className="decision-note">

        <div className="note-icon">
          i
        </div>

        <div>

          <h2>
            Decision-support principle
          </h2>

          <p>
            The system does not automatically
            control hospital operations. It
            provides analytical evidence and
            recommendations that can help
            authorized hospital staff make
            operational decisions.
          </p>

        </div>

      </section>

    </div>
  );
}

export default DecisionSupport;
