package com.campusride.app;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RideHelper {

    private static RideHelper instance;
    private FirebaseFirestore db;

    private RideHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static RideHelper getInstance() {
        if (instance == null) {
            instance = new RideHelper();
        }
        return instance;
    }

    // ─── CREATE ─────────────────────────────────────────

    // Post a new ride
    public void postRide(Ride ride, OnCompleteListener listener) {
        // Auto-generate a unique ride ID
        String rideId = db.collection("rides").document().getId();
        ride.setRideId(rideId);

        db.collection("rides").document(rideId)
                .set(ride)
                .addOnCompleteListener(listener);
    }

    // ─── READ ────────────────────────────────────────────

    // Get all active rides
    public void getActiveRides(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("rides")
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(listener);
    }

    // Get rides posted by a specific driver
    public void getRidesByDriver(String driverUid, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("rides")
                .whereEqualTo("driverUid", driverUid)
                .get()
                .addOnCompleteListener(listener);
    }

    // Search rides by origin and destination
    public void searchRides(String origin, String destination,
                            OnCompleteListener<QuerySnapshot> listener) {
        db.collection("rides")
                .whereEqualTo("origin", origin)
                .whereEqualTo("destination", destination)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(listener);
    }

    // ─── UPDATE ──────────────────────────────────────────

    // Update available seats after booking
    public void updateSeats(String rideId, int newSeatCount, OnCompleteListener listener) {
        db.collection("rides").document(rideId)
                .update("availableSeats", newSeatCount)
                .addOnCompleteListener(listener);
    }

    // Cancel a ride
    public void cancelRide(String rideId, OnCompleteListener listener) {
        db.collection("rides").document(rideId)
                .update("status", "cancelled")
                .addOnCompleteListener(listener);
    }

    // Complete a ride
    public void completeRide(String rideId, OnCompleteListener listener) {
        db.collection("rides").document(rideId)
                .update("status", "completed")
                .addOnCompleteListener(listener);
    }
}