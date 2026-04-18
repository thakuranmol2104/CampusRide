package com.campusride.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PassengerBookingAdapter extends RecyclerView.Adapter<PassengerBookingAdapter.BookingViewHolder> {

    private final List<PassengerBookingItem> bookingItems = new ArrayList<>();

    public void submitList(@NonNull List<PassengerBookingItem> items) {
        bookingItems.clear();
        bookingItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_passenger_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        PassengerBookingItem item = bookingItems.get(position);
        holder.tvRoute.setText(item.getRoute());
        holder.tvDriver.setText("Driver: " + item.getDriverName());
        holder.tvSchedule.setText(item.getSchedule());
        holder.tvStatus.setText(item.getStatus());
    }

    @Override
    public int getItemCount() {
        return bookingItems.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRoute;
        private final TextView tvDriver;
        private final TextView tvSchedule;
        private final TextView tvStatus;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvBookingRoute);
            tvDriver = itemView.findViewById(R.id.tvBookingDriver);
            tvSchedule = itemView.findViewById(R.id.tvBookingSchedule);
            tvStatus = itemView.findViewById(R.id.tvBookingStatus);
        }
    }

    public static class PassengerBookingItem {
        private final String route;
        private final String driverName;
        private final String schedule;
        private final String status;

        public PassengerBookingItem(String route, String driverName, String schedule, String status) {
            this.route = route;
            this.driverName = driverName;
            this.schedule = schedule;
            this.status = status;
        }

        public String getRoute() {
            return route;
        }

        public String getDriverName() {
            return driverName;
        }

        public String getSchedule() {
            return schedule;
        }

        public String getStatus() {
            return status;
        }
    }
}
