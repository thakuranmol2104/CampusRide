package com.campusride.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchRideActivity extends AppCompatActivity {

    private EditText etSearchOrigin, etSearchDestination;
    private Button btnSearchRide;
    private RecyclerView recyclerRides;

    private List<Ride> rideList;
    private RideAdapter rideAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_ride);

        etSearchOrigin = findViewById(R.id.etSearchOrigin);
        etSearchDestination = findViewById(R.id.etSearchDestination);
        btnSearchRide = findViewById(R.id.btnSearchRide);
        recyclerRides = findViewById(R.id.recyclerRides);

        rideList = new ArrayList<>();
        rideAdapter = new RideAdapter(rideList);

        recyclerRides.setLayoutManager(new LinearLayoutManager(this));
        recyclerRides.setAdapter(rideAdapter);

        btnSearchRide.setOnClickListener(v -> searchRides());
    }

    private void searchRides() {
        String origin = etSearchOrigin.getText().toString().trim();
        String destination = etSearchDestination.getText().toString().trim();

        if (TextUtils.isEmpty(origin)) {
            etSearchOrigin.setError("Enter origin");
            etSearchOrigin.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(destination)) {
            etSearchDestination.setError("Enter destination");
            etSearchDestination.requestFocus();
            return;
        }

        RideHelper.getInstance().searchRides(origin, destination, task -> {
            if (task.isSuccessful()) {
                rideList.clear();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Ride ride = document.toObject(Ride.class);
                    rideList.add(ride);
                }

                rideAdapter.notifyDataSetChanged();

                if (rideList.isEmpty()) {
                    Toast.makeText(this, "No rides found", Toast.LENGTH_SHORT).show();
                }
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Search failed";
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}