import React from "react";
import "./Home.css";

function Home() {

  return (
    <div className="home-page">
      <div className="home-header">
        <div>
          <h1>Smart Hospital</h1>
          <p>Hospital Management and Decision Support System</p>
        </div>

        <button className="logout-button" onClick={() => window.location.href = "/"}>
          Logout
        </button>
      </div>

      <div className="home-section">
        <div className="home-welcome">
  <h2>Welcome to Smart Hospital</h2>
  <p>
    Manage patient operations and access hospital analytics and
    decision-support tools from one place.
  </p>
</div>
        <h2>Hospital Operations</h2>

        <div className="home-grid">
          <button onClick={() => window.location.href = "/patient-registration"}>
  🧑‍⚕️
  <span>Patient Registration</span>
</button>
          <button onClick={() => window.location.href = "/waiting-list"}>
            ⏳
            <span>Waiting List</span>
          </button>

          <button onClick={() => window.location.href = "/patient-list"}>
            👥
            <span>Patient List</span>
          </button>

          <button onClick={() => window.location.href = "/doctor-list"}>
            👨‍⚕️
            <span>Doctor List</span>
          </button>

          <button onClick={() => window.location.href = "/room-list"}>
            🚪
            <span>Hospital Rooms</span>
          </button>
        </div>
      </div>

      <div className="home-section">
        <h2>Analysis & Decision Support</h2>

        <div className="home-grid">
          <button onClick={() => window.location.href = "/patient-flow"}>
            📊
            <span>Patient Flow</span>
          </button>

          <button onClick={() => window.location.href = "/data-mining"}>
            📈
            <span>Data Mining</span>
          </button>

          <button onClick={() => window.location.href = "/decision-support"}>
            🧠
            <span>Decision Support</span>
          </button>

          <button onClick={() => window.location.href = "/resource-analysis"}>
            📦
            <span>Resource Analysis</span>
          </button>
        </div>
      </div>
    </div>
  );
}

export default Home;