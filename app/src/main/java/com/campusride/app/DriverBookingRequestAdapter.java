package com.campusride.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DriverBookingRequestAdapter extends RecyclerView.Adapter<DriverBookingRequestAdapter.RequestViewHolder> {

    private final List<DriverBookingRequestItem> requestItems = new ArrayList<>();
    private final OnBookingActionListener onBookingActionListener;

    public DriverBookingRequestAdapter(OnBookingActionListener onBookingActionListener) {
        this.onBookingActionListener = onBookingActionListener;
    }

    public void submitList(@NonNull List<DriverBookingRequestItem> items) {
        requestItems.clear();
        requestItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_booking_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        DriverBookingRequestItem item = requestItems.get(position);
        holder.tvPassenger.setText(item.getPassengerName());
        holder.tvRoute.setText(item.getRoute());
        holder.tvSeats.setText("Seats requested: " + item.getSeatsBooked());
        holder.tvStatus.setText(item.getStatus());

        boolean pending = "pending".equalsIgnoreCase(item.getStatus());
        holder.btnAccept.setVisibility(pending ? View.VISIBLE : View.GONE);
        holder.btnReject.setVisibility(pending ? View.VISIBLE : View.GONE);

        holder.btnAccept.setOnClickListener(v -> onBookingActionListener.onAccept(item));
        holder.btnReject.setOnClickListener(v -> onBookingActionListener.onReject(item));
    }

    @Override
    public int getItemCount() {
        return requestItems.size();
    }

    public interface OnBookingActionListener {
        void onAccept(DriverBookingRequestItem item);
        void onReject(DriverBookingRequestItem item);
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPassenger;
        private final TextView tvRoute;
        private final TextView tvSeats;
        private final TextView tvStatus;
        private final Button btnAccept;
        private final Button btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassenger = itemView.findViewById(R.id.tvRequestPassenger);
            tvRoute = itemView.findViewById(R.id.tvRequestRoute);
            tvSeats = itemView.findViewById(R.id.tvRequestSeats);
            tvStatus = itemView.findViewById(R.id.tvRequestStatus);
            btnAccept = itemView.findViewById(R.id.btnAcceptBooking);
            btnReject = itemView.findViewById(R.id.btnRejectBooking);
        }
    }

    public static class DriverBookingRequestItem {
        private final Booking booking;
        private final Ride ride;

        public DriverBookingRequestItem(Booking booking, Ride ride) {
            this.booking = booking;
            this.ride = ride;
        }

        public Booking getBooking() {
            return booking;
        }

        public Ride getRide() {
            return ride;
        }

        public String getPassengerName() {
            return booking.getPassengerName();
        }

        public String getRoute() {
            return ride.getOrigin() + " -> " + ride.getDestination();
        }

        public int getSeatsBooked() {
            return booking.getSeatsBooked();
        }

        public String getStatus() {
            return booking.getStatus();
        }
    }
}
