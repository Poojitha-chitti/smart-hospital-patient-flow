import { useEffect, useState } from "react";
import "./DataMining.css";

function DataMining() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("http://192.168.43.177:8080/api/data-mining/patterns")
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load data mining results");
        }

        return response.json();
      })
      .then((result) => {
        setData(result);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Data mining error:", error);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="dm-page">
        <div className="dm-loading">
          Loading data mining results...
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="dm-page">
        <div className="dm-loading">
          Unable to load data.
        </div>
      </div>
    );
  }

  const stagePatterns = data.stagePatterns || [];
  const arrivalPatterns = data.arrivalPatterns || [];

  const maxWaiting =
    stagePatterns.length > 0
      ? Math.max(
          ...stagePatterns.map((item) =>
            Number(item.averageWaitingTime)
          )
        )
      : 0;

  const maxArrival =
    arrivalPatterns.length > 0
      ? Math.max(
          ...arrivalPatterns.map((item) =>
            Number(item.patientCount)
          )
        )
      : 0;

  // Stage with highest waiting time
  const highestWaitingStage =
    stagePatterns.length > 0
      ? [...stagePatterns].sort(
          (a, b) =>
            Number(b.averageWaitingTime) -
            Number(a.averageWaitingTime)
        )[0]
      : null;

  // Arrival period with highest patient count
  const peakArrival =
    arrivalPatterns.length > 0
      ? [...arrivalPatterns].sort(
          (a, b) =>
            Number(b.patientCount) -
            Number(a.patientCount)
        )[0]
      : null;

  return (
    <div className="dm-page">

      {/* HEADER */}

      <header className="dm-top">

        <div>

          <div className="dm-brand">
            SMART HOSPITAL
          </div>

          <h1>
            Data Mining & Pattern Analysis
          </h1>

          <p>
            Discover meaningful patterns from
            hospital patient-flow data.
          </p>

        </div>

        <button
          className="dm-dashboard-btn"
          onClick={() => {
            window.location.href = "/dashboard";
          }}
        >
          ← Dashboard
        </button>

      </header>


      {/* SUMMARY */}

      <section className="dm-summary">

        <div className="dm-stat">

          <span>
            High Waiting Events
          </span>

          <strong>
            {data.highWaitingEvents}
          </strong>

          <small>
            Waiting time ≥ 20 min
          </small>

        </div>


        <div className="dm-stat">

          <span>
            Stages Analyzed
          </span>

          <strong>
            {stagePatterns.length}
          </strong>

          <small>
            Hospital workflow stages
          </small>

        </div>


        <div className="dm-stat">

          <span>
            Arrival Periods
          </span>

          <strong>
            {arrivalPatterns.length}
          </strong>

          <small>
            Observed time periods
          </small>

        </div>

      </section>


      {/* MAIN ANALYTICS */}

      <section className="dm-grid">


        {/* WAITING TIME */}

        <div className="dm-panel">

          <div className="dm-panel-title">

            <div>

              <h2>
                Waiting Time by Stage
              </h2>

              <p>
                Average waiting time discovered
                from workflow data.
              </p>

            </div>

          </div>


          <div className="waiting-chart">

            {stagePatterns.map((item, index) => {

              const value =
                Number(item.averageWaitingTime);

              const width =
                maxWaiting > 0
                  ? (value / maxWaiting) * 100
                  : 0;

              return (

                <div
                  className="waiting-row"
                  key={index}
                >

                  <div className="waiting-name">
                    {item.stage}
                  </div>


                  <div className="waiting-bar-area">

                    <div className="waiting-track">

                      <div
                        className="waiting-bar"
                        style={{
                          width: `${width}%`,
                        }}
                      />

                    </div>

                    <span>
                      {value.toFixed(2)} min
                    </span>

                  </div>

                </div>

              );

            })}

          </div>

        </div>


        {/* ARRIVAL PATTERN */}

        <div className="dm-panel">

          <div className="dm-panel-title">

            <div>

              <h2>
                Patient Arrival Pattern
              </h2>

              <p>
                Patients observed during each
                arrival hour.
              </p>

            </div>

          </div>


          <div className="arrival-chart">

            {arrivalPatterns.map(
              (item, index) => {

                const value =
                  Number(item.patientCount);

                const height =
                  maxArrival > 0
                    ? (value / maxArrival) * 100
                    : 0;

                return (

                  <div
                    className="arrival-item"
                    key={index}
                  >

                    <span className="arrival-number">
                      {value}
                    </span>


                    <div className="arrival-track">

                      <div
                        className="arrival-bar"
                        style={{
                          height: `${height}%`,
                        }}
                      />

                    </div>


                    <span className="arrival-hour">
                      {item.hour}:00
                    </span>

                  </div>

                );

              }
            )}

          </div>

        </div>

      </section>


      {/* KEY FINDINGS */}

      <section className="dm-findings">

        <div className="findings-heading">

          <div>

            <h2>
              Key Findings
            </h2>

            <p>
              Patterns identified from historical
              patient-flow data.
            </p>

          </div>

        </div>


        <div className="finding-grid">


          {/* FINDING 1 */}

          <div className="finding">

            <div className="finding-number">
              01
            </div>

            <div>

              <h3>
                Highest waiting stage detected
              </h3>

              <p>

                {highestWaitingStage ? (
                  <>
                    <strong>
                      {highestWaitingStage.stage}
                    </strong>{" "}
                    has the highest average waiting
                    time of{" "}
                    <strong>
                      {Number(
                        highestWaitingStage.averageWaitingTime
                      ).toFixed(2)}{" "}
                      minutes
                    </strong>{" "}
                    among the analyzed stages.
                  </>
                ) : (
                  "No stage waiting pattern is available."
                )}

              </p>

            </div>

          </div>


          {/* FINDING 2 */}

          <div className="finding">

            <div className="finding-number">
              02
            </div>

            <div>

              <h3>
                Peak arrival period detected
              </h3>

              <p>

                {peakArrival ? (
                  <>
                    The highest patient arrival
                    count was observed around{" "}
                    <strong>
                      {peakArrival.hour}:00
                    </strong>{" "}
                    with{" "}
                    <strong>
                      {peakArrival.patientCount}
                    </strong>{" "}
                    recorded patients.
                  </>
                ) : (
                  "No arrival pattern is available."
                )}

              </p>

            </div>

          </div>


          {/* FINDING 3 */}

          <div className="finding">

            <div className="finding-number">
              03
            </div>

            <div>

              <h3>
                High-waiting events identified
              </h3>

              <p>

                The system detected{" "}
                <strong>
                  {data.highWaitingEvents}
                </strong>{" "}
                historical workflow events with
                waiting time of 20 minutes or more.

              </p>

            </div>

          </div>


          {/* FINDING 4 */}

          <div className="finding">

            <div className="finding-number">
              04
            </div>

            <div>

              <h3>
                Patterns support ML prediction
              </h3>

              <p>
                The discovered patient-flow
                patterns provide analytical
                information that can support
                ML-based prediction and
                operational decisions.
              </p>

            </div>

          </div>

        </div>

      </section>


      {/* PROCESS FOOTER */}

      <section className="dm-process">

        <span>
          DATA
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
          PATTERNS
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

    </div>
  );
}

export default DataMining;
