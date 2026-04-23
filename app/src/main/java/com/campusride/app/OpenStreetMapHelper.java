package com.campusride.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OpenStreetMapHelper {

    private static final String USER_AGENT = "CampusRide/1.0";

    private OpenStreetMapHelper() {
    }

    @NonNull
    public static List<LocationSearchAdapter.LocationResult> searchLocations(@NonNull String query,
                                                                             int limit,
                                                                             @Nullable String countryCode,
                                                                             @Nullable String currentCity,
                                                                             @Nullable LatLng currentLatLng) throws Exception {
        Map<String, LocationSearchAdapter.LocationResult> merged = new LinkedHashMap<>();

        if (currentCity != null && !currentCity.trim().isEmpty()) {
            addResults(merged, executeSearch(query + " " + currentCity, limit, countryCode));
        }

        addResults(merged, executeSearch(query, limit, countryCode));

        if (merged.isEmpty()) {
            addResults(merged, executeSearch(query, limit, null));
        }

        List<LocationSearchAdapter.LocationResult> ranked = new ArrayList<>(merged.values());
        ranked.sort((first, second) -> compareResults(first, second, currentCity, currentLatLng));

        if (ranked.size() > limit) {
            return new ArrayList<>(ranked.subList(0, limit));
        }
        return ranked;
    }

    @Nullable
    public static LocationSearchAdapter.LocationResult geocodeSingle(@NonNull String query) throws Exception {
        List<LocationSearchAdapter.LocationResult> results = executeSearch(query, 1, null);
        return results.isEmpty() ? null : results.get(0);
    }

    @Nullable
    public static LocationSearchAdapter.LocationResult reverseGeocode(@NonNull LatLng latLng) throws Exception {
        String endpoint = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat="
                + latLng.latitude + "&lon=" + latLng.longitude + "&addressdetails=1";

        JSONObject response = new JSONObject(readUrl(endpoint));
        String displayName = response.optString("display_name");
        if (displayName.isEmpty()) {
            return null;
        }

        String title = firstNonEmpty(
                response.optString("name"),
                displayName
        );

        return new LocationSearchAdapter.LocationResult(title, displayName, latLng);
    }

    @Nullable
    public static String reverseGeocodeCity(@NonNull LatLng latLng) throws Exception {
        JSONObject address = getReverseAddress(latLng);
        return firstNonEmpty(
                address.optString("city"),
                address.optString("town"),
                address.optString("village"),
                address.optString("county")
        );
    }

    @Nullable
    public static String reverseGeocodeCountryCode(@NonNull LatLng latLng) throws Exception {
        JSONObject address = getReverseAddress(latLng);
        String countryCode = address.optString("country_code");
        return countryCode != null ? countryCode.toLowerCase(Locale.US) : null;
    }

    @NonNull
    private static JSONObject getReverseAddress(@NonNull LatLng latLng) throws Exception {
        String endpoint = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat="
                + latLng.latitude + "&lon=" + latLng.longitude + "&addressdetails=1";
        JSONObject response = new JSONObject(readUrl(endpoint));
        return response.optJSONObject("address") != null ? response.getJSONObject("address") : new JSONObject();
    }

    @NonNull
    private static List<LocationSearchAdapter.LocationResult> executeSearch(@NonNull String query,
                                                                            int limit,
                                                                            @Nullable String countryCode) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        StringBuilder endpoint = new StringBuilder(
                "https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit="
                        + limit + "&q=" + encodedQuery
        );

        if (countryCode != null && !countryCode.trim().isEmpty()) {
            endpoint.append("&countrycodes=").append(countryCode.toLowerCase(Locale.US));
        }

        JSONArray response = new JSONArray(readUrl(endpoint.toString()));
        List<LocationSearchAdapter.LocationResult> results = new ArrayList<>();

        for (int index = 0; index < response.length(); index++) {
            JSONObject item = response.getJSONObject(index);
            String displayName = item.optString("display_name");
            String title = firstNonEmpty(
                    item.optString("name"),
                    item.optString("display_name")
            );

            double latitude = item.optDouble("lat", 0d);
            double longitude = item.optDouble("lon", 0d);
            results.add(new LocationSearchAdapter.LocationResult(
                    title,
                    displayName,
                    new LatLng(latitude, longitude)
            ));
        }

        return results;
    }

    private static void addResults(@NonNull Map<String, LocationSearchAdapter.LocationResult> merged,
                                   @NonNull List<LocationSearchAdapter.LocationResult> incoming) {
        for (LocationSearchAdapter.LocationResult result : incoming) {
            String key = (result.getTitle() + "|" + result.getSubtitle()).toLowerCase(Locale.US);
            if (!merged.containsKey(key)) {
                merged.put(key, result);
            }
        }
    }

    private static int compareResults(@NonNull LocationSearchAdapter.LocationResult first,
                                      @NonNull LocationSearchAdapter.LocationResult second,
                                      @Nullable String currentCity,
                                      @Nullable LatLng currentLatLng) {
        int firstScore = localityScore(first, currentCity);
        int secondScore = localityScore(second, currentCity);

        if (firstScore != secondScore) {
            return Integer.compare(secondScore, firstScore);
        }

        if (currentLatLng != null) {
            double firstDistance = distanceBetween(currentLatLng, first.getLatLng());
            double secondDistance = distanceBetween(currentLatLng, second.getLatLng());
            return Double.compare(firstDistance, secondDistance);
        }

        return first.getTitle().compareToIgnoreCase(second.getTitle());
    }

    private static int localityScore(@NonNull LocationSearchAdapter.LocationResult result,
                                     @Nullable String currentCity) {
        if (currentCity == null || currentCity.trim().isEmpty()) {
            return 0;
        }

        String city = currentCity.toLowerCase(Locale.US);
        String title = result.getTitle().toLowerCase(Locale.US);
        String subtitle = result.getSubtitle().toLowerCase(Locale.US);

        if (title.contains(city)) {
            return 2;
        }
        if (subtitle.contains(city)) {
            return 1;
        }
        return 0;
    }

    private static double distanceBetween(@NonNull LatLng first, @NonNull LatLng second) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(second.latitude - first.latitude);
        double dLon = Math.toRadians(second.longitude - first.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(first.latitude))
                * Math.cos(Math.toRadians(second.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    @NonNull
    private static String readUrl(@NonNull String endpoint) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag());
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            connection.disconnect();
        }
    }

    @Nullable
    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
