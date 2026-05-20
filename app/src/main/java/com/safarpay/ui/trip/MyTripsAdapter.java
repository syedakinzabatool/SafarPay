package com.safarpay.ui.trip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.safarpay.R;
import com.safarpay.data.local.entity.Trip;
import java.util.ArrayList;
import java.util.List;

public class MyTripsAdapter extends RecyclerView.Adapter<MyTripsAdapter.VH> {

    public interface TripActionListener {
        void onSetActive(Trip trip);
        void onEditBudget(Trip trip);
        void onDelete(Trip trip);
    }

    private List<Trip> trips = new ArrayList<>();
    private final TripActionListener listener;

    public MyTripsAdapter(TripActionListener listener) {
        this.listener = listener;
    }

    public void setTrips(List<Trip> data) {
        trips = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Trip t = trips.get(pos);
        h.tvName.setText(t.name);
        h.tvDestination.setText(t.destination);
        h.tvDates.setText(t.startDate + " → " + t.endDate);
        h.tvBudget.setText(String.format("Budget: PKR %.0f", t.budgetPKR));

        if (t.isActive == 1) {
            h.tvBadge.setVisibility(View.VISIBLE);
            h.btnSetActive.setEnabled(false);
            h.btnSetActive.setAlpha(0.4f);
        } else {
            h.tvBadge.setVisibility(View.GONE);
            h.btnSetActive.setEnabled(true);
            h.btnSetActive.setAlpha(1f);
        }

        h.btnSetActive.setOnClickListener(v -> listener.onSetActive(t));
        h.btnEditBudget.setOnClickListener(v -> listener.onEditBudget(t));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(t));
    }

    @Override public int getItemCount() { return trips.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDestination, tvDates, tvBudget, tvBadge;
        Button btnSetActive, btnEditBudget, btnDelete;

        VH(View v) {
            super(v);
            tvName        = v.findViewById(R.id.tvItemTripName);
            tvDestination = v.findViewById(R.id.tvItemDestination);
            tvDates       = v.findViewById(R.id.tvItemDates);
            tvBudget      = v.findViewById(R.id.tvItemBudget);
            tvBadge       = v.findViewById(R.id.tvActiveBadge);
            btnSetActive  = v.findViewById(R.id.btnSetActive);
            btnEditBudget = v.findViewById(R.id.btnEditBudget);
            btnDelete     = v.findViewById(R.id.btnDeleteTrip);
        }
    }
}