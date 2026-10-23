import { useEffect, useMemo, useState } from "react";
import "./ResourceAnalysis.css";
import { API_URL } from "./config";

function ResourceAnalysis() {
  const [staff, setStaff] = useState([]);
  const [resources, setResources] = useState([]);
  const [staffUsers, setStaffUsers] = useState([]);
  const [selectedAreas, setSelectedAreas] = useState({});
  const [selectedStaffIds, setSelectedStaffIds] = useState({});
  const [loading, setLoading] = useState(true);
  const [savingUser, setSavingUser] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");

      const [staffResponse, resourceResponse, usersResponse] = await Promise.all([
        fetch(`${API_URL}/api/staff`),
        fetch(`${API_URL}/api/resources`),
        fetch(`${API_URL}/api/admin/users/staff`),
      ]);

      if (!staffResponse.ok || !resourceResponse.ok || !usersResponse.ok) {
        throw new Error("Unable to load staff and resource information.");
      }

      const [staffData, resourceData, usersData] = await Promise.all([
        staffResponse.json(),
        resourceResponse.json(),
        usersResponse.json(),
      ]);

      setStaff(staffData);
      setResources(resourceData);
      setStaffUsers(usersData);

      const areas = {};
      const staffIds = {};

      usersData.forEach((user) => {
        areas[user.user_id] = user.work_area || "";
        staffIds[user.user_id] = user.staff_id ? String(user.staff_id) : "";
      });

      setSelectedAreas(areas);
      setSelectedStaffIds(staffIds);
    } catch (err) {
      setError(err.message || "Unable to load staff and resource information.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const stats = useMemo(() => {
    const totalStaff = staff.length;
    const availableStaff = staff.filter((person) => person.available).length;
    const unavailableStaff = totalStaff - availableStaff;

    const resourceCapacity = resources.reduce(
      (sum, resource) => sum + Number(resource.capacity || 0),
      0
    );

    const resourceAvailable = resources.reduce(
      (sum, resource) => sum + Number(resource.available || 0),
      0
    );

    const busiest = resources.length
      ? [...resources].sort((a, b) => {
          const utilizationA = Number(a.capacity || 0)
            ? (Number(a.capacity || 0) - Number(a.available || 0)) /
              Number(a.capacity || 0)
            : 0;
          const utilizationB = Number(b.capacity || 0)
            ? (Number(b.capacity || 0) - Number(b.available || 0)) /
              Number(b.capacity || 0)
            : 0;
          return utilizationB - utilizationA;
        })[0]
      : null;

    return {
      totalStaff,
      availableStaff,
      unavailableStaff,
      resourceCapacity,
      resourceAvailable,
      busiest,
    };
  }, [staff, resources]);

  const workAreaLabel = (area) => {
  if (area === "REGISTRATION") return "Registration";
  if (area === "OP") return "OP Consultation";
  if (area === "PHARMACY") return "Pharmacy";
  if (area === "DIAGNOSTICS") return "Diagnostics";
  return "Select work area";
};

  const staffOptionsForArea = (area) => {
    if (!area) return [];
    return staff.filter(
      (person) =>
        String(person.department || "").trim().toUpperCase() === area
    );
  };

  const saveAssignment = async (userId) => {
    const workArea = selectedAreas[userId];
    const staffId = selectedStaffIds[userId];

    if (!workArea) {
      setError("Please select a work area.");
      setMessage("");
      return;
    }

    if (!staffId) {
      setError("Please select a staff member.");
      setMessage("");
      return;
    }

    try {
      setSavingUser(userId);
      setError("");
      setMessage("");

      const workAreaResponse = await fetch(
        `${API_URL}/api/admin/users/${userId}/work-area`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ work_area: workArea }),
        }
      );

      const workAreaData = await workAreaResponse.json().catch(() => ({}));

      if (!workAreaResponse.ok) {
        throw new Error(
          workAreaData.message || "Unable to update work area."
        );
      }

      const staffIdResponse = await fetch(
        `${API_URL}/api/admin/users/${userId}/staff-id`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ staff_id: Number(staffId) }),
        }
      );

      const staffIdData = await staffIdResponse.json().catch(() => ({}));

      if (!staffIdResponse.ok) {
        throw new Error(
          staffIdData.message || "Unable to link staff member."
        );
      }

      setMessage("Staff account assignment saved successfully.");
      await loadData();
    } catch (err) {
      setError(err.message || "Unable to save staff account assignment.");
    } finally {
      setSavingUser(null);
    }
  };

  const handleAreaChange = (userId, area) => {
    setSelectedAreas((current) => ({ ...current, [userId]: area }));

    const matchingStaff = staffOptionsForArea(area);
    const currentStaffId = String(selectedStaffIds[userId] || "");
    const stillValid = matchingStaff.some(
      (person) => String(person.staff_id) === currentStaffId
    );

    if (!stillValid) {
      setSelectedStaffIds((current) => ({
        ...current,
        [userId]: matchingStaff[0] ? String(matchingStaff[0].staff_id) : "",
      }));
    }
  };

  if (loading) {
    return (
      <div className="resource-page">
        <main className="resource-main">
          <div className="resource-empty">Loading staff and resource information...</div>
        </main>
      </div>
    );
  }

  if (error && !staff.length && !resources.length && !staffUsers.length) {
    return (
      <div className="resource-page">
        <main className="resource-main">
          <div className="resource-empty resource-error">{error}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="resource-page">
      <main className="resource-main">
        <div className="resource-top-row">
          <div>
            <h1>Staff &amp; Resources</h1>
            <p>Monitor staff availability and hospital resources.</p>
          </div>

          <button
  className="resource-home-btn"
  onClick={() =>
    (window.location.href = "/home?view=operations")
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

        <div className="resource-summary">
          <strong>Staff and Resource Availability</strong>
          <span>View the current availability of staff members and hospital resources.</span>
        </div>

        {message && <div className="resource-message">{message}</div>}
        {error && <div className="resource-inline-error">{error}</div>}

        <section>
          <div className="resource-section-head">
            <h2>Staff Account Assignment</h2>
            <p>Assign each staff login to the hospital area they handle and link it to the matching staff record.</p>
          </div>

          <div className="staff-assignment-card">
            <div className="staff-assignment-table-wrap">
              <table className="staff-assignment-table">
                <thead>
                  <tr>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Work Area</th>
                    <th>Staff Member</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {staffUsers.map((user) => {
                    const area = selectedAreas[user.user_id] || "";
                    const areaStaff = staffOptionsForArea(area);

                    return (
                      <tr key={user.user_id}>
                        <td className="assignment-username">{user.username}</td>
                        <td className="assignment-email">{user.email || "No email"}</td>
                        <td>
                          <select
                            className="assignment-select"
                            value={area}
                            onChange={(e) => handleAreaChange(user.user_id, e.target.value)}
                            disabled={savingUser === user.user_id}
                          >
                            <option value="">Select area</option>
                            <option value="REGISTRATION">Registration</option>
<option value="OP">OP Consultation</option>
<option value="PHARMACY">Pharmacy</option>
<option value="DIAGNOSTICS">Diagnostics</option>
                          </select>
                        </td>
                        <td>
                          <select
                            className="assignment-select"
                            value={selectedStaffIds[user.user_id] || ""}
                            onChange={(e) =>
                              setSelectedStaffIds((current) => ({
                                ...current,
                                [user.user_id]: e.target.value,
                              }))
                            }
                            disabled={savingUser === user.user_id || !area}
                          >
                            <option value="">Select staff</option>
                            {areaStaff.map((person) => (
                              <option key={person.staff_id} value={person.staff_id}>
                                {person.staff_name} — {workAreaLabel(area)}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <button
                            className="assignment-save-btn"
                            onClick={() => saveAssignment(user.user_id)}
                            disabled={savingUser === user.user_id}
                          >
                            {savingUser === user.user_id ? "Saving..." : "Save"}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <section>
          <div className="resource-section-head">
            <h2>Availability Summary</h2>
          </div>

          <div className="resource-fact-grid">
            <div className="resource-fact-card">
              <div className="big">{stats.availableStaff} of {stats.totalStaff}</div>
              <div className="caption">Staff members available</div>
            </div>
            <div className="resource-fact-card">
              <div className="big">{stats.resourceAvailable} of {stats.resourceCapacity}</div>
              <div className="caption">Resource slots available</div>
            </div>
            <div className="resource-fact-card">
              <div className="big">{stats.unavailableStaff}</div>
              <div className="caption">Staff members unavailable</div>
            </div>
          </div>
        </section>

        <section>
          <div className="resource-section-head">
            <h2>Staff Availability</h2>
            <p>Current availability of registered hospital staff.</p>
          </div>

          <div className="resource-people-list">
            {staff.map((person) => (
              <div className="resource-person-row" key={person.staff_id}>
                <div className="resource-person-who">
                  <strong>{person.staff_name}</strong>
                  <span>{person.role}</span>
                </div>
                <div className="resource-person-where">{person.department}</div>
                <span className={`resource-status-dot ${person.available ? "free" : "busy"}`}>
                  {person.available ? "Available" : "Unavailable"}
                </span>
              </div>
            ))}
          </div>
        </section>

        <section>
          <div className="resource-section-head">
            <h2>Hospital Resources</h2>
            <p>Current availability of counters and consultation rooms.</p>
          </div>

          <div className="resource-room-grid">
            {resources.map((resource) => {
              const capacity = Number(resource.capacity || 0);
              const available = Number(resource.available || 0);
              const utilization = capacity
                ? ((capacity - available) / capacity) * 100
                : 0;

              return (
                <div className="resource-room-card" key={resource.resource_id}>
                  <span className="dept">{resource.department}</span>
                  <div className="name">{resource.resource_type}</div>
                  <div className="count">
                    <span className="n">{available}</span>
                    <span className="label">of {capacity} available</span>
                  </div>
                  <div className="resource-card-label">Used: {capacity - available} of {capacity}</div>
                  <div className="util-bar">
                    <div
                      className="util-fill"
                      style={{ width: `${Math.min(100, Math.max(0, utilization))}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      </main>
    </div>
  );
}

export default ResourceAnalysis;
