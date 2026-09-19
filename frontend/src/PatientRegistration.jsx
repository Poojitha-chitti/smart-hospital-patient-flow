import { useState } from "react";
import "./PatientRegistration.css";
import { API_URL } from "./config";

function PatientRegistration() {
  const [form, setForm] = useState({
    patient_name: "",
    age: "",
    gender: "",
    phone: "",
    department: "OP",
  });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [registeredVisitId, setRegisteredVisitId] = useState(null);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");
    setError("");
    setRegisteredVisitId(null);

    if (!form.patient_name.trim() || !form.age || !form.department) {
      setError("Please fill in patient name, age and department.");
      return;
    }

    try {
      setSaving(true);
      const response = await fetch(`${API_URL}/api/patients/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          patient_name: form.patient_name,
          age: Number(form.age),
          gender: form.gender,
          phone: form.phone,
          department: form.department,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Patient registration failed.");
      }

      setMessage(data.message);
      setRegisteredVisitId(data.visit_id);
      setForm({
        patient_name: "",
        age: "",
        gender: "",
        phone: "",
        department: "OP",
      });
    } catch (err) {
      setError(err.message || "Unable to register patient.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="registration-page">
      <header className="registration-header">
        <div>
          <div className="registration-brand">SMART HOSPITAL</div>
          <h1>Patient Registration</h1>
          <p>Register a patient and create a hospital visit.</p>
        </div>
        <button
          className="registration-back"
          onClick={() => { window.location.href = "/home"; }}
        >
          ← Home
        </button>
      </header>

      <main className="registration-content">
        <section className="registration-card">
          <div className="registration-card-heading">
            <h2>Patient Details</h2>
            <p>Enter the basic information required to start the workflow.</p>
          </div>

          <form onSubmit={handleSubmit} className="registration-form">
            <div className="form-group full-width">
              <label>Patient Name *</label>
              <input
                name="patient_name"
                value={form.patient_name}
                onChange={handleChange}
                placeholder="Enter patient name"
              />
            </div>

            <div className="form-group">
              <label>Age *</label>
              <input
                type="number"
                name="age"
                min="0"
                max="120"
                value={form.age}
                onChange={handleChange}
                placeholder="Enter age"
              />
            </div>

            <div className="form-group">
              <label>Gender</label>
              <select name="gender" value={form.gender} onChange={handleChange}>
                <option value="">Select gender</option>
                <option value="Female">Female</option>
                <option value="Male">Male</option>
                <option value="Other">Other</option>
              </select>
            </div>

            <div className="form-group">
              <label>Phone</label>
              <input
                name="phone"
                value={form.phone}
                onChange={handleChange}
                placeholder="Enter phone number"
                maxLength="15"
              />
            </div>

            <div className="form-group">
              <label>Department *</label>
              <select name="department" value={form.department} onChange={handleChange}>
                <option value="OP">OP Consultation</option>
                <option value="PHARMACY">Pharmacy</option>
                <option value="DIAGNOSTICS">Diagnostics</option>
              </select>
            </div>

            <button className="register-submit" type="submit" disabled={saving}>
              {saving ? "Registering..." : "Register Patient"}
            </button>
          </form>

          {message && <div className="registration-success">{message}</div>}
          {registeredVisitId && (
            <div className="visit-number">Visit ID: <strong>{registeredVisitId}</strong></div>
          )}
          {error && <div className="registration-error">{error}</div>}
        </section>

        <aside className="registration-info">
          <div className="info-icon">✓</div>
          <h3>What happens after registration?</h3>
          <ol>
            <li>A unique Visit ID is created.</li>
            <li>The patient is stored with <strong>WAITING</strong> status.</li>
            <li>The visit becomes part of the hospital workflow.</li>
            <li>The workflow can later be used by the analysis modules.</li>
          </ol>
        </aside>
      </main>
    </div>
  );
}

export default PatientRegistration;
