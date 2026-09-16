import { useState } from "react";
import { API_URL } from "./config";

function ChangePassword() {
  const username = localStorage.getItem("username");
  const role = localStorage.getItem("role");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);

  if (role !== "ADMIN") {
    window.location.href = "/dashboard";
    return null;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage("");
    setSuccess(false);

    if (!currentPassword || !newPassword || !confirmPassword) {
      setMessage("Please fill all fields.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setMessage("New passwords do not match.");
      return;
    }

    if (newPassword.length < 6) {
      setMessage("New password must be at least 6 characters.");
      return;
    }

    try {
      const response = await fetch(
        `${API_URL}/api/auth/change-password`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            username,
            currentPassword,
            newPassword,
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        setMessage(data.message || "Password change failed.");
        return;
      }

      setSuccess(true);
      setMessage("Password changed successfully.");

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (error) {
      console.error("Change password error:", error);
      setMessage("Cannot connect to server.");
    }
  };

  return (
    <div className="change-password-page">

      <div className="change-password-card">

        <div className="change-password-brand">
          <div className="change-password-icon">
            +
          </div>

          <span>
            Smart Hospital
          </span>
        </div>

        <h2>
          Change Password
        </h2>

        <p className="change-password-user">
          Admin: {username}
        </p>

        <form onSubmit={handleSubmit}>

          <label>
            Current Password
          </label>

          <input
            type="password"
            value={currentPassword}
            onChange={(e) =>
              setCurrentPassword(e.target.value)
            }
            placeholder="Enter current password"
          />

          <label>
            New Password
          </label>

          <input
            type="password"
            value={newPassword}
            onChange={(e) =>
              setNewPassword(e.target.value)
            }
            placeholder="Enter new password"
          />

          <label>
            Confirm New Password
          </label>

          <input
            type="password"
            value={confirmPassword}
            onChange={(e) =>
              setConfirmPassword(e.target.value)
            }
            placeholder="Confirm new password"
          />

          <button type="submit">
            Change Password
          </button>

        </form>

        {message && (
          <div
            className={
              success
                ? "password-message success"
                : "password-message"
            }
          >
            {message}
          </div>
        )}

        <button
          className="back-dashboard-button"
          onClick={() => {
            window.location.href = "/dashboard";
          }}
        >
          ← Back to Dashboard
        </button>

      </div>

    </div>
  );
}

export default ChangePassword;