import React, { useEffect, useState } from "react";
import { API_URL } from "./config";
import "./RoomList.css";

function RoomList() {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${API_URL}/api/rooms`)
      .then((response) => response.json())
      .then((data) => {
        setRooms(data);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Error fetching rooms:", error);
        setLoading(false);
      });
  }, []);

  return (
    <div className="room-list-page">
        <button
  className="back-home-button"
  onClick={() => (window.location.href = "/home")}
>
  ← Back to Home
</button>
      <div className="room-list-header">
        <h2>Rooms</h2>
        <p>View hospital consultation room information</p>
      </div>

      {loading ? (
        <p>Loading rooms...</p>
      ) : rooms.length === 0 ? (
        <p>No rooms available.</p>
      ) : (
        <div className="room-table-container">
          <table className="room-table">
            <thead>
              <tr>
                <th>Room ID</th>
                <th>Room No.</th>
                <th>Department</th>
                <th>Room Type</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {rooms.map((room) => (
                <tr key={room.room_id}>
                  <td>{room.room_id}</td>
                  <td>{room.room_no}</td>
                  <td>{room.department}</td>
                  <td>{room.room_type}</td>
                  <td>
                    <span
                      className={
                        room.status === "AVAILABLE"
                          ? "room-status available"
                          : "room-status busy"
                      }
                    >
                      {room.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default RoomList;