package com.campusride.app;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RideMapActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String EXTRA_ORIGIN = "origin";
    public static final String EXTRA_DESTINATION = "destination";
    private static final LatLng INDIA_DEFAULT = new LatLng(20.5937, 78.9629);

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private Geocoder geocoder;
    private ExecutorService executorService;

    private TextView tvMapOrigin;
    private TextView tvMapDestination;
    private ImageButton btnBack;

    private String originName;
    private String destinationName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_ride_map);

        mapView = findViewById(R.id.rideMapView);
        tvMapOrigin = findViewById(R.id.tvMapOrigin);
        tvMapDestination = findViewById(R.id.tvMapDestination);
        btnBack = findViewById(R.id.btnBack);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

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

        geocoder = new Geocoder(this, Locale.getDefault());
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap map) {
        mapLibreMap = map;
        NavigationMapHelper.applyNavigationUi(mapLibreMap);
        mapLibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(INDIA_DEFAULT, 4.5));
        loadRoute();
    }

    private void loadRoute() {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Map search is unavailable on this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            LatLng originLatLng = geocode(originName);
            LatLng destinationLatLng = geocode(destinationName);
            runOnUiThread(() -> renderRoute(originLatLng, destinationLatLng));
        });
    }

    private void renderRoute(@Nullable LatLng originLatLng, @Nullable LatLng destinationLatLng) {
        if (mapLibreMap == null) {
            return;
        }

        if (originLatLng == null || destinationLatLng == null) {
            Toast.makeText(this, "Unable to load route locations.", Toast.LENGTH_SHORT).show();
            return;
        }

        mapLibreMap.clear();
        mapLibreMap.addMarker(new MarkerOptions()
                .position(originLatLng)
                .title(originName)
                .snippet("Pickup"));
        mapLibreMap.addMarker(new MarkerOptions()
                .position(destinationLatLng)
                .title(destinationName)
                .snippet("Drop"));
        mapLibreMap.addPolyline(new PolylineOptions()
                .add(originLatLng)
                .add(destinationLatLng)
                .color(0xFF0F8B6D)
                .width(7f)
                .alpha(0.9f));

        double bearing = NavigationMapHelper.calculateBearing(
                originLatLng.getLatitude(),
                originLatLng.getLongitude(),
                destinationLatLng.getLatitude(),
                destinationLatLng.getLongitude()
        );

        if (samePoint(originLatLng, destinationLatLng)) {
            mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    NavigationMapHelper.navigationCamera()
                            .target(originLatLng)
                            .bearing(bearing)
                            .build()
            ));
            return;
        }

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(originLatLng)
                .include(destinationLatLng)
                .build();

        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96));
        mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder(mapLibreMap.getCameraPosition())
                        .bearing(bearing)
                        .tilt(40.0)
                        .build()
        ));
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private LatLng geocode(@NonNull String query) {
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses == null || addresses.isEmpty()) {
                return null;
            }

            Address address = addresses.get(0);
            return new LatLng(address.getLatitude(), address.getLongitude());
        } catch (IOException exception) {
            return null;
        }
    }

    private boolean samePoint(@NonNull LatLng first, @NonNull LatLng second) {
        return Math.abs(first.getLatitude() - second.getLatitude()) < 0.00001
                && Math.abs(first.getLongitude() - second.getLongitude()) < 0.00001;
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
        if (executorService != null) {
            executorService.shutdownNow();
        }
        mapView.onDestroy();
        super.onDestroy();
    }
}
