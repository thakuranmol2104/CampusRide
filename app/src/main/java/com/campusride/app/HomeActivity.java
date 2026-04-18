package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button btnPostRide, btnSearchRides, btnMyRides, btnLogout;
    private TextView tvWelcome;
    private Button btnThemeToggle;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = new ThemeManager(this);
        themeManager.applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnPostRide = findViewById(R.id.btnPostRide);
        btnSearchRides = findViewById(R.id.btnSearchRides);
        btnMyRides = findViewById(R.id.btnMyRides);
        btnLogout = findViewById(R.id.btnLogout);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);

        if (FirebaseHelper.getInstance().getCurrentUser() != null
                && FirebaseHelper.getInstance().getCurrentUser().getEmail() != null) {
            tvWelcome.setText("Welcome back, " + FirebaseHelper.getInstance().getCurrentUser().getEmail());
        }
        updateThemeToggleText();

        btnPostRide.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, com.campusride.app.PostRideActivity.class))
        );

        btnSearchRides.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SearchRideActivity.class))
        );

        btnMyRides.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MyRidesActivity.class))
        );

        btnThemeToggle.setOnClickListener(v -> {
            themeManager.toggleTheme();
            recreate();
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseHelper.getInstance().logoutUser();
            new SessionManager(HomeActivity.this).clearSession();
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        });
    }

    private void updateThemeToggleText() {
        btnThemeToggle.setText(themeManager.isDarkMode() ? "Switch to Light" : "Switch to Dark");
    }
}
