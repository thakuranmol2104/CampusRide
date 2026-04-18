package com.campusride.app;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private final List<Ride> rideList;
    private final OnRideClickListener onRideClickListener;
    private final OnRideActionClickListener onRideActionClickListener;
    private final String actionLabel;

    public RideAdapter(List<Ride> rideList) {
        this(rideList, null, null, null);
    }

    public RideAdapter(List<Ride> rideList, OnRideClickListener onRideClickListener) {
        this(rideList, onRideClickListener, null, null);
    }

    public RideAdapter(List<Ride> rideList,
                       OnRideClickListener onRideClickListener,
                       OnRideActionClickListener onRideActionClickListener,
                       String actionLabel) {
        this.rideList = rideList;
        this.onRideClickListener = onRideClickListener;
        this.onRideActionClickListener = onRideActionClickListener;
        this.actionLabel = actionLabel;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ride_item, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        Ride ride = rideList.get(position);

        holder.tvDriverName.setText(ride.getDriverName());
        holder.tvRoute.setText(ride.getOrigin() + " -> " + ride.getDestination());
        holder.tvDateTime.setText(ride.getDate() + " | " + ride.getTime());
        holder.tvSeatsPrice.setText(ride.getAvailableSeats() + " seats left | Rs. " + ride.getPricePerSeat() + " / seat");
        holder.tvRideStatus.setText(ride.getStatus());

        holder.btnViewRoute.setOnClickListener(v -> {
            if (onRideClickListener != null) {
                onRideClickListener.onRideClick(ride);
            }
        });

        if (TextUtils.isEmpty(actionLabel) || onRideActionClickListener == null) {
            holder.btnRideAction.setVisibility(View.GONE);
        } else {
            holder.btnRideAction.setVisibility(View.VISIBLE);
            holder.btnRideAction.setText(actionLabel);
            holder.btnRideAction.setOnClickListener(v -> onRideActionClickListener.onRideActionClick(ride));
        }
    }

    @Override
    public int getItemCount() {
        return rideList.size();
    }

    public static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName;
        TextView tvRoute;
        TextView tvDateTime;
        TextView tvSeatsPrice;
        TextView tvRideStatus;
        Button btnViewRoute;
        Button btnRideAction;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvSeatsPrice = itemView.findViewById(R.id.tvSeatsPrice);
            tvRideStatus = itemView.findViewById(R.id.tvRideStatus);
            btnViewRoute = itemView.findViewById(R.id.btnViewRoute);
            btnRideAction = itemView.findViewById(R.id.btnRideAction);
        }
    }

    public interface OnRideClickListener {
        void onRideClick(Ride ride);
    }

    public interface OnRideActionClickListener {
        void onRideActionClick(Ride ride);
    }
}
