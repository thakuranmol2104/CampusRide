package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchRideActivity extends AppCompatActivity {

    private static final int FIELD_ORIGIN = 1;
    private static final int FIELD_DESTINATION = 2;

    private EditText etSearchOrigin;
    private EditText etSearchDestination;
    private Button btnSearchRide;
    private ImageButton btnBack;
    private RecyclerView recyclerRides;

    private final List<Ride> rideList = new ArrayList<>();
    private final List<Ride> allRideList = new ArrayList<>();
    private RideAdapter rideAdapter;
    private int activeField = FIELD_ORIGIN;
    private ActivityResultLauncher<Intent> placePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_ride);

        etSearchOrigin = findViewById(R.id.etSearchOrigin);
        etSearchDestination = findViewById(R.id.etSearchDestination);
        btnSearchRide = findViewById(R.id.btnSearchRide);
        btnBack = findViewById(R.id.btnBack);
        recyclerRides = findViewById(R.id.recyclerRides);

        rideAdapter = new RideAdapter(
                rideList,
                ride -> openRideMap(ride.getOrigin(), ride.getDestination()),
                this::requestBooking,
                "Book Seat"
        );

        recyclerRides.setLayoutManager(new LinearLayoutManager(this));
        recyclerRides.setAdapter(rideAdapter);

        setupPlaceAutocomplete();
        btnBack.setOnClickListener(v -> finish());
        btnSearchRide.setOnClickListener(v -> filterRides());
        loadAvailableRides();
    }

    private void setupPlaceAutocomplete() {
        placePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                        String errorMessage = PlacesAutoCompleteHelper.getPlacesErrorMessage(result.getData());
                        Toast.makeText(
                                this,
                                TextUtils.isEmpty(errorMessage) ? "Unable to search place" : errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }

                    String placeName = PlacesAutoCompleteHelper.getSelectedPlaceName(result.getData());
                    if (TextUtils.isEmpty(placeName)) {
                        Toast.makeText(this, "Unable to fetch place", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (activeField == FIELD_ORIGIN) {
                        etSearchOrigin.setText(placeName);
                    } else if (activeField == FIELD_DESTINATION) {
                        etSearchDestination.setText(placeName);
                    }
                }
        );

        configurePlaceField(etSearchOrigin);
        configurePlaceField(etSearchDestination);

        etSearchOrigin.setOnClickListener(v -> launchPlacePicker(FIELD_ORIGIN));
        etSearchDestination.setOnClickListener(v -> launchPlacePicker(FIELD_DESTINATION));
    }

    private void configurePlaceField(EditText editText) {
        editText.setKeyListener(null);
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setCursorVisible(false);
        editText.setClickable(true);
    }

    private void launchPlacePicker(int fieldType) {
        activeField = fieldType;
        PlacesAutoCompleteHelper.launchAutocomplete(this, placePickerLauncher);
    }

    private void openRideMap(String origin, String destination) {
        Intent intent = new Intent(this, RideMapActivity.class);
        intent.putExtra(RideMapActivity.EXTRA_ORIGIN, origin);
        intent.putExtra(RideMapActivity.EXTRA_DESTINATION, destination);
        startActivity(intent);
    }

    private void filterRides() {
        String origin = etSearchOrigin.getText().toString().trim();
        String destination = etSearchDestination.getText().toString().trim();

        rideList.clear();

        if (TextUtils.isEmpty(origin) && TextUtils.isEmpty(destination)) {
            rideList.addAll(allRideList);
        } else {
            for (Ride ride : allRideList) {
                boolean originMatches = TextUtils.isEmpty(origin)
                        || containsIgnoreCase(ride.getOrigin(), origin);
                boolean destinationMatches = TextUtils.isEmpty(destination)
                        || containsIgnoreCase(ride.getDestination(), destination);

                if (originMatches && destinationMatches) {
                    rideList.add(ride);
                }
            }
        }

        rideAdapter.notifyDataSetChanged();

        if (rideList.isEmpty()) {
            Toast.makeText(this, "No rides found for this route", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.getDefault())
                .contains(query.toLowerCase(Locale.getDefault()));
    }

    private void loadAvailableRides() {
        RideHelper.getInstance().getActiveRides(task -> {
            if (task.isSuccessful()) {
                allRideList.clear();
                rideList.clear();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Ride ride = document.toObject(Ride.class);
                    allRideList.add(ride);
                    rideList.add(ride);
                }

                addLocalActiveRides();
                rideAdapter.notifyDataSetChanged();

                if (rideList.isEmpty()) {
                    Toast.makeText(this, "No active rides available right now", Toast.LENGTH_SHORT).show();
                }
            } else {
                allRideList.clear();
                rideList.clear();
                addLocalActiveRides();
                rideAdapter.notifyDataSetChanged();
                if (rideList.isEmpty()) {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unable to load rides";
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Showing rides saved on this device.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addLocalActiveRides() {
        for (Ride ride : LocalRideStore.getActiveRides(this)) {
            allRideList.add(ride);
            rideList.add(ride);
        }
    }

    private void requestBooking(Ride ride) {
        FirebaseUser currentUser = FirebaseHelper.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login to book a ride", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getUid().equals(ride.getDriverUid())) {
            Toast.makeText(this, "You cannot book your own ride", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ride.getAvailableSeats() <= 0) {
            Toast.makeText(this, "No seats left on this ride", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper.getInstance().getDb().collection("bookings")
                .whereEqualTo("rideId", ride.getRideId())
                .whereEqualTo("passengerUid", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Unable to verify previous bookings", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!task.getResult().isEmpty()) {
                        Toast.makeText(this, "Booking already requested for this ride", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseHelper.getInstance().getDb()
                            .collection("users")
                            .document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String passengerName = "Passenger";
                                if (documentSnapshot.exists()) {
                                    User user = documentSnapshot.toObject(User.class);
                                    if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                                        passengerName = user.getName().trim();
                                    }
                                }

                                Booking booking = new Booking(
                                        "",
                                        ride.getRideId(),
                                        currentUser.getUid(),
                                        passengerName,
                                        ride.getDriverUid(),
                                        1
                                );

                                BookingHelper.getInstance().requestBooking(booking, bookingTask -> {
                                    if (bookingTask.isSuccessful()) {
                                        Toast.makeText(this, "Seat request sent to driver", Toast.LENGTH_SHORT).show();
                                    } else {
                                        String error = bookingTask.getException() != null
                                                ? bookingTask.getException().getMessage()
                                                : "Booking request failed";
                                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                                    }
                                });
                            })
                            .addOnFailureListener(exception ->
                                    Toast.makeText(this, "Unable to load your profile name", Toast.LENGTH_SHORT).show());
                });
    }
}
