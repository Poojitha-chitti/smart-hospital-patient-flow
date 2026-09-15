import { useEffect, useState } from "react";
import "./ResourceAnalysis.css";
import { API_URL } from "./config";

function ResourceAnalysis() {
  const [staff, setStaff] = useState([]);
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([
      fetch(`${API_URL}/api/staff`).then((res) => {
        if (!res.ok) {
          throw new Error("Failed to load staff information");
        }
        return res.json();
      }),

      fetch(`${API_URL}/api/resources`).then((res) => {
        if (!res.ok) {
          throw new Error("Failed to load resource information");
        }
        return res.json();
      }),
    ])
      .then(([staffData, resourceData]) => {
        setStaff(staffData);
        setResources(resourceData);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Resource analysis error:", error);
        setError("Unable to load staff and resource information.");
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="resource-page">
        <div className="resource-loading">
          Loading staff and resource information...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="resource-page">
        <div className="resource-loading">
          {error}
        </div>
      </div>
    );
  }

  // Staff calculations
  const totalStaff = staff.length;

  const availableStaff = staff.filter(
    (person) => person.available
  ).length;

  const unavailableStaff =
    totalStaff - availableStaff;

  const totalStaffCapacity = staff.reduce(
    (sum, person) => sum + Number(person.capacity || 0),
    0
  );

  // Resource calculations
  const totalResourceCapacity = resources.reduce(
    (sum, resource) =>
      sum + Number(resource.capacity || 0),
    0
  );

  const totalResourceAvailable = resources.reduce(
    (sum, resource) =>
      sum + Number(resource.available || 0),
    0
  );

  const totalResourceUsed =
    totalResourceCapacity - totalResourceAvailable;

  const overallResourceUtilization =
    totalResourceCapacity > 0
      ? (totalResourceUsed / totalResourceCapacity) * 100
      : 0;

  // Find the most utilized resource
  const mostUtilizedResource =
    resources.length > 0
      ? [...resources].sort((a, b) => {
          const utilizationA =
            a.capacity > 0
              ? (a.capacity - a.available) / a.capacity
              : 0;

          const utilizationB =
            b.capacity > 0
              ? (b.capacity - b.available) / b.capacity
              : 0;

          return utilizationB - utilizationA;
        })[0]
      : null;

  // Find department with unavailable staff
  const departmentsWithUnavailableStaff =
    [
      ...new Set(
        staff
          .filter((person) => !person.available)
          .map((person) => person.department)
      ),
    ];

  return (
    <div className="resource-page">

      {/* HEADER */}

      <div className="resource-header">

        <div>

          <div className="resource-brand">
            SMART HOSPITAL
          </div>

          <h1>
            Staff & Resource Analysis
          </h1>

          <p>
            Monitor staff availability and resource
            capacity across hospital departments.
          </p>

        </div>

        <button
          className="resource-back"
          onClick={() => {
            window.location.href = "/dashboard";
          }}
        >
          ← Dashboard
        </button>

      </div>


      {/* SUMMARY */}

      <section className="resource-section">

        <div className="section-heading">

          <div>

            <h2>
              Current Operational Status
            </h2>

            <p>
              Staff and resource conditions retrieved
              from the hospital database.
            </p>

          </div>

        </div>


        <div className="staff-grid">

          <div className="staff-card">

            <div className="staff-top">

              <div className="staff-avatar">
                S
              </div>

              <span className="status available">
                Current
              </span>

            </div>

            <h3>
              Available Staff
            </h3>

            <p>
              Currently available hospital staff
            </p>

            <div className="staff-details">

              <div>
                <span>Available</span>

                <strong>
                  {availableStaff}
                </strong>
              </div>

              <div>
                <span>Total</span>

                <strong>
                  {totalStaff}
                </strong>
              </div>

            </div>

          </div>


          <div className="staff-card">

            <div className="staff-top">

              <div className="staff-avatar">
                C
              </div>

              <span className="status available">
                Capacity
              </span>

            </div>

            <h3>
              Staff Capacity
            </h3>

            <p>
              Combined capacity of registered staff
            </p>

            <div className="staff-details">

              <div>
                <span>Capacity</span>

                <strong>
                  {totalStaffCapacity}
                </strong>
              </div>

              <div>
                <span>Unavailable</span>

                <strong>
                  {unavailableStaff}
                </strong>
              </div>

            </div>

          </div>


          <div className="staff-card">

            <div className="staff-top">

              <div className="staff-avatar">
                R
              </div>

              <span className="status available">
                Current
              </span>

            </div>

            <h3>
              Resource Utilization
            </h3>

            <p>
              Overall utilization of available resources
            </p>

            <div className="staff-details">

              <div>
                <span>Utilization</span>

                <strong>
                  {overallResourceUtilization.toFixed(1)}%
                </strong>
              </div>

              <div>
                <span>Available</span>

                <strong>
                  {totalResourceAvailable}
                </strong>
              </div>

            </div>

          </div>

        </div>

      </section>


      {/* STAFF */}

      <section className="resource-section">

        <div className="section-heading">

          <div>

            <h2>
              Staff Availability
            </h2>

            <p>
              Current staff capacity and availability.
            </p>

          </div>

        </div>


        <div className="staff-grid">

          {staff.map((person) => (

            <div
              className="staff-card"
              key={person.staff_id}
            >

              <div className="staff-top">

                <div className="staff-avatar">
                  {person.staff_name
                    ? person.staff_name.charAt(0)
                    : "S"}
                </div>

                <span
                  className={
                    person.available
                      ? "status available"
                      : "status unavailable"
                  }
                >
                  {person.available
                    ? "Available"
                    : "Unavailable"}
                </span>

              </div>


              <h3>
                {person.staff_name}
              </h3>

              <p>
                {person.role}
              </p>


              <div className="staff-details">

                <div>

                  <span>
                    Department
                  </span>

                  <strong>
                    {person.department}
                  </strong>

                </div>


                <div>

                  <span>
                    Capacity
                  </span>

                  <strong>
                    {person.capacity}
                  </strong>

                </div>

              </div>

            </div>

          ))}

        </div>

      </section>


      {/* RESOURCES */}

      <section className="resource-section">

        <div className="section-heading">

          <div>

            <h2>
              Hospital Resources
            </h2>

            <p>
              Capacity and currently available
              resources.
            </p>

          </div>

        </div>


        <div className="resource-table">

          <table>

            <thead>

              <tr>

                <th>
                  Department
                </th>

                <th>
                  Resource
                </th>

                <th>
                  Total Capacity
                </th>

                <th>
                  Available
                </th>

                <th>
                  Utilization
                </th>

              </tr>

            </thead>


            <tbody>

              {resources.map((resource) => {

                const capacity =
                  Number(resource.capacity || 0);

                const available =
                  Number(resource.available || 0);

                const utilization =
                  capacity > 0
                    ? ((capacity - available) /
                        capacity) *
                      100
                    : 0;

                return (

                  <tr
                    key={resource.resource_id}
                  >

                    <td>
                      {resource.department}
                    </td>

                    <td>
                      <strong>
                        {resource.resource_type}
                      </strong>
                    </td>

                    <td>
                      {capacity}
                    </td>

                    <td>
                      {available}
                    </td>

                    <td>

                      <div className="utilization-cell">

                        <div className="utilization-track">

                          <div
                            className="utilization-bar"
                            style={{
                              width: `${utilization}%`,
                            }}
                          />

                        </div>

                        <span>
                          {utilization.toFixed(1)}%
                        </span>

                      </div>

                    </td>

                  </tr>

                );
              })}

            </tbody>

          </table>

        </div>

      </section>


      {/* ANALYTICAL INSIGHT */}

      <section className="resource-insight">

        <div className="insight-icon">
          i
        </div>


        <div>

          <h2>
            Resource Insight
          </h2>


          <p>

            {mostUtilizedResource ? (
              <>
                <strong>
                  {mostUtilizedResource.resource_type}
                </strong>{" "}
                in the{" "}
                <strong>
                  {mostUtilizedResource.department}
                </strong>{" "}
                department currently has the
                highest resource utilization at{" "}
                <strong>
                  {(
                    ((Number(
                      mostUtilizedResource.capacity
                    ) -
                      Number(
                        mostUtilizedResource.available
                      )) /
                      Number(
                        mostUtilizedResource.capacity
                      )) *
                    100
                  ).toFixed(1)}
                  %
                </strong>
                . Staff availability and resource
                capacity can be analyzed together
                with patient-flow information to
                understand conditions associated
                with waiting and bottlenecks.
              </>
            ) : (
              <>
                Staff availability and resource
                capacity can be analyzed together
                with patient-flow information to
                understand conditions associated
                with waiting and bottlenecks.
              </>
            )}

          </p>


          {departmentsWithUnavailableStaff.length > 0 && (

            <p>

              Departments with currently unavailable
              staff:{" "}

              <strong>
                {departmentsWithUnavailableStaff.join(
                  ", "
                )}
              </strong>
              .

            </p>

          )}

        </div>

      </section>

    </div>
  );
}

export default ResourceAnalysis;
