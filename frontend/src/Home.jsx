import React, { useEffect, useState } from "react";
import "./Home.css";
import { API_URL } from "./config";

const Arrow = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M9 18l6-6-6-6" />
  </svg>
);

const Back = () => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    aria-hidden="true"
  >
    <path d="M15 18l-6-6 6-6" />
  </svg>
);

function Home() {
  const initialView =
  new URLSearchParams(window.location.search).get("view") || "home";

const [view, setView] = useState(initialView);
  const [workArea, setWorkArea] = useState("");

  const username =
    localStorage.getItem("username") || "Poojitha";

  const role =
    localStorage.getItem("role") || "ADMIN";

  const go = (path) => {
    window.location.href = path;
  };

  const logout = () => {
    localStorage.removeItem("username");
    localStorage.removeItem("role");

    window.location.href = "/";
  };

  useEffect(() => {
    const loadWorkArea = async () => {
      if (role === "ADMIN") {
        setWorkArea("ALL");
        return;
      }

      try {
        const response = await fetch(
          `${API_URL}/api/admin/users/staff`
        );

        if (!response.ok) {
          return;
        }

        const users = await response.json();

        const currentUser = users.find(
          (user) =>
            user.username &&
            user.username.toLowerCase() ===
              username.toLowerCase()
        );

        if (currentUser) {
          setWorkArea(
            currentUser.work_area || ""
          );
        }
      } catch (error) {
        console.error(
          "Unable to load staff work area.",
          error
        );
      }
    };

    loadWorkArea();
  }, [username, role]);

  /*
   * Patient Management options
   *
   * ADMIN:
   *   Register Patient
   *   Patient List
   *   Waiting List
   *
   * REGISTRATION:
   *   Register Patient
   *   Patient List
   *   Waiting List
   *
   * OP:
   *   Patient List
   *   Waiting List
   *
   * PHARMACY:
   *   Patient List
   *   Waiting List
   *
   * DIAGNOSTICS:
   *   Patient List
   *   Waiting List
   */

  const getPatientOptions = () => {
    if (role === "ADMIN") {
      return [
        ["Register Patient", "/patient-registration"],
        ["Patient List", "/patient-list"],
        ["Waiting List", "/waiting-list"],
      ];
    }

    if (workArea === "REGISTRATION") {
      return [
        ["Register Patient", "/patient-registration"],
        ["Patient List", "/patient-list"],
        ["Waiting List", "/waiting-list"],
      ];
    }

    if (workArea === "OP") {
      return [
        ["Patient List", "/patient-list"],
        ["Waiting List", "/waiting-list"],
      ];
    }

    if (workArea === "PHARMACY") {
      return [
        ["Patient List", "/patient-list"],
        ["Waiting List", "/waiting-list"],
      ];
    }

    if (workArea === "DIAGNOSTICS") {
      return [
        ["Patient List", "/patient-list"],
        ["Waiting List", "/waiting-list"],
      ];
    }

    return [];
  };

  const patientOptions =
    getPatientOptions();

  const canAccessPatients =
    role === "ADMIN" ||
    workArea === "REGISTRATION" ||
    workArea === "OP" ||
    workArea === "PHARMACY" ||
    workArea === "DIAGNOSTICS";

  const canAccessOperations =
    role === "ADMIN";

  const canAccessAnalysis =
    role === "ADMIN" ||
    workArea === "REGISTRATION" ||
    workArea === "OP" ||
    workArea === "PHARMACY" ||
    workArea === "DIAGNOSTICS";

  const groups = {
    patients: patientOptions,

    operations: [
      ["Doctors", "/doctor-list"],
      ["Rooms", "/room-list"],
      ["Staff & Resources", "/resource-analysis"],
    ],

    analysis: [
      ["Overview", "/dashboard"],
      ["Patient Flow", "/patient-flow"],
      ["Apriori & Data Mining", "/data-mining"],
      ["Decision Support", "/decision-support"],
    ],
  };

  const visibleGroups = {};

  if (canAccessPatients) {
    visibleGroups.patients =
      groups.patients;
  }

  if (canAccessOperations) {
    visibleGroups.operations =
      groups.operations;
  }

  if (canAccessAnalysis) {
    visibleGroups.analysis =
      groups.analysis;
  }

  const goToGroup = (group) => {
    if (visibleGroups[group]) {
      setView(group);
    }
  };

  return (
    <div className="home-page">

      <header className="home-header">

        <div className="home-brand-mark">

          <div className="home-cross-badge" />

          <div className="home-brand-text">
            <div className="name">
              Smart Hospital
            </div>
          </div>

        </div>

        <div className="home-user-block">

          <div className="home-user-id">

            <div className="who">
              {username}
            </div>

            <div className="role">
              {role}
            </div>

          </div>

          <div className="home-avatar">
            {username
              .charAt(0)
              .toUpperCase()}
          </div>

          <button
            className="home-logout"
            onClick={logout}
          >
            Logout
          </button>

        </div>

      </header>

      <main className="home-main">

        {view === "home" && (
          <div>

            <div className="home-hero">
              <h1>
                Smart Hospital Operations
              </h1>
            </div>

            <div className="home-grid">

              {canAccessPatients && (
                <button
                  className="home-card"
                  onClick={() =>
                    goToGroup("patients")
                  }
                >

                  <div className="home-card-icon">

                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                    >
                      <circle
                        cx="9"
                        cy="8"
                        r="3"
                      />

                      <path d="M3 20c0-3 2.7-5 6-5s6 2 6 5" />

                      <circle
                        cx="17"
                        cy="7"
                        r="2.4"
                      />

                      <path d="M15.5 20c0-2.6 1.6-4.3 3.8-4.6" />
                    </svg>

                  </div>

                  <h2>
                    Patient Management
                  </h2>

                </button>
              )}

              {canAccessOperations && (
                <button
                  className="home-card"
                  onClick={() =>
                    goToGroup("operations")
                  }
                >

                  <div className="home-card-icon">

                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                    >
                      <rect
                        x="4"
                        y="3"
                        width="16"
                        height="18"
                        rx="2"
                      />

                      <path d="M9 8h6M9 12h6M9 16h3" />
                    </svg>

                  </div>

                  <h2>
                    Hospital Operations
                  </h2>

                </button>
              )}

              {canAccessAnalysis && (
  <button
    className="home-card"
    onClick={() =>
      go("/dashboard")
    }
  >

                  <div className="home-card-icon">

                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                    >
                      <path d="M4 20V10M11 20V4M18 20v-7" />
                    </svg>

                  </div>

                  <h2>
                    Patient Flow Analysis
                  </h2>

                </button>
              )}

            </div>

            {role !== "ADMIN" &&
              !workArea && (
                <div
                  style={{
                    marginTop: "20px",
                    textAlign: "center",
                    fontSize: "14px",
                    opacity: 0.7,
                  }}
                >
                  Loading your work area...
                </div>
              )}

          </div>
        )}

        {view !== "home" && (
          <div className="home-sub-view">

            <button
              className="home-breadcrumb"
              onClick={() =>
                setView("home")
              }
            >
              <Back />
              Home
            </button>

            <h1 className="home-sub-title">
              {view === "patients"
                ? "Patient Management"
                : view === "operations"
                ? "Hospital Operations"
                : "Patient Flow Analysis"}
            </h1>

            <div className="home-option-list">

              {visibleGroups[view]?.map(
                ([label, path]) => (
                  <button
                    className="home-option"
                    key={path}
                    onClick={() =>
                      go(path)
                    }
                  >
                    <span>
                      {label}
                    </span>

                    <Arrow />
                  </button>
                )
              )}

            </div>

          </div>
        )}

      </main>

    </div>
  );
}

export default Home;