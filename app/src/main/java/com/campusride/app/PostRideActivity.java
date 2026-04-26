package com.campusride.app;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.android.libraries.places.widget.AutocompleteActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PostRideActivity extends AppCompatActivity {

    private static final int FIELD_ORIGIN = 1;
    private static final int FIELD_DESTINATION = 2;

    private EditText etOrigin, etDestination, etDate, etTime, etSeats, etPrice;
    private Button btnPostRide;
    private ImageButton btnBack;
    private int activeField = FIELD_ORIGIN;
    private ActivityResultLauncher<Intent> placePickerLauncher;
    private final Calendar selectedCalendar = Calendar.getInstance();
    private boolean isPosting;

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
        btnBack = findViewById(R.id.btnBack);

        setupPlaceAutocomplete();
        setupDateAndTimePickers();
        btnBack.setOnClickListener(v -> finish());
        btnPostRide.setOnClickListener(v -> postRide());
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

    private void setupDateAndTimePickers() {
        configurePickerField(etDate);
        configurePickerField(etTime);

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
    }

    private void configurePickerField(EditText editText) {
        editText.setKeyListener(null);
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setCursorVisible(false);
        editText.setClickable(true);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    etDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(selectedCalendar.getTime()));
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);
                    etTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(selectedCalendar.getTime()));
                },
                selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void launchPlacePicker(int fieldType) {
        activeField = fieldType;
        PlacesAutoCompleteHelper.launchAutocomplete(this, placePickerLauncher);
    }

    private void postRide() {
        if (isPosting) {
            return;
        }

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

        if (availableSeats <= 0) {
            etSeats.setError("Seats must be at least 1");
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

        if (pricePerSeat < 0) {
            etPrice.setError("Price cannot be negative");
            etPrice.requestFocus();
            return;
        }

        FirebaseUser currentUser = FirebaseHelper.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login again to post a ride.", Toast.LENGTH_SHORT).show();
            new SessionManager(this).clearSession();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String driverUid = currentUser.getUid();
        setPostingState(true);

        FirebaseHelper.getInstance().getDb()
                .collection("users")
                .document(driverUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String driverName = "Rider";
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                            driverName = user.getName().trim();
                        }
                    }

                    publishRide(driverUid, driverName, origin, destination, date, time, availableSeats, pricePerSeat);
                })
                .addOnFailureListener(exception ->
                        publishRide(driverUid, fallbackDriverName(currentUser), origin, destination, date, time,
                                availableSeats, pricePerSeat));
    }

    private void publishRide(String driverUid,
                             String driverName,
                             String origin,
                             String destination,
                             String date,
                             String time,
                             int availableSeats,
                             double pricePerSeat) {
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
            setPostingState(false);
            if (task.isSuccessful()) {
                Toast.makeText(PostRideActivity.this, "Ride posted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                saveRideLocally(ride);
            }
        });
    }

    private void saveRideLocally(Ride ride) {
        try {
            LocalRideStore.saveRide(this, ride);
            Toast.makeText(
                    PostRideActivity.this,
                    "Ride saved on this device. Firebase rules are blocking online posting.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        } catch (Exception exception) {
            Toast.makeText(PostRideActivity.this, "Failed to post ride", Toast.LENGTH_LONG).show();
        }
    }

    private String fallbackDriverName(FirebaseUser currentUser) {
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
            return currentUser.getDisplayName().trim();
        }
        if (currentUser.getEmail() != null && !currentUser.getEmail().trim().isEmpty()) {
            return currentUser.getEmail().trim();
        }
        return "Rider";
    }

    private void setPostingState(boolean posting) {
        isPosting = posting;
        btnPostRide.setEnabled(!posting);
        btnPostRide.setText(posting ? "Publishing..." : "Publish Ride");
    }
}
