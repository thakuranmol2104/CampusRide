package com.campusride.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class SessionManager {

    private static final String PREF_NAME = "campusride_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences preferences;

    public SessionManager(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLoginSession(@NonNull String email) {
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public boolean hasActiveSession() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    @NonNull
    public String getSavedEmail() {
        String email = preferences.getString(KEY_EMAIL, "");
        return email != null ? email : "";
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
