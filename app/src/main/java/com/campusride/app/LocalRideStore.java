package com.campusride.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LocalRideStore {

    private static final String PREF_NAME = "campusride_local_rides";
    private static final String KEY_RIDES = "rides";

    private LocalRideStore() {
    }

    public static void saveRide(@NonNull Context context, @NonNull Ride ride) throws Exception {
        if (ride.getRideId() == null || ride.getRideId().trim().isEmpty()) {
            ride.setRideId("local_" + UUID.randomUUID());
        }
        if (ride.getStatus() == null || ride.getStatus().trim().isEmpty()) {
            ride.setStatus("active");
        }

        JSONArray rides = readArray(context);
        rides.put(toJson(ride));
        preferences(context).edit().putString(KEY_RIDES, rides.toString()).apply();
    }

    @NonNull
    public static List<Ride> getActiveRides(@NonNull Context context) {
        List<Ride> rides = new ArrayList<>();
        JSONArray array = readArray(context);
        for (int index = 0; index < array.length(); index++) {
            Ride ride = fromJson(array.optJSONObject(index));
            if (ride != null && "active".equals(ride.getStatus())) {
                rides.add(ride);
            }
        }
        return rides;
    }

    @NonNull
    public static List<Ride> getRidesByDriver(@NonNull Context context, @NonNull String driverUid) {
        List<Ride> rides = new ArrayList<>();
        JSONArray array = readArray(context);
        for (int index = 0; index < array.length(); index++) {
            Ride ride = fromJson(array.optJSONObject(index));
            if (ride != null && driverUid.equals(ride.getDriverUid())) {
                rides.add(ride);
            }
        }
        return rides;
    }

    @NonNull
    private static JSONArray readArray(@NonNull Context context) {
        String raw = preferences(context).getString(KEY_RIDES, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception exception) {
            return new JSONArray();
        }
    }

    @NonNull
    private static SharedPreferences preferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    private static JSONObject toJson(@NonNull Ride ride) throws Exception {
        JSONObject json = new JSONObject();
        json.put("rideId", ride.getRideId());
        json.put("driverUid", ride.getDriverUid());
        json.put("driverName", ride.getDriverName());
        json.put("origin", ride.getOrigin());
        json.put("destination", ride.getDestination());
        json.put("date", ride.getDate());
        json.put("time", ride.getTime());
        json.put("availableSeats", ride.getAvailableSeats());
        json.put("pricePerSeat", ride.getPricePerSeat());
        json.put("status", ride.getStatus());
        return json;
    }

    private static Ride fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        Ride ride = new Ride();
        ride.setRideId(json.optString("rideId"));
        ride.setDriverUid(json.optString("driverUid"));
        ride.setDriverName(json.optString("driverName", "Rider"));
        ride.setOrigin(json.optString("origin"));
        ride.setDestination(json.optString("destination"));
        ride.setDate(json.optString("date"));
        ride.setTime(json.optString("time"));
        ride.setAvailableSeats(json.optInt("availableSeats"));
        ride.setPricePerSeat(json.optDouble("pricePerSeat"));
        ride.setStatus(json.optString("status", "active"));
        return ride;
    }
}
