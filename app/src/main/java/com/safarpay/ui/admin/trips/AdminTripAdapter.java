package com.safarpay.ui.admin.trips;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.safarpay.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminTripAdapter extends RecyclerView.Adapter<AdminTripAdapter.VH> {

    public interface OnTripClickListener {
        void onTripClick(AdminTripsFragment.TripItem trip);
    }

    private final List<AdminTripsFragment.TripItem> data = new ArrayList<>();
    private final OnTripClickListener listener;

    public AdminTripAdapter(List<AdminTripsFragment.TripItem> initial, OnTripClickListener listener) {
        if (initial != null) data.addAll(initial);
        this.listener = listener;
    }

    public void setData(List<AdminTripsFragment.TripItem> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_trip_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AdminTripsFragment.TripItem trip = data.get(position);

        h.tvTripName.setText(trip.name);
        h.tvDestination.setText(trip.destination);
        h.tvOwner.setText("Owner: " + trip.owner);
        h.tvBudget.setText(String.format(Locale.getDefault(), "PKR %.0f", trip.budgetPkr));
        h.tvStatus.setText(trip.active ? "ACTIVE" : "DONE");
        h.tvStatus.setBackgroundResource(trip.active ? R.drawable.bg_pill_success : R.drawable.bg_pill_warning);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTripClick(trip);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTripName;
        TextView tvDestination;
        TextView tvOwner;
        TextView tvBudget;
        TextView tvStatus;

        VH(View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tvTripName);
            tvDestination = itemView.findViewById(R.id.tvTripDestination);
            tvOwner = itemView.findViewById(R.id.tvTripOwner);
            tvBudget = itemView.findViewById(R.id.tvTripBudget);
            tvStatus = itemView.findViewById(R.id.tvTripStatusBadge);
        }
    }
}
