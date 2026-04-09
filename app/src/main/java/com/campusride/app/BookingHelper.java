package com.campusride.app;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class BookingHelper {

    private static BookingHelper instance;
    private FirebaseFirestore db;

    private BookingHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static BookingHelper getInstance() {
        if (instance == null) {
            instance = new BookingHelper();
        }
        return instance;
    }

    // ─── REQUEST BOOKING ────────────────────────────────

    public void requestBooking(Booking booking, OnCompleteListener listener) {
        // Auto-generate booking ID
        String bookingId = db.collection("bookings").document().getId();
        booking.setBookingId(bookingId);

        db.collection("bookings").document(bookingId)
                .set(booking)
                .addOnCompleteListener(listener);
    }

    // ─── ACCEPT / REJECT ────────────────────────────────

    public void acceptBooking(String bookingId, String rideId,
                              int seatsBooked, int currentSeats,
                              OnCompleteListener listener) {
        // Update booking status to accepted
        db.collection("bookings").document(bookingId)
                .update("status", "accepted")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Reduce available seats on the ride
                        int newSeats = currentSeats - seatsBooked;
                        RideHelper.getInstance().updateSeats(rideId, newSeats, listener);
                    } else {
                        listener.onComplete(task);
                    }
                });
    }

    public void rejectBooking(String bookingId, OnCompleteListener listener) {
        db.collection("bookings").document(bookingId)
                .update("status", "rejected")
                .addOnCompleteListener(listener);
    }

    // ─── GET BOOKINGS ────────────────────────────────────

    // Get all bookings for a passenger
    public void getBookingsByPassenger(String passengerUid,
                                       OnCompleteListener<QuerySnapshot> listener) {
        db.collection("bookings")
                .whereEqualTo("passengerUid", passengerUid)
                .get()
                .addOnCompleteListener(listener);
    }

    // Get all bookings for a specific ride (driver sees who requested)
    public void getBookingsForRide(String rideId,
                                   OnCompleteListener<QuerySnapshot> listener) {
        db.collection("bookings")
                .whereEqualTo("rideId", rideId)
                .get()
                .addOnCompleteListener(listener);
    }

    // Get pending bookings for a driver's ride
    public void getPendingBookings(String rideId,
                                   OnCompleteListener<QuerySnapshot> listener) {
        db.collection("bookings")
                .whereEqualTo("rideId", rideId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(listener);
    }
}