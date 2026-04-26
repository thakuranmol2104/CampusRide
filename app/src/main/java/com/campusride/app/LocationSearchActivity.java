package com.campusride.app;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationSearchActivity extends AppCompatActivity {

    private static final long SEARCH_DELAY_MS = 220L;
    private static final int MAX_LOCATION_RESULTS = 5;
    private static final LatLng INDIA_DEFAULT = new LatLng(20.5937, 78.9629);

    private EditText etLocationSearch;
    private ImageButton btnBack;
    private LinearLayout layoutCurrentLocation;
    private TextView tvCurrentLocationSubtitle;
    private TextView tvSearchState;
    private ProgressBar progressLocationSearch;
    private RecyclerView recyclerLocationResults;
    private FrameLayout layoutMapContainer;
    private WebView locationSearchMapWebView;

    private LocationSearchAdapter adapter;
    private Handler handler;
    private Runnable pendingSearchRunnable;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();

    private String latestQuery = "";
    private String currentCountryCode = defaultCountryCode();
    private LatLng currentLatLng;
    private int mapExpandedHeightPx;
    private int mapCollapsedHeightPx;
    private boolean mapCollapsed;
    private boolean placesAvailable;
    private int searchGeneration;

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
        layoutMapContainer = findViewById(R.id.layoutMapContainer);
        locationSearchMapWebView = findViewById(R.id.locationSearchMapWebView);

        mapExpandedHeightPx = dpToPx(220);
        mapCollapsedHeightPx = dpToPx(88);
        configureMapWebView();
        movePreviewToDefault();

        try {
            PlacesAutoCompleteHelper.initialize(this);
            placesClient = Places.createClient(this);
            placesAvailable = true;
        } catch (Exception exception) {
            placesAvailable = false;
        }
        handler = new Handler(Looper.getMainLooper());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();

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

    private void configureMapWebView() {
        WebSettings settings = locationSearchMapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        locationSearchMapWebView.setBackgroundColor(0xFF101614);
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
            sessionToken = AutocompleteSessionToken.newInstance();
            searchGeneration++;
            setMapCollapsed(false);
            movePreviewToDefault();
            return;
        }

        setMapCollapsed(true);
        progressLocationSearch.setVisibility(ProgressBar.VISIBLE);
        tvSearchState.setText("Searching places...");
        pendingSearchRunnable = () -> searchLocations(query);
        handler.postDelayed(pendingSearchRunnable, SEARCH_DELAY_MS);
    }

    private void searchLocations(@NonNull String query) {
        int generation = ++searchGeneration;
        if (!placesAvailable || placesClient == null) {
            searchLocationsWithOpenStreetMap(query, generation);
            return;
        }

        FindAutocompletePredictionsRequest.Builder requestBuilder =
                FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .setSessionToken(sessionToken);

        if (currentLatLng != null) {
            requestBuilder.setOrigin(currentLatLng);
            requestBuilder.setLocationBias(createLocationBias(currentLatLng));
        }

        placesClient.findAutocompletePredictions(requestBuilder.build())
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
                    if (!query.equals(latestQuery) || generation != searchGeneration) {
                        return;
                    }
                    searchLocationsWithOpenStreetMap(query, generation);
                });
    }

    private void searchLocationsWithOpenStreetMap(@NonNull String query, int generation) {
        searchExecutor.execute(() -> {
            try {
                List<LocationSearchAdapter.LocationResult> results =
                        OpenStreetMapHelper.searchLocations(
                                query,
                                MAX_LOCATION_RESULTS,
                                currentCountryCode,
                                null,
                                currentLatLng
                        );
                runOnUiThread(() -> showOpenStreetMapResults(query, generation, results));
            } catch (Exception exception) {
                runOnUiThread(() -> showSearchFailure(query, generation));
            }
        });
    }

    private void showOpenStreetMapResults(@NonNull String query,
                                          int generation,
                                          @NonNull List<LocationSearchAdapter.LocationResult> results) {
        if (!query.equals(latestQuery) || generation != searchGeneration) {
            return;
        }

        progressLocationSearch.setVisibility(ProgressBar.GONE);
        adapter.submitList(results);
        tvSearchState.setText(results.isEmpty()
                ? "No matches found"
                : results.size() + " quick matches");
    }

    private void showSearchFailure(@NonNull String query, int generation) {
        if (!query.equals(latestQuery) || generation != searchGeneration) {
            return;
        }

        progressLocationSearch.setVisibility(ProgressBar.GONE);
        adapter.submitList(new ArrayList<>());
        tvSearchState.setText("Unable to load suggestions");
        Toast.makeText(this, "Unable to search locations right now.", Toast.LENGTH_SHORT).show();
    }

    private void onLocationResultTapped(@NonNull LocationSearchAdapter.LocationResult result) {
        if (result.getLatLng() != null) {
            setMapCollapsed(false);
            updateMapPreview(result.getLatLng(), result.getTitle(), result.getSubtitle());
            returnSelectionWithPreview(result);
            return;
        }

        if (TextUtils.isEmpty(result.getPlaceId())) {
            return;
        }

        fetchPlaceDetails(result);
    }

    private void fetchPlaceDetails(@NonNull LocationSearchAdapter.LocationResult result) {
        progressLocationSearch.setVisibility(ProgressBar.VISIBLE);

        FetchPlaceRequest request = FetchPlaceRequest.builder(
                result.getPlaceId(),
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        ).setSessionToken(sessionToken).build();

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

                    setMapCollapsed(false);
                    updateMapPreview(finalResult.getLatLng(), finalResult.getTitle(), finalResult.getSubtitle());
                    returnSelectionWithPreview(finalResult);
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
                    LocationSearchAdapter.LocationResult currentLocationResult =
                            new LocationSearchAdapter.LocationResult(
                                    "Current location",
                                    formatCoordinates(currentLatLng),
                                    currentLatLng
                            );
                    tvCurrentLocationSubtitle.setText(currentLocationResult.getSubtitle());
                    updateMapPreview(
                            currentLocationResult.getLatLng(),
                            currentLocationResult.getTitle(),
                            currentLocationResult.getSubtitle()
                    );
                    if (finishOnSuccess) {
                        returnSelectionWithPreview(currentLocationResult);
                    }
                })
                .addOnFailureListener(exception -> {
                    tvCurrentLocationSubtitle.setText("Current location unavailable right now");
                    if (finishOnSuccess) {
                        Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show();
                    }
                });
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

    private void returnSelectionWithPreview(@NonNull LocationSearchAdapter.LocationResult result) {
        handler.postDelayed(() -> finishWithSelection(result), 180L);
    }

    private void updateMapPreview(@NonNull LatLng latLng, @NonNull String title, @NonNull String subtitle) {
        locationSearchMapWebView.loadDataWithBaseURL(
                "https://localhost/",
                WebMapHtmlBuilder.buildSingleLocationMap(latLng, title, subtitle),
                "text/html",
                "UTF-8",
                null
        );
    }

    private void movePreviewToDefault() {
        locationSearchMapWebView.loadDataWithBaseURL(
                "https://localhost/",
                WebMapHtmlBuilder.buildSingleLocationMap(
                        currentLatLng,
                        "CampusRide",
                        currentLatLng != null ? "Current area" : "Search for a pickup or drop"
                ),
                "text/html",
                "UTF-8",
                null
        );
    }

    @NonNull
    private RectangularBounds createLocationBias(@NonNull LatLng latLng) {
        double latOffset = 0.18d;
        double lngOffset = 0.18d;
        LatLng southwest = new LatLng(latLng.latitude - latOffset, latLng.longitude - lngOffset);
        LatLng northeast = new LatLng(latLng.latitude + latOffset, latLng.longitude + lngOffset);
        return RectangularBounds.newInstance(southwest, northeast);
    }

    private void setMapCollapsed(boolean collapsed) {
        if (mapCollapsed == collapsed) {
            return;
        }

        mapCollapsed = collapsed;
        int startHeight = layoutMapContainer.getLayoutParams().height;
        int endHeight = collapsed ? mapCollapsedHeightPx : mapExpandedHeightPx;

        ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);
        animator.setDuration(180L);
        animator.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams params = layoutMapContainer.getLayoutParams();
            params.height = (int) valueAnimator.getAnimatedValue();
            layoutMapContainer.setLayoutParams(params);
        });
        animator.start();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @NonNull
    private static String defaultCountryCode() {
        String localeCountry = Locale.getDefault().getCountry();
        if (localeCountry == null || localeCountry.trim().isEmpty()) {
            return "in";
        }
        return localeCountry.toLowerCase(Locale.US);
    }

    @NonNull
    private String formatCoordinates(@NonNull LatLng latLng) {
        return String.format(
                Locale.getDefault(),
                "%.5f, %.5f",
                latLng.latitude,
                latLng.longitude
        );
    }

    @Override
    protected void onDestroy() {
        if (pendingSearchRunnable != null) {
            handler.removeCallbacks(pendingSearchRunnable);
        }
        searchExecutor.shutdownNow();
        super.onDestroy();
    }
}
