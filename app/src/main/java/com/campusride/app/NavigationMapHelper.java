package com.campusride.app;

import androidx.annotation.NonNull;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;

public final class NavigationMapHelper {

    public static final String NAVIGATION_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

    private NavigationMapHelper() {
    }

    public static void applyNavigationUi(@NonNull MapLibreMap mapLibreMap) {
        mapLibreMap.setStyle(new Style.Builder().fromUri(NAVIGATION_STYLE_URL));
        mapLibreMap.getUiSettings().setCompassEnabled(false);
        mapLibreMap.getUiSettings().setLogoEnabled(false);
        mapLibreMap.getUiSettings().setAttributionEnabled(true);
        mapLibreMap.getUiSettings().setTiltGesturesEnabled(false);
        mapLibreMap.getUiSettings().setRotateGesturesEnabled(false);
        mapLibreMap.getUiSettings().setDoubleTapGesturesEnabled(true);
        mapLibreMap.getUiSettings().setZoomGesturesEnabled(true);
        mapLibreMap.setMinZoomPreference(3.0);
        mapLibreMap.setMaxZoomPreference(18.0);
    }

    @NonNull
    public static CameraPosition.Builder navigationCamera() {
        return new CameraPosition.Builder()
                .zoom(15.5)
                .tilt(45.0);
    }

    public static double calculateBearing(double startLat,
                                          double startLng,
                                          double endLat,
                                          double endLng) {
        double startLatRad = Math.toRadians(startLat);
        double startLngRad = Math.toRadians(startLng);
        double endLatRad = Math.toRadians(endLat);
        double endLngRad = Math.toRadians(endLng);
        double deltaLng = endLngRad - startLngRad;

        double y = Math.sin(deltaLng) * Math.cos(endLatRad);
        double x = Math.cos(startLatRad) * Math.sin(endLatRad)
                - Math.sin(startLatRad) * Math.cos(endLatRad) * Math.cos(deltaLng);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }
}
