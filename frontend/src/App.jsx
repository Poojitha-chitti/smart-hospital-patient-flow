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
  const [isForgotPassword, setIsForgotPassword] = useState(false);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");

  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [otpVerified, setOtpVerified] = useState(false);

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const path = window.location.pathname;

  if (path === "/home") {
    if (!loggedInUser || !role) {
      window.location.href = "/";
      return null;
    }
    return <Home />;
  }

  if (path === "/doctor-list") return <DoctorList />;
  if (path === "/room-list") return <RoomList />;
  if (path === "/patient-list") return <PatientList />;
  if (path === "/waiting-list") return <WaitingList />;
  if (path === "/patient-registration") return <PatientRegistration />;

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

    if (!username || !password || (isRegister && !email)) {
      setMessage(
        isRegister
          ? "Please enter username, email and password."
          : "Please enter username and password."
      );
      return;
    }

    const endpoint = isRegister
      ? `${API_URL}/api/auth/register`
      : `${API_URL}/api/auth/login`;

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username,
          password,
          ...(isRegister ? { email } : {}),
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
        setEmail("");
      } else {
        localStorage.setItem("username", data.username);
        localStorage.setItem("role", data.role);
        setMessage(`Welcome, ${data.username}! Login successful.`);

        setTimeout(() => {
          window.location.href = "/home";
        }, 800);
      }
    } catch (error) {
      setMessage("Cannot connect to server. Make sure Spring Boot is running.");
    }
  };

  const handleForgotPassword = async (e) => {
    e.preventDefault();
    setMessage("");

    if (!email.trim()) {
      setMessage("Please enter your registered email.");
      return;
    }

    try {
      const response = await fetch(`${API_URL}/api/auth/forgot-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: email.trim(),
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setMessage(data.message || "Unable to send OTP.");
        return;
      }

      setMessage("OTP sent successfully to your registered email.");
      setOtpSent(true);
      setOtpVerified(false);
      setOtp("");
    } catch (error) {
      setMessage("Cannot connect to server. Make sure Spring Boot is running.");
    }
  };

  const handleVerifyOtp = async () => {
    setMessage("");

    if (!otp.trim()) {
      setMessage("Please enter the OTP.");
      return;
    }

    if (otp.length !== 6) {
      setMessage("Please enter the 6-digit OTP.");
      return;
    }

    try {
      const response = await fetch(`${API_URL}/api/auth/verify-otp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: email.trim(),
          otp: otp.trim(),
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setMessage(data.message || "Invalid OTP.");
        return;
      }

      setMessage("OTP verified successfully.");
      setOtpVerified(true);
    } catch (error) {
      setMessage("Cannot connect to server. Make sure Spring Boot is running.");
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setMessage("");

    if (!newPassword || !confirmPassword) {
      setMessage("Please enter and confirm your new password.");
      return;
    }

    if (newPassword.length < 6) {
      setMessage("New password must be at least 6 characters.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setMessage("Passwords do not match.");
      return;
    }

    try {
      const response = await fetch(`${API_URL}/api/auth/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: email.trim(),
          newPassword: newPassword,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setMessage(data.message || "Unable to reset password.");
        return;
      }

      setMessage("Password reset successfully. You can now login.");

      setTimeout(() => {
        setIsForgotPassword(false);
        setIsRegister(false);
        setEmail("");
        setOtp("");
        setOtpSent(false);
        setOtpVerified(false);
        setNewPassword("");
        setConfirmPassword("");
        setMessage("");
      }, 1500);

    } catch (error) {
      setMessage("Cannot connect to server. Make sure Spring Boot is running.");
    }
  };

  const showForgotPassword = () => {
    setIsForgotPassword(true);
    setIsRegister(false);
    setUsername("");
    setPassword("");
    setEmail("");
    setOtp("");
    setOtpSent(false);
    setOtpVerified(false);
    setNewPassword("");
    setConfirmPassword("");
    setMessage("");
  };

  const backToLogin = () => {
    setIsForgotPassword(false);
    setIsRegister(false);
    setUsername("");
    setPassword("");
    setEmail("");
    setOtp("");
    setOtpSent(false);
    setOtpVerified(false);
    setNewPassword("");
    setConfirmPassword("");
    setMessage("");
  };

  return (
    <div className="login-page">
      <div className="login-frame">

        <section className="login-brand" aria-label="Smart Hospital">
          <div className="login-brand-name">
            <span className="brand-cross" aria-hidden="true">
              +
            </span>
            <span>Smart Hospital</span>
          </div>

          <div className="hospital-visual">
            <img
              src="/hospital-illustration.svg"
              alt="Hospital building illustration"
            />
          </div>
        </section>

        <section className="login-panel">

          {isForgotPassword ? (
            <>
              <h1>Forgot Password</h1>

              <p className="login-subtitle">
                {otpVerified
                  ? "Create a new password for your account."
                  : "Enter your registered email to receive an OTP."}
              </p>

              {!otpVerified && (
                <>
                  <form onSubmit={handleForgotPassword}>

                    <label htmlFor="forgot-email">
                      Registered Email
                    </label>

                    <input
                      id="forgot-email"
                      type="email"
                      placeholder="Enter your registered email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      autoComplete="email"
                    />

                    <button
                      type="submit"
                      className="login-submit"
                    >
                      Send OTP
                    </button>

                  </form>

                  {otpSent && (
                    <div className="otp-section">

                      <label htmlFor="otp">
                        Enter OTP
                      </label>

                      <input
                        id="otp"
                        type="text"
                        placeholder="Enter 6-digit OTP"
                        value={otp}
                        onChange={(e) =>
                          setOtp(e.target.value.replace(/\D/g, ""))
                        }
                        maxLength={6}
                        inputMode="numeric"
                      />

                      <button
                        type="button"
                        className="login-submit"
                        onClick={handleVerifyOtp}
                      >
                        Verify OTP
                      </button>

                    </div>
                  )}
                </>
              )}

              {otpVerified && (
                <form onSubmit={handleResetPassword}>

                  <label htmlFor="new-password">
                    New Password
                  </label>

                  <input
                    id="new-password"
                    type="password"
                    placeholder="Enter new password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    autoComplete="new-password"
                  />

                  <label htmlFor="confirm-password">
                    Confirm New Password
                  </label>

                  <input
                    id="confirm-password"
                    type="password"
                    placeholder="Confirm new password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    autoComplete="new-password"
                  />

                  <button
                    type="submit"
                    className="login-submit"
                  >
                    Reset Password
                  </button>

                </form>
              )}

              {message && (
                <div
                  className="login-message"
                  aria-live="polite"
                >
                  {message}
                </div>
              )}

              <p className="login-switch">
                Remember your password?

                <button
                  type="button"
                  className="text-link switch-link"
                  onClick={backToLogin}
                >
                  Back to Login
                </button>
              </p>
            </>
          ) : (
            <>
              <h1>
                {isRegister ? "Create Account" : "Welcome back"}
              </h1>

              <p className="login-subtitle">
                {isRegister
                  ? "Create an account for authorized hospital staff."
                  : "Enter your credentials to access your dashboard."}
              </p>

              <form onSubmit={handleSubmit}>

                <label htmlFor="username">
                  Username
                </label>

                <input
                  id="username"
                  type="text"
                  placeholder="Enter username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                />

                {isRegister && (
                  <>
                    <label htmlFor="email">
                      Email
                    </label>

                    <input
                      id="email"
                      type="email"
                      placeholder="Enter email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      autoComplete="email"
                    />
                  </>
                )}

                <label htmlFor="password">
                  Password
                </label>

                <input
                  id="password"
                  type="password"
                  placeholder="Enter password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete={
                    isRegister
                      ? "new-password"
                      : "current-password"
                  }
                />

                {!isRegister && (
                  <div className="login-links">
                    <button
                      type="button"
                      className="text-link"
                      onClick={showForgotPassword}
                    >
                      Forgot password?
                    </button>
                  </div>
                )}

                <button
                  type="submit"
                  className="login-submit"
                >
                  {isRegister
                    ? "Create Account"
                    : "Sign in"}
                </button>

              </form>

              {message && (
                <div
                  className="login-message"
                  aria-live="polite"
                >
                  {message}
                </div>
              )}

              <p className="login-switch">

                {isRegister
                  ? "Already have an account?"
                  : "Don't have an account?"}

                <button
                  type="button"
                  className="text-link switch-link"
                  onClick={() => {
                    setIsRegister(!isRegister);
                    setMessage("");
                    setEmail("");
                  }}
                >
                  {isRegister
                    ? "Sign in"
                    : "Create Account"}
                </button>

              </p>
            </>
          )}

        </section>
      </div>
    </div>
  );
}

export default App;