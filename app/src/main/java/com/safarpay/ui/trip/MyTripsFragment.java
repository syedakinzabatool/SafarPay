package com.safarpay.ui.trip;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.safarpay.R;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.data.repository.TripRepository;

public class MyTripsFragment extends Fragment implements MyTripsAdapter.TripActionListener {

    private TripViewModel viewModel;
    private MyTripsAdapter adapter;
    private TextView tvNoTrips;
    private TripRepository repository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_trips, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new TripRepository(requireActivity().getApplication());
        viewModel  = new ViewModelProvider(this).get(TripViewModel.class);

        tvNoTrips = view.findViewById(R.id.tvNoTrips);

        adapter = new MyTripsAdapter(this);
        RecyclerView rv = view.findViewById(R.id.rvTrips);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        viewModel.getAllTrips().observe(getViewLifecycleOwner(), trips -> {
            adapter.setTrips(trips);
            tvNoTrips.setVisibility(trips == null || trips.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onSetActive(Trip trip) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Switch Active Trip")
                .setMessage("Set \"" + trip.name + "\" as your active trip?\n\nDashboard will update to show this trip.")
                .setPositiveButton("Yes, Switch", (d, w) -> repository.setActiveTrip(trip.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditBudget(Trip trip) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("New budget in PKR");
        input.setText(String.valueOf((int) trip.budgetPKR));
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(p, p, p, p);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Budget — " + trip.name)
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        trip.budgetPKR = Double.parseDouble(val);
                        repository.updateTrip(trip, null);
                        Toast.makeText(requireContext(), "Budget updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(Trip trip) {
        // 3-step verification
        new AlertDialog.Builder(requireContext())
                .setTitle("⚠ Delete Trip")
                .setMessage("Delete \"" + trip.name + "\"?\n\nThis will permanently delete the trip and ALL its expenses. This cannot be undone.")
                .setPositiveButton("Yes, Delete", (d, w) -> confirmDelete2(trip))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete2(Trip trip) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Are you absolutely sure?")
                .setMessage("All expenses for this trip will be lost forever.")
                .setPositiveButton("DELETE", (d, w) -> {
                    repository.deleteTrip(trip);
                    Toast.makeText(requireContext(), "Trip deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}