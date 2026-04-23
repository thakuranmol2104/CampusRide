package com.campusride.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationSearchAdapter extends RecyclerView.Adapter<LocationSearchAdapter.LocationViewHolder> {

    private final List<LocationResult> results = new ArrayList<>();
    private final OnLocationClickListener onLocationClickListener;

    public LocationSearchAdapter(OnLocationClickListener onLocationClickListener) {
        this.onLocationClickListener = onLocationClickListener;
    }

    public void submitList(@NonNull List<LocationResult> items) {
        results.clear();
        results.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_result, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        LocationResult result = results.get(position);
        holder.tvTitle.setText(result.getTitle());
        holder.tvSubtitle.setText(result.getSubtitle());
        holder.itemView.setOnClickListener(v -> onLocationClickListener.onLocationClick(result));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public interface OnLocationClickListener {
        void onLocationClick(LocationResult result);
    }

    public static class LocationViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvSubtitle;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLocationTitle);
            tvSubtitle = itemView.findViewById(R.id.tvLocationSubtitle);
        }
    }

    public static class LocationResult {
        private final String title;
        private final String subtitle;
        private final LatLng latLng;
        private final String placeId;
        private final Integer distanceMeters;

        public LocationResult(String title, String subtitle, LatLng latLng) {
            this(title, subtitle, latLng, null, null);
        }

        public LocationResult(String title,
                              String subtitle,
                              LatLng latLng,
                              String placeId,
                              Integer distanceMeters) {
            this.title = title;
            this.subtitle = subtitle;
            this.latLng = latLng;
            this.placeId = placeId;
            this.distanceMeters = distanceMeters;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public LatLng getLatLng() {
            return latLng;
        }

        public String getPlaceId() {
            return placeId;
        }

        public Integer getDistanceMeters() {
            return distanceMeters;
        }
    }
}
