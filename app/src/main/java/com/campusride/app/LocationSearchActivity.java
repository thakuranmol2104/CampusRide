package com.campusride.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationSearchActivity extends AppCompatActivity {

    private static final long SEARCH_DELAY_MS = 180L;
    private static final int MAX_LOCATION_RESULTS = 10;

    private EditText etLocationSearch;
    private ImageButton btnBack;
    private LinearLayout layoutCurrentLocation;
    private TextView tvCurrentLocationSubtitle;
    private TextView tvSearchState;
    private ProgressBar progressLocationSearch;
    private RecyclerView recyclerLocationResults;

    private Geocoder geocoder;
    private ExecutorService executorService;
    private Handler handler;
    private Runnable pendingSearchRunnable;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private LocationSearchAdapter adapter;
    private String latestQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search);

        etLocationSearch = findViewById(R.id.etLocationSearch);
        btnBack = findViewById(R.id.btnBack);
        layoutCurrentLocation = findViewById(R.id.layoutCurrentLocation);
        tvCurrentLocationSubtitle = findViewById(R.id.tvCurrentLocationSubtitle);
        tvSearchState = findViewById(R.id.tvSearchState);
        progressLocationSearch = findViewById(R.id.progressLocationSearch);
        recyclerLocationResults = findViewById(R.id.recyclerLocationResults);

        geocoder = new Geocoder(this, Locale.getDefault());
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        adapter = new LocationSearchAdapter(this::finishWithSelection);
        recyclerLocationResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerLocationResults.setAdapter(adapter);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarseGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                    if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                        fetchCurrentLocation();
                    } else {
                        Toast.makeText(this, "Location permission is needed for current location.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        etLocationSearch.requestFocus();
        etLocationSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSearch(s != null ? s.toString().trim() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        layoutCurrentLocation.setOnClickListener(v -> handleCurrentLocationTap());
        btnBack.setOnClickListener(v -> finish());
    }

    private void scheduleSearch(@NonNull String query) {
        latestQuery = query;

        if (pendingSearchRunnable != null) {
            handler.removeCallbacks(pendingSearchRunnable);
        }

        if (query.length() < 2) {
            progressLocationSearch.setVisibility(ProgressBar.GONE);
            adapter.submitList(new ArrayList<>());
            tvSearchState.setText("Start typing for quick suggestions");
            return;
        }

        progressLocationSearch.setVisibility(ProgressBar.VISIBLE);
        tvSearchState.setText("Searching nearby matches...");
        pendingSearchRunnable = () -> searchLocations(query);
        handler.postDelayed(pendingSearchRunnable, SEARCH_DELAY_MS);
    }

    private void searchLocations(@NonNull String query) {
        if (!Geocoder.isPresent()) {
            progressLocationSearch.setVisibility(ProgressBar.GONE);
            tvSearchState.setText("Location search is unavailable on this device");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, MAX_LOCATION_RESULTS, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    updateSuggestions(query, addresses);
                }

                @Override
                public void onError(@Nullable String errorMessage) {
                    showSearchError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                List<Address> addresses = getLegacyResults(query);
                updateSuggestions(query, addresses);
            } catch (IOException exception) {
                showSearchError(exception.getMessage());
            }
        });
    }

    @SuppressWarnings("deprecation")
    private List<Address> getLegacyResults(@NonNull String query) throws IOException {
        return geocoder.getFromLocationName(query, MAX_LOCATION_RESULTS);
    }

    private void updateSuggestions(@NonNull String query, @Nullable List<Address> addresses) {
        runOnUiThread(() -> {
            if (!query.equals(latestQuery)) {
                return;
            }

            progressLocationSearch.setVisibility(ProgressBar.GONE);

            List<LocationSearchAdapter.LocationResult> results = new ArrayList<>();
            if (addresses != null) {
                for (Address address : addresses) {
                    String title = buildTitle(address);
                    String subtitle = buildSubtitle(address);
                    if (TextUtils.isEmpty(title) || containsResult(results, title, subtitle)) {
                        continue;
                    }
                    results.add(new LocationSearchAdapter.LocationResult(
                            title,
                            subtitle,
                            new LatLng(address.getLatitude(), address.getLongitude())
                    ));
                    if (results.size() >= MAX_LOCATION_RESULTS) {
                        break;
                    }
                }
            }

            adapter.submitList(results);
            if (results.isEmpty()) {
                tvSearchState.setText("No quick matches found");
            } else {
                tvSearchState.setText(results.size() + " quick matches");
            }
        });
    }

    private boolean containsResult(@NonNull List<LocationSearchAdapter.LocationResult> results,
                                   @NonNull String title,
                                   @NonNull String subtitle) {
        for (LocationSearchAdapter.LocationResult result : results) {
            if (title.equalsIgnoreCase(result.getTitle())
                    && subtitle.equalsIgnoreCase(result.getSubtitle())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private String buildTitle(@NonNull Address address) {
        if (!TextUtils.isEmpty(address.getFeatureName())) {
            return address.getFeatureName();
        }
        if (!TextUtils.isEmpty(address.getSubLocality())) {
            return address.getSubLocality();
        }
        if (!TextUtils.isEmpty(address.getLocality())) {
            return address.getLocality();
        }
        if (address.getMaxAddressLineIndex() >= 0) {
            return address.getAddressLine(0);
        }
        return null;
    }

    @NonNull
    private String buildSubtitle(@NonNull Address address) {
        List<String> parts = new ArrayList<>();

        String locality = address.getLocality();
        String adminArea = address.getAdminArea();
        String country = address.getCountryName();

        if (!TextUtils.isEmpty(locality)) {
            parts.add(locality);
        }
        if (!TextUtils.isEmpty(adminArea) && !parts.contains(adminArea)) {
            parts.add(adminArea);
        }
        if (!TextUtils.isEmpty(country) && !parts.contains(country)) {
            parts.add(country);
        }

        if (parts.isEmpty() && address.getMaxAddressLineIndex() >= 0) {
            return address.getAddressLine(0);
        }

        return TextUtils.join(", ", parts);
    }

    private void handleCurrentLocationTap() {
        if (hasLocationPermission()) {
            fetchCurrentLocation();
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void fetchCurrentLocation() {
        tvCurrentLocationSubtitle.setText("Fetching current location...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        tvCurrentLocationSubtitle.setText("Current location unavailable right now");
                        Toast.makeText(this, "Turn on location and try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    reverseGeocodeCurrentLocation(latLng);
                })
                .addOnFailureListener(exception -> {
                    tvCurrentLocationSubtitle.setText("Current location unavailable right now");
                    Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show();
                });
    }

    private void reverseGeocodeCurrentLocation(@NonNull LatLng latLng) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    finishWithCurrentLocation(latLng, addresses);
                }

                @Override
                public void onError(@Nullable String errorMessage) {
                    tvCurrentLocationSubtitle.setText("Current location ready");
                    finishWithSelection(new LocationSearchAdapter.LocationResult(
                            "Current location",
                            "Live device position",
                            latLng
                    ));
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                List<Address> addresses = getLegacyCurrentLocation(latLng);
                finishWithCurrentLocation(latLng, addresses);
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    tvCurrentLocationSubtitle.setText("Current location ready");
                    finishWithSelection(new LocationSearchAdapter.LocationResult(
                            "Current location",
                            "Live device position",
                            latLng
                    ));
                });
            }
        });
    }

    @SuppressWarnings("deprecation")
    private List<Address> getLegacyCurrentLocation(@NonNull LatLng latLng) throws IOException {
        return geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
    }

    private void finishWithCurrentLocation(@NonNull LatLng latLng, @Nullable List<Address> addresses) {
        runOnUiThread(() -> {
            String title = "Current location";
            String subtitle = "Live device position";

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String builtTitle = buildTitle(address);
                String builtSubtitle = buildSubtitle(address);
                if (!TextUtils.isEmpty(builtTitle)) {
                    title = builtTitle;
                }
                if (!TextUtils.isEmpty(builtSubtitle)) {
                    subtitle = builtSubtitle;
                }
            }

            tvCurrentLocationSubtitle.setText(subtitle);
            finishWithSelection(new LocationSearchAdapter.LocationResult(title, subtitle, latLng));
        });
    }

    private void finishWithSelection(@NonNull LocationSearchAdapter.LocationResult result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(PlacesAutoCompleteHelper.EXTRA_SELECTED_PLACE_NAME, result.getTitle());
        resultIntent.putExtra(PlacesAutoCompleteHelper.EXTRA_SELECTED_PLACE_LAT, result.getLatLng().latitude);
        resultIntent.putExtra(PlacesAutoCompleteHelper.EXTRA_SELECTED_PLACE_LNG, result.getLatLng().longitude);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void showSearchError(@Nullable String message) {
        runOnUiThread(() -> {
            progressLocationSearch.setVisibility(ProgressBar.GONE);
            tvSearchState.setText("Unable to load suggestions");
            Toast.makeText(
                    this,
                    message != null ? message : "Unable to search locations.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingSearchRunnable != null) {
            handler.removeCallbacks(pendingSearchRunnable);
        }
        executorService.shutdownNow();
    }
}
