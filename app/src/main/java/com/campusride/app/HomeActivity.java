package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

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

        loadDisplayName();
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

    private void loadDisplayName() {
        FirebaseUser currentUser = FirebaseHelper.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        FirebaseHelper.getInstance().getDb()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fallback = "Rider";
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                            tvWelcome.setText("Welcome back, " + user.getName());
                            return;
                        }
                    }
                    tvWelcome.setText("Welcome back, " + fallback);
                })
                .addOnFailureListener(exception -> {
                    tvWelcome.setText("Welcome back, Rider");
                });
    }
}
