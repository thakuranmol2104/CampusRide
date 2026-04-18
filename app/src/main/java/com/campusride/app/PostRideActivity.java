package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.android.libraries.places.widget.AutocompleteActivity;

public class PostRideActivity extends AppCompatActivity {

    private static final int FIELD_ORIGIN = 1;
    private static final int FIELD_DESTINATION = 2;

    private EditText etOrigin, etDestination, etDate, etTime, etSeats, etPrice;
    private Button btnPostRide;
    private int activeField = FIELD_ORIGIN;
    private ActivityResultLauncher<Intent> placePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_ride);

        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etSeats = findViewById(R.id.etSeats);
        etPrice = findViewById(R.id.etPrice);
        btnPostRide = findViewById(R.id.btnPostRide);

        setupPlaceAutocomplete();
        btnPostRide.setOnClickListener(v -> postRide());
    }

    private void setupPlaceAutocomplete() {
        placePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                        launchFallbackAutocomplete();
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
                        etOrigin.setText(placeName);
                    } else if (activeField == FIELD_DESTINATION) {
                        etDestination.setText(placeName);
                    }
                }
        );

        configurePlaceField(etOrigin);
        configurePlaceField(etDestination);

        etOrigin.setOnClickListener(v -> launchPlacePicker(FIELD_ORIGIN));
        etDestination.setOnClickListener(v -> launchPlacePicker(FIELD_DESTINATION));
    }

    private void configurePlaceField(EditText editText) {
        editText.setKeyListener(null);
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setCursorVisible(false);
        editText.setClickable(true);
    }

    private void launchPlacePicker(int fieldType) {
        try {
            activeField = fieldType;
            PlacesAutoCompleteHelper.launchAutocomplete(this, placePickerLauncher);
        } catch (IllegalStateException exception) {
            launchFallbackAutocomplete();
        }
    }

    private void launchFallbackAutocomplete() {
        PlacesAutoCompleteHelper.launchFallbackAutocomplete(this, placePickerLauncher);
    }

    private void postRide() {
        String origin = etOrigin.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (TextUtils.isEmpty(origin)) {
            etOrigin.setError("Enter origin");
            etOrigin.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(destination)) {
            etDestination.setError("Enter destination");
            etDestination.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(date)) {
            etDate.setError("Enter date");
            etDate.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(time)) {
            etTime.setError("Enter time");
            etTime.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(seatsStr)) {
            etSeats.setError("Enter seats");
            etSeats.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Enter price");
            etPrice.requestFocus();
            return;
        }

        int availableSeats;
        double pricePerSeat;

        try {
            availableSeats = Integer.parseInt(seatsStr);
        } catch (Exception e) {
            etSeats.setError("Enter valid number");
            etSeats.requestFocus();
            return;
        }

        try {
            pricePerSeat = Double.parseDouble(priceStr);
        } catch (Exception e) {
            etPrice.setError("Enter valid price");
            etPrice.requestFocus();
            return;
        }

        FirebaseUser currentUser = FirebaseHelper.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String driverUid = currentUser.getUid();
        String driverName = currentUser.getEmail();

        Ride ride = new Ride(
                "",
                driverUid,
                driverName,
                origin,
                destination,
                date,
                time,
                availableSeats,
                pricePerSeat
        );

        RideHelper.getInstance().postRide(ride, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(PostRideActivity.this, "Ride posted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String error = task.getException() != null
                        ? task.getException().getMessage()
                        : "Failed to post ride";
                Toast.makeText(PostRideActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
