package com.campusride.app;

public class Ride {
    private String rideId;
    private String driverUid;
    private String driverName;
    private String origin;
    private String destination;
    private String date;
    private String time;
    private int availableSeats;
    private double pricePerSeat;
    private String status; // "active", "completed", "cancelled"

    // Required empty constructor for Firestore
    public Ride() {}

    public Ride(String rideId, String driverUid, String driverName,
                String origin, String destination, String date,
                String time, int availableSeats, double pricePerSeat) {
        this.rideId = rideId;
        this.driverUid = driverUid;
        this.driverName = driverName;
        this.origin = origin;
        this.destination = destination;
        this.date = date;
        this.time = time;
        this.availableSeats = availableSeats;
        this.pricePerSeat = pricePerSeat;
        this.status = "active";
    }

    // Getters and Setters
    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getDriverUid() { return driverUid; }
    public void setDriverUid(String driverUid) { this.driverUid = driverUid; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public double getPricePerSeat() { return pricePerSeat; }
    public void setPricePerSeat(double pricePerSeat) { this.pricePerSeat = pricePerSeat; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}