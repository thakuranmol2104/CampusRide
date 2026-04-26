package com.campusride.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GoogleRoutesHelper {

    private static final String ROUTES_ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private GoogleRoutesHelper() {
    }

    @Nullable
    public static RouteResult computeRoute(@NonNull Context context,
                                           @NonNull LatLng origin,
                                           @NonNull LatLng destination) throws Exception {
        String apiKey = PlacesAutoCompleteHelper.requireApiKey(context);

        HttpURLConnection connection = (HttpURLConnection) new URL(ROUTES_ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-Goog-Api-Key", apiKey);
        connection.setRequestProperty(
                "X-Goog-FieldMask",
                "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"
        );
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        JSONObject body = new JSONObject();
        body.put("origin", waypoint(origin));
        body.put("destination", waypoint(destination));
        body.put("travelMode", "DRIVE");
        body.put("routingPreference", "TRAFFIC_AWARE");
        body.put("polylineQuality", "HIGH_QUALITY");
        body.put("polylineEncoding", "ENCODED_POLYLINE");
        body.put("languageCode", Locale.getDefault().toLanguageTag());
        body.put("units", "METRIC");

        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(bytes);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException(readStream(connection.getErrorStream()));
        }

        JSONObject response = new JSONObject(readStream(connection.getInputStream()));
        JSONArray routes = response.optJSONArray("routes");
        if (routes == null || routes.length() == 0) {
            return null;
        }

        JSONObject route = routes.getJSONObject(0);
        JSONObject polyline = route.optJSONObject("polyline");
        String encodedPolyline = polyline != null ? polyline.optString("encodedPolyline") : null;
        if (encodedPolyline == null || encodedPolyline.trim().isEmpty()) {
            return null;
        }

        int distanceMeters = route.optInt("distanceMeters", 0);
        String durationRaw = route.optString("duration");
        return new RouteResult(
                decodePolyline(encodedPolyline),
                distanceMeters,
                formatDuration(durationRaw)
        );
    }

    public static void computeRouteAsync(@NonNull Context context,
                                         @NonNull LatLng origin,
                                         @NonNull LatLng destination,
                                         @NonNull RouteResultCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onResult(computeRoute(context, origin, destination));
            } catch (Exception exception) {
                callback.onResult(null);
            }
        });
    }

    @NonNull
    private static JSONObject waypoint(@NonNull LatLng latLng) throws Exception {
        JSONObject location = new JSONObject();
        JSONObject latLngJson = new JSONObject();
        latLngJson.put("latitude", latLng.latitude);
        latLngJson.put("longitude", latLng.longitude);
        location.put("latLng", latLngJson);

        JSONObject waypoint = new JSONObject();
        waypoint.put("location", location);
        return waypoint;
    }

    @NonNull
    private static String readStream(@Nullable java.io.InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    @NonNull
    private static List<LatLng> decodePolyline(@NonNull String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < encoded.length()) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encoded.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encoded.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            polyline.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return polyline;
    }

    @NonNull
    private static String formatDuration(@Nullable String durationRaw) {
        if (durationRaw == null || !durationRaw.endsWith("s")) {
            return "";
        }

        try {
            long totalSeconds = Math.round(Double.parseDouble(durationRaw.substring(0, durationRaw.length() - 1)));
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;

            if (hours > 0) {
                return hours + " hr " + minutes + " min";
            }
            if (minutes > 0) {
                return minutes + " min";
            }
            return Math.max(totalSeconds, 1) + " sec";
        } catch (Exception ignored) {
            return "";
        }
    }

    public static final class RouteResult {
        private final List<LatLng> points;
        private final int distanceMeters;
        private final String durationText;

        public RouteResult(@NonNull List<LatLng> points, int distanceMeters, @NonNull String durationText) {
            this.points = points;
            this.distanceMeters = distanceMeters;
            this.durationText = durationText;
        }

        @NonNull
        public List<LatLng> getPoints() {
            return points;
        }

        public int getDistanceMeters() {
            return distanceMeters;
        }

        @NonNull
        public String getDurationText() {
            return durationText;
        }
    }

    public interface RouteResultCallback {
        void onResult(@Nullable RouteResult routeResult);
    }
}
