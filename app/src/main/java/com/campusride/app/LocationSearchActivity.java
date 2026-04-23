package com.campusride.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationSearchActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final long SEARCH_DELAY_MS = 220L;
    private static final int MAX_LOCATION_RESULTS = 10;
    private static final LatLng INDIA_DEFAULT = new LatLng(20.5937, 78.9629);

    private EditText etLocationSearch;
    private ImageButton btnBack;
    private LinearLayout layoutCurrentLocation;
    private TextView tvCurrentLocationSubtitle;
    private TextView tvSearchState;
    private ProgressBar progressLocationSearch;
    private RecyclerView recyclerLocationResults;
    private MapView mapView;

    private LocationSearchAdapter adapter;
    private Handler handler;
    private Runnable pendingSearchRunnable;
    private MapLibreMap mapLibreMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private PlacesClient placesClient;
    private Geocoder geocoder;
    private ExecutorService executorService;

    private String latestQuery = "";
    private LatLng currentLatLng;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_location_search);

        etLocationSearch = findViewById(R.id.etLocationSearch);
        btnBack = findViewById(R.id.btnBack);
        layoutCurrentLocation = findViewById(R.id.layoutCurrentLocation);
        tvCurrentLocationSubtitle = findViewById(R.id.tvCurrentLocationSubtitle);
        tvSearchState = findViewById(R.id.tvSearchState);
        progressLocationSearch = findViewById(R.id.progressLocationSearch);
        recyclerLocationResults = findViewById(R.id.recyclerLocationResults);
        mapView = findViewById(R.id.locationSearchMapView);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        PlacesAutoCompleteHelper.initialize(this);
        placesClient = Places.createClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        handler = new Handler(Looper.getMainLooper());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executorService = Executors.newSingleThreadExecutor();

        adapter = new LocationSearchAdapter(this::onLocationResultTapped);
        recyclerLocationResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerLocationResults.setAdapter(adapter);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarseGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                    if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                        fetchCurrentLocation(true);
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

        btnBack.setOnClickListener(v -> finish());
        layoutCurrentLocation.setOnClickListener(v -> handleCurrentLocationTap());
        tvSearchState.setText("Start typing for quick matches");
        preloadCurrentLocation();
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap map) {
        mapLibreMap = map;
        NavigationMapHelper.applyNavigationUi(mapLibreMap);
        movePreviewToDefault();
    }

    private void scheduleSearch(@NonNull String query) {
        latestQuery = query;

        if (pendingSearchRunnable != null) {
            handler.removeCallbacks(pendingSearchRunnable);
        }

        if (query.length() < 2) {
            progressLocationSearch.setVisibility(ProgressBar.GONE);
            adapter.submitList(new ArrayList<>());
            tvSearchState.setText("Start typing for quick matches");
            movePreviewToDefault();
            return;
        }

        progressLocationSearch.setVisibility(ProgressBar.VISIBLE);
        tvSearchState.setText("Searching places...");
        pendingSearchRunnable = () -> searchLocations(query);
        handler.postDelayed(pendingSearchRunnable, SEARCH_DELAY_MS);
    }

    private void searchLocations(@NonNull String query) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    if (!query.equals(latestQuery)) {
                        return;
                    }

                    List<LocationSearchAdapter.LocationResult> results = new ArrayList<>();
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    int size = Math.min(predictions.size(), MAX_LOCATION_RESULTS);

                    for (int index = 0; index < size; index++) {
                        AutocompletePrediction prediction = predictions.get(index);
                        String title = prediction.getPrimaryText(null).toString();
                        String subtitle = prediction.getSecondaryText(null) != null
                                ? prediction.getSecondaryText(null).toString()
                                : prediction.getFullText(null).toString();
                        results.add(new LocationSearchAdapter.LocationResult(
                                title,
                                subtitle,
                                null,
                                prediction.getPlaceId(),
                                prediction.getDistanceMeters()
                        ));
                    }

                    progressLocationSearch.setVisibility(ProgressBar.GONE);
                    adapter.submitList(results);
                    tvSearchState.setText(results.isEmpty()
                            ? "No matches found"
                            : results.size() + " quick matches");
                })
                .addOnFailureListener(exception -> {
                    progressLocationSearch.setVisibility(ProgressBar.GONE);
                    tvSearchState.setText("Unable to load suggestions");
                    Toast.makeText(this, "Unable to search locations right now.", Toast.LENGTH_SHORT).show();
                });
    }

    private void onLocationResultTapped(@NonNull LocationSearchAdapter.LocationResult result) {
        if (!TextUtils.isEmpty(result.getPlaceId())) {
            fetchPlaceDetails(result);
            return;
        }

        if (result.getLatLng() != null) {
            updateMapPreview(result.getLatLng(), result.getTitle(), result.getSubtitle());
            finishWithSelection(result);
        }
    }

    private void fetchPlaceDetails(@NonNull LocationSearchAdapter.LocationResult result) {
        progressLocationSearch.setVisibility(ProgressBar.VISIBLE);

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(
                result.getPlaceId(),
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        );

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    progressLocationSearch.setVisibility(ProgressBar.GONE);
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();
                    if (latLng == null) {
                        Toast.makeText(this, "Unable to open this location.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LocationSearchAdapter.LocationResult finalResult =
                            new LocationSearchAdapter.LocationResult(
                                    place.getName() != null ? place.getName() : result.getTitle(),
                                    place.getAddress() != null ? place.getAddress() : result.getSubtitle(),
                                    latLng,
                                    place.getId(),
                                    result.getDistanceMeters()
                            );
                    updateMapPreview(finalResult.getLatLng(), finalResult.getTitle(), finalResult.getSubtitle());
                    finishWithSelection(finalResult);
                })
                .addOnFailureListener(exception -> {
                    progressLocationSearch.setVisibility(ProgressBar.GONE);
                    Toast.makeText(this, "Unable to fetch location details.", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleCurrentLocationTap() {
        if (hasLocationPermission()) {
            fetchCurrentLocation(true);
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void preloadCurrentLocation() {
        if (!hasLocationPermission()) {
            movePreviewToDefault();
            return;
        }

        fetchCurrentLocation(false);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void fetchCurrentLocation(boolean finishOnSuccess) {
        tvCurrentLocationSubtitle.setText("Fetching current location...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        tvCurrentLocationSubtitle.setText("Current location unavailable right now");
                        if (finishOnSuccess) {
                            Toast.makeText(this, "Turn on location and try again.", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    resolveCurrentLocation(finishOnSuccess);
                })
                .addOnFailureListener(exception -> {
                    tvCurrentLocationSubtitle.setText("Current location unavailable right now");
                    if (finishOnSuccess) {
                        Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resolveCurrentLocation(boolean finishOnSuccess) {
        executorService.execute(() -> {
            LocationSearchAdapter.LocationResult currentLocationResult = buildCurrentLocationResult();
            runOnUiThread(() -> {
                if (currentLocationResult == null) {
                    tvCurrentLocationSubtitle.setText("Current location unavailable right now");
                    return;
                }

                tvCurrentLocationSubtitle.setText(currentLocationResult.getSubtitle());
                updateMapPreview(
                        currentLocationResult.getLatLng(),
                        currentLocationResult.getTitle(),
                        currentLocationResult.getSubtitle()
                );

                if (finishOnSuccess) {
                    finishWithSelection(currentLocationResult);
                }
            });
        });
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private LocationSearchAdapter.LocationResult buildCurrentLocationResult() {
        if (currentLatLng == null) {
            return null;
        }

        String title = "Current location";
        String subtitle = "Live device position";

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    currentLatLng.latitude,
                    currentLatLng.longitude,
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                title = firstNonEmpty(address.getFeatureName(), address.getAddressLine(0), title);
                subtitle = firstNonEmpty(address.getAddressLine(0), subtitle);
            }
        } catch (IOException ignored) {
        }

        return new LocationSearchAdapter.LocationResult(title, subtitle, currentLatLng);
    }

    private void finishWithSelection(@NonNull LocationSearchAdapter.LocationResult result) {
        if (result.getLatLng() == null) {
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(PlacesAutoCompleteHelper.EXTRA_SELECTED_PLACE_NAME, result.getTitle());
        resultIntent.putExtra(PlacesAutoCompleteHelper.EXTRA_SELECTED_PLACE_LAT, result.getLatLng().latitude);
        resultIntent.putExtra(PlacesAutoCompleteHelper.EXTRA_SELECTED_PLACE_LNG, result.getLatLng().longitude);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void updateMapPreview(@NonNull LatLng latLng, @NonNull String title, @NonNull String subtitle) {
        if (mapLibreMap == null) {
            return;
        }

        mapLibreMap.clear();
        mapLibreMap.addMarker(new MarkerOptions()
                .position(toMapLibreLatLng(latLng))
                .title(title)
                .snippet(subtitle));
        mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                NavigationMapHelper.navigationCamera()
                        .target(toMapLibreLatLng(latLng))
                        .bearing(0.0)
                        .build()
        ));
    }

    private void movePreviewToDefault() {
        if (mapLibreMap == null) {
            return;
        }

        if (currentLatLng != null) {
            updateMapPreview(currentLatLng, "Current area", "Move on the map to preview nearby places");
            return;
        }

        mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(toMapLibreLatLng(INDIA_DEFAULT))
                        .zoom(4.5)
                        .tilt(25.0)
                        .bearing(0.0)
                        .build()
        ));
    }

    @NonNull
    private org.maplibre.android.geometry.LatLng toMapLibreLatLng(@NonNull LatLng latLng) {
        return new org.maplibre.android.geometry.LatLng(latLng.latitude, latLng.longitude);
    }

    @NonNull
    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        if (pendingSearchRunnable != null) {
            handler.removeCallbacks(pendingSearchRunnable);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        mapView.onDestroy();
        super.onDestroy();
    }
}
