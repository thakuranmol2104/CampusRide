package com.campusride.app;

public class Booking {
    private String bookingId;
    private String rideId;
    private String passengerUid;
    private String passengerName;
    private String driverUid;
    private int seatsBooked;
    private String status; // "pending", "accepted", "rejected", "completed"
    private long timestamp;

    // Required empty constructor for Firestore
    public Booking() {}

    public Booking(String bookingId, String rideId, String passengerUid,
                   String passengerName, String driverUid, int seatsBooked) {
        this.bookingId = bookingId;
        this.rideId = rideId;
        this.passengerUid = passengerUid;
        this.passengerName = passengerName;
        this.driverUid = driverUid;
        this.seatsBooked = seatsBooked;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getPassengerUid() { return passengerUid; }
    public void setPassengerUid(String passengerUid) { this.passengerUid = passengerUid; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getDriverUid() { return driverUid; }
    public void setDriverUid(String driverUid) { this.driverUid = driverUid; }

    public int getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(int seatsBooked) { this.seatsBooked = seatsBooked; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}