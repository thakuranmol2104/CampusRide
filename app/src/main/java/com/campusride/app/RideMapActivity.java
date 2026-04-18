package com.campusride.app;

import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageButton;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.charset.StandardCharsets;

public class RideMapActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String EXTRA_ORIGIN = "origin";
    public static final String EXTRA_DESTINATION = "destination";
    private static final LatLng DEFAULT_MAP_CENTER = new LatLng(20.5937, 78.9629);
    private static final long MAP_FALLBACK_DELAY_MS = 3500L;

    private GoogleMap googleMap;
    private Geocoder geocoder;
    private ExecutorService executorService;
    private WebView webRideMap;
    private TextView tvMapOrigin;
    private TextView tvMapDestination;
    private ImageButton btnBack;
    private Handler handler;
    private boolean mapLoaded;
    private boolean fallbackShown;
    private Runnable mapFallbackRunnable;

    private String originName;
    private String destinationName;
    private LatLng originLatLng;
    private LatLng destinationLatLng;
    private boolean originResolved;
    private boolean destinationResolved;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_map);

        webRideMap = findViewById(R.id.webRideMap);
        tvMapOrigin = findViewById(R.id.tvMapOrigin);
        tvMapDestination = findViewById(R.id.tvMapDestination);
        btnBack = findViewById(R.id.btnBack);
        handler = new Handler(Looper.getMainLooper());

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
        prepareWebMap();

        geocoder = new Geocoder(this, Locale.getDefault());
        executorService = Executors.newSingleThreadExecutor();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.rideMapFragment);

        if (mapFragment == null) {
            showWebFallback();
            return;
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        MapsInitializer.initialize(getApplicationContext());
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_CENTER, 4.5f));
        googleMap.setOnMapLoadedCallback(() -> {
            mapLoaded = true;
            if (mapFallbackRunnable != null) {
                handler.removeCallbacks(mapFallbackRunnable);
            }
        });
        scheduleMapFallback();
        resolveLocations();
    }

    private void scheduleMapFallback() {
        mapFallbackRunnable = () -> {
            if (!mapLoaded) {
                showWebFallback();
            }
        };
        handler.postDelayed(mapFallbackRunnable, MAP_FALLBACK_DELAY_MS);
    }

    private void resolveLocations() {
        if (geocoder == null || !Geocoder.isPresent()) {
            showGeocodeError("Geocoder is unavailable on this device.");
            return;
        }

        geocodeAddress(originName, new GeocodeCallback() {
            @Override
            public void onResult(@Nullable LatLng latLng) {
                originResolved = true;
                originLatLng = latLng;
                renderMapIfReady();
            }
        });

        geocodeAddress(destinationName, new GeocodeCallback() {
            @Override
            public void onResult(@Nullable LatLng latLng) {
                destinationResolved = true;
                destinationLatLng = latLng;
                renderMapIfReady();
            }
        });
    }

    private void geocodeAddress(@Nullable String locationName, @NonNull GeocodeCallback callback) {
        if (TextUtils.isEmpty(locationName)) {
            callback.onResult(null);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(locationName, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    callback.onResult(getLatLngFromAddressList(addresses));
                }

                @Override
                public void onError(@Nullable String errorMessage) {
                    showGeocodeError(errorMessage);
                    callback.onResult(null);
                }
            });
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onResult(getLatLngWithLegacyGeocoder(locationName));
                } catch (IOException exception) {
                    showGeocodeError(exception.getMessage());
                    callback.onResult(null);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private LatLng getLatLngWithLegacyGeocoder(@NonNull String locationName) throws IOException {
        List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
        return getLatLngFromAddressList(addresses);
    }

    @Nullable
    private LatLng getLatLngFromAddressList(@Nullable List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        Address address = addresses.get(0);
        return new LatLng(address.getLatitude(), address.getLongitude());
    }

    private void renderMapIfReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (googleMap == null || !originResolved || !destinationResolved) {
                    return;
                }

                if (originLatLng == null || destinationLatLng == null) {
                    Toast.makeText(RideMapActivity.this,
                            "Unable to locate one or both places.",
                            Toast.LENGTH_SHORT).show();
                    showWebFallback();
                    return;
                }

                googleMap.clear();
                googleMap.addMarker(new MarkerOptions()
                        .position(originLatLng)
                        .title(originName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                googleMap.addMarker(new MarkerOptions()
                        .position(destinationLatLng)
                        .title(destinationName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                googleMap.addPolyline(new PolylineOptions()
                        .add(originLatLng, destinationLatLng)
                        .width(8f)
                        .geodesic(true));

                moveCameraToBounds(originLatLng, destinationLatLng);
                if (webRideMap != null) {
                    webRideMap.setVisibility(WebView.GONE);
                }
            }
        });
    }

    private void moveCameraToBounds(@NonNull LatLng origin, @NonNull LatLng destination) {
        if (origin.latitude == destination.latitude && origin.longitude == destination.longitude) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 14f));
            return;
        }

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(origin)
                .include(destination)
                .build();

        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                96,
                getResources().getDisplayMetrics()
        );
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void showGeocodeError(@Nullable String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String errorMessage = TextUtils.isEmpty(message)
                        ? "Failed to fetch location details."
                        : message;
                Toast.makeText(RideMapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWebFallback() {
        if (fallbackShown || webRideMap == null) {
            return;
        }

        fallbackShown = true;
        if (mapFallbackRunnable != null) {
            handler.removeCallbacks(mapFallbackRunnable);
        }

        webRideMap.setVisibility(WebView.VISIBLE);
        webRideMap.loadUrl(buildDirectionsUrl());
    }

    private void prepareWebMap() {
        if (webRideMap == null) {
            return;
        }

        WebSettings settings = webRideMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webRideMap.setVisibility(WebView.VISIBLE);
        webRideMap.loadUrl(buildDirectionsUrl());
    }

    @NonNull
    private String buildDirectionsUrl() {
        String encodedOrigin = URLEncoder.encode(originName, StandardCharsets.UTF_8);
        String encodedDestination = URLEncoder.encode(destinationName, StandardCharsets.UTF_8);
        return "https://www.google.com/maps/dir/?api=1&origin="
                + encodedOrigin
                + "&destination="
                + encodedDestination
                + "&travelmode=driving";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && mapFallbackRunnable != null) {
            handler.removeCallbacks(mapFallbackRunnable);
        }
        if (webRideMap != null) {
            webRideMap.destroy();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private interface GeocodeCallback {
        void onResult(@Nullable LatLng latLng);
    }
}
