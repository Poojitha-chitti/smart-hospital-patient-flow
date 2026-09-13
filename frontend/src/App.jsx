import { useState } from "react";
import "./App.css";
import Dashboard from "./Dashboard";
import PatientFlow from "./PatientFlow";
import DataMining from "./DataMining";
import ResourceAnalysis from "./ResourceAnalysis";
import DecisionSupport from "./DecisionSupport";

function App() {
  const role = localStorage.getItem("role");
  const loggedInUser = localStorage.getItem("username");

  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const path = window.location.pathname;

  // Protected pages
  if (path === "/dashboard") {
    if (!loggedInUser || !role) {
      window.location.href = "/";
      return null;
    }

    return <Dashboard />;
  }

  if (path === "/patient-flow") {
    if (!loggedInUser || !role) {
      window.location.href = "/";
      return null;
    }

    return <PatientFlow />;
  }

  if (path === "/data-mining") {
    if (!loggedInUser || !role) {
      window.location.href = "/";
      return null;
    }

    return <DataMining />;
  }

  if (path === "/resource-analysis") {
    if (!loggedInUser || !role) {
      window.location.href = "/";
      return null;
    }

    if (role !== "ADMIN") {
      window.location.href = "/dashboard";
      return null;
    }

    return <ResourceAnalysis />;
  }

  if (path === "/decision-support") {
    if (!loggedInUser || !role) {
      window.location.href = "/";
      return null;
    }

    return <DecisionSupport />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");

    if (!username || !password) {
      setMessage("Please enter username and password.");
      return;
    }

    const endpoint = isRegister
      ? "http://192.168.43.177:8080/api/auth/register"
      : "http://192.168.43.177:8080/api/auth/login";

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: username,
          password: password,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setMessage(data.message || "Something went wrong.");
        return;
      }

      if (isRegister) {
        setMessage("Registration successful! You can now login.");
        setIsRegister(false);
        setPassword("");
      } else {
        localStorage.setItem("username", data.username);
        localStorage.setItem("role", data.role);

        setMessage(`Welcome, ${data.username}! Login successful.`);

        setTimeout(() => {
          window.location.href = "/dashboard";
        }, 800);
      }
    } catch (error) {
      setMessage(
        "Cannot connect to server. Make sure Spring Boot is running."
      );
    }
  };

  return (
    <div className="app-container">
      <div className="left-panel">
        <div className="brand">
          <div className="brand-icon">+</div>
          <span>Smart Hospital</span>
        </div>

        <div className="left-content">
          <h1>
            Smarter Patient Flow.
            <br />
            Better Hospital Decisions.
          </h1>

          <p>
            Analyze patient flow, identify bottlenecks, predict waiting
            conditions, and support better operational decisions.
          </p>

          <div className="features">
            <div>✓ Patient Flow Analysis</div>
            <div>✓ Bottleneck Detection</div>
            <div>✓ ML-Based Prediction</div>
            <div>✓ Decision Support</div>
          </div>
        </div>
      </div>

      <div className="right-panel">
        <div className="login-card">
          <h2>{isRegister ? "Create Account" : "Welcome Back"}</h2>

          <p className="subtitle">
            {isRegister
              ? "Register as a hospital staff member"
              : "Sign in to access the Smart Hospital dashboard"}
          </p>

          <form onSubmit={handleSubmit}>
            <label>Username</label>

            <input
              type="text"
              placeholder="Enter username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <label>Password</label>

            <input
              type="password"
              placeholder="Enter password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <button type="submit">
              {isRegister ? "Register" : "Login"}
            </button>
          </form>

          {message && <div className="message">{message}</div>}

          <div className="switch-mode">
            {isRegister
              ? "Already have an account?"
              : "Don't have an account?"}

            <button
              type="button"
              className="switch-button"
              onClick={() => {
                setIsRegister(!isRegister);
                setMessage("");
              }}
            >
              {isRegister ? "Login" : "Register"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
