import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./RoomList.css";

function RoomList() {

  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [updatingRoom, setUpdatingRoom] = useState(null);

  const role =
    localStorage.getItem("role") || "";

  const username =
    localStorage.getItem("username") || "";


  const loadRooms = async () => {

    try {

      setLoading(true);

      const response = await fetch(
        `${API_URL}/api/rooms`
      );

      if (!response.ok) {
        throw new Error();
      }

      const data = await response.json();

      setRooms(data);
      setError("");

    } catch {

      setError(
        "Unable to load room list."
      );

    } finally {

      setLoading(false);

    }
  };


  useEffect(() => {

    loadRooms();

  }, []);


  const updateRoomStatus = async (
    roomId,
    status
  ) => {

    try {

      setUpdatingRoom(roomId);
      setError("");

      const response = await fetch(
        `${API_URL}/api/rooms/${roomId}/status`,
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
            "Unable to update room status."
        );

      }


      await loadRooms();

    } catch (error) {

      setError(
        error.message ||
          "Unable to update room status."
      );

    } finally {

      setUpdatingRoom(null);

    }
  };


  const getStatusClass = (status) => {

    if (status === "AVAILABLE") {
      return "available";
    }

    if (status === "MAINTENANCE") {
      return "maintenance";
    }

    return "occupied";
  };


  const getStatusText = (status) => {

    if (status === "AVAILABLE") {
      return "Available";
    }

    if (status === "MAINTENANCE") {
      return "Maintenance";
    }

    return "Occupied";
  };


  return (

    <div className="room-page">

      <main className="room-main">

        <div className="room-top-row">

          <div>

            <h1>
              Room List
            </h1>

            <p>
              Rooms available in the hospital.
            </p>

          </div>


          <button
  className="room-home-btn"
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


        <div className="room-stat">

          <span>
            Total rooms
          </span>

          <strong>
            {rooms.length}
          </strong>

        </div>


        {loading && (

          <div className="room-empty">

            Loading rooms...

          </div>

        )}


        {error && (

          <div className="room-empty room-error">

            {error}

          </div>

        )}


        {!loading &&
          !error &&
          !rooms.length && (

            <div className="room-empty">

              No rooms found.

              <br />

              Room information will appear here.

            </div>

          )}


        {!loading &&
          !error &&
          rooms.length > 0 && (

            <div className="room-list">

              {rooms.map((room) => (

                <div
                  className="room-row"
                  key={room.room_id}
                >

                  <div className="room-number">

                    <strong>
                      {room.room_no}
                    </strong>

                  </div>


                  <div className="room-info">

                    <div className="name">

                      {room.room_type}

                    </div>

                    <div className="spec">

                      {room.department}

                    </div>

                  </div>


                  <span className="room-dept-tag">

                    {room.department}

                  </span>


                  {role === "ADMIN" ? (

                    <select
                      className="room-status-select"
                      value={
                        room.status ||
                        "AVAILABLE"
                      }
                      disabled={
                        updatingRoom ===
                        room.room_id
                      }
                      onChange={(e) =>
                        updateRoomStatus(
                          room.room_id,
                          e.target.value
                        )
                      }
                    >

                      <option value="AVAILABLE">
                        Available
                      </option>

                      <option value="OCCUPIED">
                        Occupied
                      </option>

                      <option value="MAINTENANCE">
                        Maintenance
                      </option>

                    </select>

                  ) : (

                    <span
                      className={`room-status-pill ${getStatusClass(
                        room.status
                      )}`}
                    >

                      {getStatusText(
                        room.status
                      )}

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

export default RoomList;