package com.campusride.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREF_NAME = "campusride_theme";
    private static final String KEY_THEME_MODE = "theme_mode";

    private final SharedPreferences preferences;

    public ThemeManager(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void applySavedTheme() {
        AppCompatDelegate.setDefaultNightMode(getSavedThemeMode());
    }

    public void toggleTheme() {
        int nextMode = isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;
        preferences.edit().putInt(KEY_THEME_MODE, nextMode).apply();
        AppCompatDelegate.setDefaultNightMode(nextMode);
    }

    public boolean isDarkMode() {
        return getSavedThemeMode() == AppCompatDelegate.MODE_NIGHT_YES;
    }

    private int getSavedThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO);
    }
}
