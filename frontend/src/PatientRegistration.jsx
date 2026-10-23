import { useState } from "react";
import "./PatientRegistration.css";
import { API_URL } from "./config";

function PatientRegistration() {
  const [form, setForm] = useState({
    patient_name: "",
    age: "",
    gender: "",
    phone: "",
    department: "GENERAL MEDICINE"
  });

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [registeredVisitId, setRegisteredVisitId] = useState(null);

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage("");
    setError("");
    setRegisteredVisitId(null);

    if (
      !form.patient_name.trim() ||
      !form.age ||
      !form.department
    ) {
      setError(
        "Please fill in patient name, age and department."
      );
      return;
    }

    try {
      setSaving(true);

      const response = await fetch(
        `${API_URL}/api/patients/register`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            patient_name: form.patient_name,
            age: Number(form.age),
            gender: form.gender,
            phone: form.phone,
            department: form.department
          })
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.message || "Patient registration failed."
        );
      }

      setMessage(data.message || "Patient registered successfully.");

      // Show the display Visit ID, not the internal database ID.
      setRegisteredVisitId(
        data.display_visit_id ||
        data.displayVisitId ||
        data.visit_display_id ||
        data.visit_id ||
        data.patient_id
      );

      setForm({
        patient_name: "",
        age: "",
        gender: "",
        phone: "",
        department: "GENERAL MEDICINE"
      });

    } catch (err) {
      setError(
        err.message || "Unable to register patient."
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="registration-page">
      <main className="registration-main">

        <div className="registration-top-row">
          <div className="registration-titles">
            <h1>Patient Registration</h1>
            <p>
              Register a patient and create a hospital visit.
            </p>
          </div>

          <button
  className="registration-home-btn"
  onClick={() => window.location.href = "/home?view=patients"}
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

        <div className="registration-layout">
          <div className="registration-form-card">
            <form onSubmit={handleSubmit}>

              <div className="registration-field">
                <label>
                  Patient Name <span>*</span>
                </label>

                <input
                  name="patient_name"
                  value={form.patient_name}
                  onChange={handleChange}
                  placeholder="Enter patient name"
                />
              </div>

              <div className="registration-field-row">
                <div className="registration-field">
                  <label>
                    Age <span>*</span>
                  </label>

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

                <div className="registration-field">
                  <label>Gender</label>

                  <select
                    name="gender"
                    value={form.gender}
                    onChange={handleChange}
                  >
                    <option value="">Select gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div className="registration-field-row">
                <div className="registration-field">
                  <label>Phone</label>

                  <input
                    name="phone"
                    value={form.phone}
                    onChange={handleChange}
                    placeholder="Enter phone number"
                    maxLength="15"
                  />
                </div>

                <div className="registration-field">
                  <label>
                    Department <span>*</span>
                  </label>

                  <select
                    name="department"
                    value={form.department}
                    onChange={handleChange}
                  >
                    <option value="GENERAL MEDICINE">
                      General Medicine
                    </option>
                    <option value="CARDIOLOGY">Cardiology</option>
                    <option value="ORTHOPEDICS">Orthopedics</option>
                    <option value="PEDIATRICS">Pediatrics</option>
                    <option value="GYNECOLOGY">Gynecology</option>
                    <option value="ENT">ENT</option>
                    <option value="OPHTHALMOLOGY">Ophthalmology</option>
                    <option value="DERMATOLOGY">Dermatology</option>
                    <option value="NEUROLOGY">Neurology</option>
                    <option value="PULMONOLOGY">Pulmonology</option>
                    <option value="GASTROENTEROLOGY">
                      Gastroenterology
                    </option>
                    <option value="UROLOGY">Urology</option>
                    <option value="ONCOLOGY">Oncology</option>
                    <option value="DENTAL">Dental</option>
                    <option value="EMERGENCY">Emergency</option>
                  </select>
                </div>
              </div>

              <button
                className="registration-submit-btn"
                type="submit"
                disabled={saving}
              >
                {saving ? "Registering..." : "Register Patient"}
              </button>

              {message && (
                <div className="registration-success">
                  {message}
                </div>
              )}

              {registeredVisitId && (
                <div className="registration-visit">
                  Visit ID:{" "}
                  <strong>{registeredVisitId}</strong>
                </div>
              )}

              {error && (
                <div className="registration-error">
                  {error}
                </div>
              )}

            </form>
          </div>

          <div className="registration-info-card">
            <h2>What happens next</h2>

            <ul className="registration-steps">
              <li>
                <span>1</span>
                <span>
                  A unique <strong>Visit ID</strong> is created.
                </span>
              </li>

              <li>
                <span>2</span>
                <span>
                  The patient is stored with the appropriate
                  waiting status.
                </span>
              </li>

              <li>
                <span>3</span>
                <span>
                  The visit joins the <strong>hospital workflow</strong>.
                </span>
              </li>

              <li>
                <span>4</span>
                <span>
                  Analysis modules can use it from here.
                </span>
              </li>
            </ul>
          </div>
        </div>

      </main>
    </div>
  );
}

export default PatientRegistration;