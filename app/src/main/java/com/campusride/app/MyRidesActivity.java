package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyRidesActivity extends AppCompatActivity {

    private RecyclerView recyclerMyRides;
    private RecyclerView recyclerBookingRequests;
    private RecyclerView recyclerPassengerBookings;

    private final List<Ride> myRideList = new ArrayList<>();
    private final List<DriverBookingRequestAdapter.DriverBookingRequestItem> bookingRequestItems = new ArrayList<>();
    private final List<PassengerBookingAdapter.PassengerBookingItem> passengerBookingItems = new ArrayList<>();
    private final Map<String, Ride> rideById = new HashMap<>();

    private RideAdapter rideAdapter;
    private DriverBookingRequestAdapter driverBookingRequestAdapter;
    private PassengerBookingAdapter passengerBookingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        recyclerMyRides = findViewById(R.id.recyclerMyRides);
        recyclerBookingRequests = findViewById(R.id.recyclerBookingRequests);
        recyclerPassengerBookings = findViewById(R.id.recyclerPassengerBookings);

        rideAdapter = new RideAdapter(myRideList, ride -> openRideMap(ride.getOrigin(), ride.getDestination()));
        driverBookingRequestAdapter = new DriverBookingRequestAdapter(new DriverBookingRequestAdapter.OnBookingActionListener() {
            @Override
            public void onAccept(DriverBookingRequestAdapter.DriverBookingRequestItem item) {
                acceptBooking(item);
            }

            @Override
            public void onReject(DriverBookingRequestAdapter.DriverBookingRequestItem item) {
                rejectBooking(item);
            }
        });
        passengerBookingAdapter = new PassengerBookingAdapter();

        recyclerMyRides.setLayoutManager(new LinearLayoutManager(this));
        recyclerBookingRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerPassengerBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerMyRides.setNestedScrollingEnabled(false);
        recyclerBookingRequests.setNestedScrollingEnabled(false);
        recyclerPassengerBookings.setNestedScrollingEnabled(false);

        recyclerMyRides.setAdapter(rideAdapter);
        recyclerBookingRequests.setAdapter(driverBookingRequestAdapter);
        recyclerPassengerBookings.setAdapter(passengerBookingAdapter);

        loadDashboard();
    }

    private void loadDashboard() {
        FirebaseUser currentUser = FirebaseHelper.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        loadMyPostedRides(currentUser.getUid());
        loadPassengerBookings(currentUser.getUid());
    }

    private void loadMyPostedRides(String driverUid) {
        RideHelper.getInstance().getRidesByDriver(driverUid, task -> {
            if (!task.isSuccessful()) {
                String error = task.getException() != null
                        ? task.getException().getMessage()
                        : "Failed to load rides";
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                return;
            }

            myRideList.clear();
            rideById.clear();

            for (QueryDocumentSnapshot document : task.getResult()) {
                Ride ride = document.toObject(Ride.class);
                myRideList.add(ride);
                rideById.put(ride.getRideId(), ride);
            }

            rideAdapter.notifyDataSetChanged();
            loadPendingRequests();
        });
    }

    private void loadPendingRequests() {
        bookingRequestItems.clear();

        if (myRideList.isEmpty()) {
            driverBookingRequestAdapter.submitList(bookingRequestItems);
            return;
        }

        final int[] completed = {0};
        for (Ride ride : myRideList) {
            BookingHelper.getInstance().getPendingBookings(ride.getRideId(), task -> {
                completed[0]++;

                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Booking booking = document.toObject(Booking.class);
                        bookingRequestItems.add(new DriverBookingRequestAdapter.DriverBookingRequestItem(booking, ride));
                    }
                }

                if (completed[0] == myRideList.size()) {
                    driverBookingRequestAdapter.submitList(new ArrayList<>(bookingRequestItems));
                }
            });
        }
    }

    private void loadPassengerBookings(String passengerUid) {
        BookingHelper.getInstance().getBookingsByPassenger(passengerUid, task -> {
            if (!task.isSuccessful()) {
                return;
            }

            List<Booking> bookings = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
                bookings.add(document.toObject(Booking.class));
            }

            if (bookings.isEmpty()) {
                passengerBookingAdapter.submitList(new ArrayList<>());
                return;
            }

            passengerBookingItems.clear();
            final int[] completed = {0};

            for (Booking booking : bookings) {
                FirebaseHelper.getInstance().getDb().collection("rides")
                        .document(booking.getRideId())
                        .get()
                        .addOnCompleteListener(rideTask -> {
                            completed[0]++;

                            if (rideTask.isSuccessful()) {
                                DocumentSnapshot snapshot = rideTask.getResult();
                                if (snapshot != null && snapshot.exists()) {
                                    Ride ride = snapshot.toObject(Ride.class);
                                    if (ride != null) {
                                        passengerBookingItems.add(new PassengerBookingAdapter.PassengerBookingItem(
                                                ride.getOrigin() + " -> " + ride.getDestination(),
                                                ride.getDriverName(),
                                                ride.getDate() + " | " + ride.getTime(),
                                                booking.getStatus()
                                        ));
                                    }
                                }
                            }

                            if (completed[0] == bookings.size()) {
                                passengerBookingAdapter.submitList(new ArrayList<>(passengerBookingItems));
                            }
                        });
            }
        });
    }

    private void acceptBooking(DriverBookingRequestAdapter.DriverBookingRequestItem item) {
        Ride ride = item.getRide();
        Booking booking = item.getBooking();

        if (ride.getAvailableSeats() < booking.getSeatsBooked()) {
            Toast.makeText(this, "Not enough seats left", Toast.LENGTH_SHORT).show();
            return;
        }

        BookingHelper.getInstance().acceptBooking(
                booking.getBookingId(),
                ride.getRideId(),
                booking.getSeatsBooked(),
                ride.getAvailableSeats(),
                task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Booking accepted", Toast.LENGTH_SHORT).show();
                        loadDashboard();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Failed to accept booking";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void rejectBooking(DriverBookingRequestAdapter.DriverBookingRequestItem item) {
        BookingHelper.getInstance().rejectBooking(item.getBooking().getBookingId(), task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Booking rejected", Toast.LENGTH_SHORT).show();
                loadDashboard();
            } else {
                String error = task.getException() != null
                        ? task.getException().getMessage()
                        : "Failed to reject booking";
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openRideMap(String origin, String destination) {
        Intent intent = new Intent(this, RideMapActivity.class);
        intent.putExtra(RideMapActivity.EXTRA_ORIGIN, origin);
        intent.putExtra(RideMapActivity.EXTRA_DESTINATION, destination);
        startActivity(intent);
    }
}
