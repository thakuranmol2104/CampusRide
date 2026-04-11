package com.campusride.app;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyRidesActivity extends AppCompatActivity {

    private RecyclerView recyclerMyRides;
    private List<Ride> rideList;
    private RideAdapter rideAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        recyclerMyRides = findViewById(R.id.recyclerMyRides);

        rideList = new ArrayList<>();
        rideAdapter = new RideAdapter(rideList);

        recyclerMyRides.setLayoutManager(new LinearLayoutManager(this));
        recyclerMyRides.setAdapter(rideAdapter);

        loadMyRides();
    }

    private void loadMyRides() {
        FirebaseUser currentUser = FirebaseHelper.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String driverUid = currentUser.getUid();

        RideHelper.getInstance().getRidesByDriver(driverUid, task -> {
            if (task.isSuccessful()) {
                rideList.clear();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Ride ride = document.toObject(Ride.class);
                    rideList.add(ride);
                }

                rideAdapter.notifyDataSetChanged();

                if (rideList.isEmpty()) {
                    Toast.makeText(this, "No rides posted yet", Toast.LENGTH_SHORT).show();
                }
            } else {
                String error = task.getException() != null
                        ? task.getException().getMessage()
                        : "Failed to load rides";
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}