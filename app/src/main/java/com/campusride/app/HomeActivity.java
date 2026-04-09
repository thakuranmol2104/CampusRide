package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button btnPostRide, btnSearchRides, btnMyRides, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnPostRide = findViewById(R.id.btnPostRide);
        btnSearchRides = findViewById(R.id.btnSearchRides);
        btnMyRides = findViewById(R.id.btnMyRides);
        btnLogout = findViewById(R.id.btnLogout);

        btnPostRide.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, PostRideActivity.class))
        );

        btnSearchRides.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SearchRideActivity.class))
        );

        btnMyRides.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MyRidesActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            FirebaseHelper.getInstance().logoutUser();
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        });
    }
}