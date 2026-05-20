package com.safarpay.ui.home;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.safarpay.R;
import com.safarpay.data.local.entity.Trip;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;

    private TextView tvTripName, tvDestination, tvDates, tvSpent, tvRemaining;
    private TextView tvDaysLeftStats;
    private TextView tvNoTrip, tvNotice;
    private TextView tvWelcomeMessage;

    private ProgressBar progressBudget;
    private View cardTrip, bannerNotice;
    private FloatingActionButton fabAddExpense;
    private Button btnNewTrip, btnMyTrips;
    private ImageButton btnDismissNotice;

    private SharedPreferences noticePreferences;
    private String currentNoticeMessage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Bind views
        tvTripName       = view.findViewById(R.id.tvTripName);
        tvDestination    = view.findViewById(R.id.tvDestination);
        tvDates          = view.findViewById(R.id.tvDates);
        tvSpent          = view.findViewById(R.id.tvSpent);
        tvRemaining      = view.findViewById(R.id.tvRemaining);

        tvDaysLeftStats  = view.findViewById(R.id.tvDaysLeftStats);

        tvNoTrip         = view.findViewById(R.id.tvNoTrip);
        tvNotice         = view.findViewById(R.id.tvNotice);
        tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage);
        btnDismissNotice = view.findViewById(R.id.btnDismissNotice);

        progressBudget   = view.findViewById(R.id.progressBudget);
        cardTrip         = view.findViewById(R.id.cardTrip);
        bannerNotice     = view.findViewById(R.id.bannerNotice);

        noticePreferences = requireContext().getSharedPreferences("home_notice_state", Context.MODE_PRIVATE);

        fabAddExpense    = view.findViewById(R.id.fabAddExpense);
        btnNewTrip  = view.findViewById(R.id.btnNewTrip);
        btnMyTrips  = view.findViewById(R.id.btnMyTrips);

        // Clicks
        fabAddExpense.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_addExpense));

        btnMyTrips.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_myTrips));

        btnNewTrip.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_createTrip));

        // Observers
        viewModel.getActiveTrip().observe(getViewLifecycleOwner(), this::bindTrip);

        viewModel.getNotice().observe(getViewLifecycleOwner(), msg -> {
            currentNoticeMessage = msg;
            if (msg != null) {
                String dismissedNotice = noticePreferences.getString("dismissed_notice", null);
                if (msg.equals(dismissedNotice)) {
                    bannerNotice.setVisibility(View.GONE);
                } else {
                    bannerNotice.setVisibility(View.VISIBLE);
                    tvNotice.setText(msg);
                }
            } else {
                bannerNotice.setVisibility(View.GONE);
            }
        });

        btnDismissNotice.setOnClickListener(v -> {
            if (currentNoticeMessage != null) {
                noticePreferences.edit().putString("dismissed_notice", currentNoticeMessage).apply();
                bannerNotice.setVisibility(View.GONE);
            }
        });

        // Load user name for welcome message
        loadUserName();
    }

    private void loadUserName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && tvWelcomeMessage != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (isAdded()) {
                            String name = doc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvWelcomeMessage.setText("Welcome " + name);
                            }
                        }
                    });
        }
    }

    private void bindTrip(Trip trip) {
        if (trip == null) {
            cardTrip.setVisibility(View.GONE);
            tvNoTrip.setVisibility(View.VISIBLE);
            fabAddExpense.hide();
            return;
        }

        cardTrip.setVisibility(View.VISIBLE);
        tvNoTrip.setVisibility(View.GONE);
        fabAddExpense.show();

        tvTripName.setText(trip.name);
        tvDestination.setText(trip.destination);
        tvDates.setText(trip.startDate + " → " + trip.endDate);

        // Days left calculation
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date end = sdf.parse(trip.endDate);

            long diff = end.getTime() - System.currentTimeMillis();
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            // 🔥 Clean UI: number on top, text below
            if (days > 0) {
                tvDaysLeftStats.setText(String.valueOf(days));
            } else {
                tvDaysLeftStats.setText("0");
            }

        } catch (Exception ignored) {}

        // Budget + spending
        viewModel.getTotalSpentPKR(trip.id).observe(getViewLifecycleOwner(), spent -> {
            double s = (spent == null) ? 0 : spent;
            double remaining = trip.budgetPKR - s;

            tvSpent.setText(String.format(Locale.getDefault(), "PKR %.0f", s));
            tvRemaining.setText(String.format(Locale.getDefault(), "PKR %.0f", remaining));

            int progress = (trip.budgetPKR > 0)
                    ? (int) ((s / trip.budgetPKR) * 100)
                    : 0;

            progressBudget.setProgress(progress);
        });
    }
}