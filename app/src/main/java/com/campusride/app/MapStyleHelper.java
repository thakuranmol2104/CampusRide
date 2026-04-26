package com.campusride.app;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;

public final class MapStyleHelper {

    private MapStyleHelper() {
    }

    public static void applyRideMapStyle(@NonNull GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }
}
