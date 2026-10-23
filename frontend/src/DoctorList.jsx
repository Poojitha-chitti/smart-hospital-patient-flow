import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./DoctorList.css";

function DoctorList() {

  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [updatingDoctor, setUpdatingDoctor] = useState(null);

  const role =
    localStorage.getItem("role") || "";

  const username =
    localStorage.getItem("username") || "";


  const loadDoctors = async () => {

    try {

      setLoading(true);

      const response = await fetch(
        `${API_URL}/api/doctors`
      );

      if (!response.ok) {
        throw new Error();
      }

      const data = await response.json();

      setDoctors(data);
      setError("");

    } catch {

      setError(
        "Unable to load doctor list."
      );

    } finally {

      setLoading(false);

    }
  };


  useEffect(() => {

    loadDoctors();

  }, []);


  const updateDoctorStatus = async (
    doctorId,
    status
  ) => {

    try {

      setUpdatingDoctor(doctorId);
      setError("");

      const response = await fetch(
        `${API_URL}/api/doctors/${doctorId}/status`,
        {
          method: "PUT",

          headers: {
            "Content-Type":
              "application/json"
          },

          body: JSON.stringify({
            status,
            username
          })
        }
      );


      const data =
        await response
          .json()
          .catch(() => ({}));


      if (!response.ok) {

        throw new Error(
          data.message ||
            "Unable to update doctor status."
        );

      }


      /*
       * Reload the doctor list so the
       * updated status is immediately visible.
       */
      await loadDoctors();

    } catch (error) {

      setError(
        error.message ||
          "Unable to update doctor status."
      );

    } finally {

      setUpdatingDoctor(null);

    }
  };


  const initials = (name) =>
    (name || "Doctor")
      .replace(/^Dr\.\s*/i, "")
      .split(/\s+/)
      .slice(0, 2)
      .map((x) => x[0])
      .join("")
      .toUpperCase();


  return (

    <div className="doctor-page">

      <main className="doctor-main">

        <div className="doctor-top-row">

          <div>

            <h1>
              Doctor List
            </h1>

            <p>
              Doctors available in the hospital.
            </p>

          </div>


          <button
  className="doctor-home-btn"
  onClick={() =>
    (window.location.href =
      "/home?view=operations")
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


        <div className="doctor-stat">

          <span>
            Total doctors
          </span>

          <strong>
            {doctors.length}
          </strong>

        </div>


        {loading && (

          <div className="doctor-empty">

            Loading doctors...

          </div>

        )}


        {error && (

          <div className="doctor-empty doctor-error">

            {error}

          </div>

        )}


        {!loading &&
          !error &&
          !doctors.length && (

            <div className="doctor-empty">

              No doctors found.

              <br />

              Doctor information will appear here.

            </div>

          )}


        {!loading &&
          !error &&
          doctors.length > 0 && (

            <div className="doctor-list">

              {doctors.map((d) => (

                <div
                  className="doctor-row"
                  key={d.doctor_id}
                >

                  <div className="doctor-avatar">

                    {initials(
                      d.doctor_name
                    )}

                  </div>


                  <div className="doctor-who">

                    <div className="name">

                      {d.doctor_name}

                    </div>

                    <div className="spec">

                      {d.specialization}

                    </div>

                  </div>


                  <span className="doctor-dept-tag">

                    {d.department}

                  </span>


                  <div className="doctor-room">

                    Room{" "}

                    <strong>
                      {d.room_no}
                    </strong>

                  </div>


                  {role === "ADMIN" ? (

                    <select
                      className="doctor-status-select"
                      value={
                        d.status ||
                        "UNAVAILABLE"
                      }
                      disabled={
                        updatingDoctor ===
                        d.doctor_id
                      }
                      onChange={(e) =>
                        updateDoctorStatus(
                          d.doctor_id,
                          e.target.value
                        )
                      }
                    >

                      <option value="AVAILABLE">
                        Available
                      </option>

                      <option value="BUSY">
                        Busy
                      </option>

                      <option value="UNAVAILABLE">
                        Unavailable
                      </option>

                    </select>

                  ) : (

                    <span
                      className={`doctor-status-pill ${
                        d.status ===
                        "AVAILABLE"
                          ? "available"
                          : "busy"
                      }`}
                    >

                      {d.status ===
                      "AVAILABLE"
                        ? "Available"
                        : d.status ===
                          "UNAVAILABLE"
                        ? "Unavailable"
                        : "Busy"}

                    </span>

                  )}

                </div>

              ))}

            </div>

          )}

      </main>

    </div>

  );
}

export default DoctorList;