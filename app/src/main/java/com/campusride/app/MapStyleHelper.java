package com.campusride.app;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MapStyleOptions;

public final class MapStyleHelper {

    private static final String TAG = "MapStyleHelper";

    private MapStyleHelper() {
    }

    public static void applyRideMapStyle(@NonNull Context context, @NonNull GoogleMap googleMap) {
        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int styleRes = uiMode == Configuration.UI_MODE_NIGHT_YES
                ? R.raw.campus_ride_map_style_night
                : R.raw.campus_ride_map_style;

        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, styleRes));
        } catch (Exception exception) {
            Log.w(TAG, "Unable to apply map style", exception);
        }
    }
}
