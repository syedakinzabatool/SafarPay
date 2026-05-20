package com.safarpay.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.safarpay.R;
import com.safarpay.auth.LoginActivity;
import com.safarpay.data.local.AppDatabase;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        TextView tvName    = view.findViewById(R.id.tvName);
        TextView tvEmail   = view.findViewById(R.id.tvEmail);
        TextView tvInitial = view.findViewById(R.id.tvInitial);
        TextView tvTrips   = view.findViewById(R.id.tvTripCount);
        TextView tvExpenses= view.findViewById(R.id.tvExpenseCount);
        TextView tvAccountStatus = view.findViewById(R.id.tvAccountStatus);

        if (user != null) {
            String email = user.getEmail() != null ? user.getEmail() : "";
            tvEmail.setText(email);

            // Load name from Firestore
            db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvName.setText(name);
                            tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                        }

                        boolean disabled = Boolean.TRUE.equals(doc.getBoolean("disabled"));
                        if (disabled) {
                            tvAccountStatus.setVisibility(View.VISIBLE);
                            tvAccountStatus.setText("Your account is deactivated. Please contact admin support.");
                            tvAccountStatus.setTextColor(Color.parseColor("#B71C1C"));
                        } else {
                            tvAccountStatus.setVisibility(View.GONE);
                        }
                    }
                });

            // Load local Room stats
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase roomDb = AppDatabase.getInstance(requireContext());
                int tripCount = roomDb.tripDao().getAllTripsSync() != null
                    ? roomDb.tripDao().getAllTripsSync().size() : 0;
                int expenseCount = roomDb.expenseDao().getAllExpensesSync() != null
                    ? roomDb.expenseDao().getAllExpensesSync().size() : 0;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        tvTrips.setText(String.valueOf(tripCount));
                        tvExpenses.setText(String.valueOf(expenseCount));
                    });
                }
            });
        }

        // Currency Converter shortcut
        view.findViewById(R.id.rowConverter).setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.converterFragment));

        // About dialog
        view.findViewById(R.id.rowAbout).setOnClickListener(v ->
            new AlertDialog.Builder(requireContext())
                .setTitle("About SafarPay")
                .setMessage("SafarPay v1.0\n\nA travel budget & expense management app.\nTrack your trips, expenses and currency conversions all in one place.")
                .setPositiveButton("OK", null)
                .show());

        // Logout
        view.findViewById(R.id.btnLogout).setOnClickListener(v ->
            new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(getContext(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }
}
