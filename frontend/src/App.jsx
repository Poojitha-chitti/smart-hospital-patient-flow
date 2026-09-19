import { useState } from "react";
import "./App.css";
import Home from "./Home";
import Dashboard from "./Dashboard";
import PatientFlow from "./PatientFlow";
import DataMining from "./DataMining";
import ResourceAnalysis from "./ResourceAnalysis";
import DecisionSupport from "./DecisionSupport";
import { API_URL } from "./config";
import ChangePassword from "./ChangePassword";
import PatientRegistration from "./PatientRegistration";
import WaitingList from "./WaitingList";
import PatientList from "./PatientList";
import DoctorList from "./DoctorList";
import RoomList from "./RoomList";

function App() {
  const role = localStorage.getItem("role");
  const loggedInUser = localStorage.getItem("username");

  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const path = window.location.pathname;

  // Protected Home page
  if (path === "/home") {
    if (!loggedInUser || !role) {
      window.location.href = "/";
      return null;
    }

    return <Home />;
  }

  
  if (path === "/doctor-list") {
    return <DoctorList />;
}
if (path === "/room-list") {
  return <RoomList />;
}
  if (path === "/patient-list") {
    return <PatientList />;
}

  if (path === "/waiting-list") {
    return <WaitingList />;
}

if (path === "/patient-registration") {
    return <PatientRegistration />;
}

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
if (path === "/change-password") {
  if (!loggedInUser || !role) {
    window.location.href = "/";
    return null;
  }

  if (role !== "ADMIN") {
    window.location.href = "/dashboard";
    return null;
  }

  return <ChangePassword />;
}

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");

    if (!username || !password) {
      setMessage("Please enter username and password.");
      return;
    }

    const endpoint = isRegister
  ? `${API_URL}/api/auth/register`
  : `${API_URL}/api/auth/login`;
  
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
  window.location.href = "/home";
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
      <div className="login-card">
        <div className="brand">
          <div className="brand-icon">+</div>
          <span>Smart Hospital</span>
        </div>

        <h2>{isRegister ? "Create Account" : "Login"}</h2>

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
  );
}

export default App;
