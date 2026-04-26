package com.campusride.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RideMapActivity extends FragmentActivity {

    public static final String EXTRA_ORIGIN = "origin";
    public static final String EXTRA_DESTINATION = "destination";

    private WebView rideMapWebView;
    private PlacesClient placesClient;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    private TextView tvMapOrigin;
    private TextView tvMapDestination;
    private ImageButton btnBack;

    private String originName;
    private String destinationName;
    private boolean placesAvailable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_map);

        rideMapWebView = findViewById(R.id.rideMapWebView);
        tvMapOrigin = findViewById(R.id.tvMapOrigin);
        tvMapDestination = findViewById(R.id.tvMapDestination);
        btnBack = findViewById(R.id.btnBack);

        originName = getIntent() != null ? getIntent().getStringExtra(EXTRA_ORIGIN) : null;
        destinationName = getIntent() != null ? getIntent().getStringExtra(EXTRA_DESTINATION) : null;

        if (TextUtils.isEmpty(originName) || TextUtils.isEmpty(destinationName)) {
            Toast.makeText(this, "Origin or destination is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvMapOrigin.setText(originName);
        tvMapDestination.setText(destinationName);
        btnBack.setOnClickListener(v -> finish());
        configureMapWebView();

        try {
            PlacesAutoCompleteHelper.initialize(this);
            placesClient = Places.createClient(this);
            placesAvailable = true;
        } catch (Exception exception) {
            placesAvailable = false;
        }

        loadRoute();
    }

    private void configureMapWebView() {
        WebSettings settings = rideMapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        rideMapWebView.setBackgroundColor(0xFF101614);
    }

    private void loadRoute() {
        resolvePlace(originName, originLatLng ->
                resolvePlace(destinationName, destinationLatLng ->
                        renderResolvedRoute(originLatLng, destinationLatLng)));
    }

    private void resolvePlace(@NonNull String query, @NonNull PlaceLatLngCallback callback) {
        if (!placesAvailable || placesClient == null) {
            resolvePlaceWithOpenStreetMap(query, callback);
            return;
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    if (predictions.isEmpty()) {
                        resolvePlaceWithOpenStreetMap(query, callback);
                        return;
                    }

                    String placeId = predictions.get(0).getPlaceId();
                    FetchPlaceRequest fetchRequest = FetchPlaceRequest.newInstance(
                            placeId,
                            Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME)
                    );

                    placesClient.fetchPlace(fetchRequest)
                            .addOnSuccessListener(fetchResponse -> callback.onResult(fetchResponse.getPlace().getLatLng()))
                            .addOnFailureListener(exception -> resolvePlaceWithOpenStreetMap(query, callback));
                })
                .addOnFailureListener(exception -> resolvePlaceWithOpenStreetMap(query, callback));
    }

    private void resolvePlaceWithOpenStreetMap(@NonNull String query, @NonNull PlaceLatLngCallback callback) {
        geocodeExecutor.execute(() -> {
            LatLng latLng = null;
            try {
                LocationSearchAdapter.LocationResult result = OpenStreetMapHelper.geocodeSingle(query);
                if (result != null) {
                    latLng = result.getLatLng();
                }
            } catch (Exception ignored) {
                latLng = null;
            }

            LatLng finalLatLng = latLng;
            runOnUiThread(() -> callback.onResult(finalLatLng));
        });
    }

    private void renderResolvedRoute(@Nullable LatLng originLatLng, @Nullable LatLng destinationLatLng) {
        if (originLatLng == null || destinationLatLng == null) {
            Toast.makeText(this, "Unable to load route locations.", Toast.LENGTH_SHORT).show();
            return;
        }

        rideMapWebView.loadDataWithBaseURL(
                "https://localhost/",
                WebMapHtmlBuilder.buildRouteMap(originLatLng, originName, destinationLatLng, destinationName),
                "text/html",
                "UTF-8",
                null
        );
    }

    private interface PlaceLatLngCallback {
        void onResult(@Nullable LatLng latLng);
    }

    @Override
    protected void onDestroy() {
        geocodeExecutor.shutdownNow();
        super.onDestroy();
    }
}
