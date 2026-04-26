package com.campusride.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.gms.common.api.Status;

import java.util.Arrays;
import java.util.List;

public final class PlacesAutoCompleteHelper {

    public static final String EXTRA_SELECTED_PLACE_NAME = "selected_place_name";
    public static final String EXTRA_SELECTED_PLACE_LAT = "selected_place_lat";
    public static final String EXTRA_SELECTED_PLACE_LNG = "selected_place_lng";

    private static final List<Place.Field> PLACE_FIELDS =
            Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);

    private PlacesAutoCompleteHelper() {
        // Utility class
    }

    public static void initialize(@NonNull Context context) {
        if (!Places.isInitialized()) {
            Places.initialize(context.getApplicationContext(), getApiKey(context));
        }
    }

    public static Intent buildAutocompleteIntent(@NonNull Context context) {
        initialize(context);
        return new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, PLACE_FIELDS)
                .build(context);
    }

    public static void launchAutocomplete(@NonNull Context context,
                                          @NonNull ActivityResultLauncher<Intent> launcher) {
        launchFallbackAutocomplete(context, launcher);
    }

    public static void launchFallbackAutocomplete(@NonNull Context context,
                                                  @NonNull ActivityResultLauncher<Intent> launcher) {
        launcher.launch(new Intent(context, LocationSearchActivity.class));
    }

    @Nullable
    public static String getSelectedPlaceName(@Nullable Intent data) {
        String fallbackPlaceName = getFallbackPlaceName(data);
        if (!TextUtils.isEmpty(fallbackPlaceName)) {
            return fallbackPlaceName;
        }

        Place place = getSelectedPlace(data);
        return place != null ? place.getName() : null;
    }

    @Nullable
    public static LatLng getSelectedPlaceLatLng(@Nullable Intent data) {
        LatLng fallbackLatLng = getFallbackLatLng(data);
        if (fallbackLatLng != null) {
            return fallbackLatLng;
        }

        Place place = getSelectedPlace(data);
        return place != null ? place.getLatLng() : null;
    }

    public static boolean isPlacesErrorResult(int resultCode) {
        return resultCode == AutocompleteActivity.RESULT_ERROR;
    }

    @Nullable
    public static String getPlacesErrorMessage(@Nullable Intent data) {
        if (data == null) {
            return null;
        }

        Status status = Autocomplete.getStatusFromIntent(data);
        return status != null ? status.getStatusMessage() : null;
    }

    @Nullable
    private static Place getSelectedPlace(@Nullable Intent data) {
        if (data == null) {
            return null;
        }
        return Autocomplete.getPlaceFromIntent(data);
    }

    @Nullable
    private static String getFallbackPlaceName(@Nullable Intent data) {
        if (data == null) {
            return null;
        }
        return data.getStringExtra(EXTRA_SELECTED_PLACE_NAME);
    }

    @Nullable
    private static LatLng getFallbackLatLng(@Nullable Intent data) {
        if (data == null
                || !data.hasExtra(EXTRA_SELECTED_PLACE_LAT)
                || !data.hasExtra(EXTRA_SELECTED_PLACE_LNG)) {
            return null;
        }

        double latitude = data.getDoubleExtra(EXTRA_SELECTED_PLACE_LAT, 0d);
        double longitude = data.getDoubleExtra(EXTRA_SELECTED_PLACE_LNG, 0d);
        return new LatLng(latitude, longitude);
    }

    @NonNull
    public static String requireApiKey(@NonNull Context context) {
        String generatedApiKey = context.getString(R.string.google_api_key);
        if (isValidApiKey(generatedApiKey)) {
            return generatedApiKey;
        }

        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA
            );
            Bundle metaData = applicationInfo.metaData;
            String apiKey = metaData != null
                    ? metaData.getString("com.google.android.geo.API_KEY")
                    : null;

            if (!isValidApiKey(apiKey)) {
                throw new IllegalStateException("Google Maps API key is missing.");
            }
            return apiKey;
        } catch (PackageManager.NameNotFoundException exception) {
            throw new IllegalStateException("Unable to read Google Maps API key.", exception);
        }
    }

    @NonNull
    private static String getApiKey(@NonNull Context context) {
        return requireApiKey(context);
    }

    private static boolean isValidApiKey(@Nullable String apiKey) {
        return apiKey != null
                && !apiKey.trim().isEmpty()
                && !"YOUR_API_KEY_HERE".equals(apiKey);
    }
}
