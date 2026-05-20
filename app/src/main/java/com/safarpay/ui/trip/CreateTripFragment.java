package com.safarpay.ui.trip;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.safarpay.R;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.util.CountryCurrencyHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class CreateTripFragment extends Fragment {

    private EditText etTripName, etBudget;
    private Spinner spinnerCountry;
    private TextView tvCurrency, tvStartDate, tvEndDate;
    private Button btnStartDate, btnEndDate, btnSave;
    private TripViewModel viewModel;
    private String startDate, endDate;
    private Trip pendingTrip;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_create_trip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TripViewModel.class);

        etTripName    = view.findViewById(R.id.etTripName);
        etBudget      = view.findViewById(R.id.etBudget);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        tvCurrency    = view.findViewById(R.id.tvCurrency);
        tvStartDate   = view.findViewById(R.id.tvStartDate);
        tvEndDate     = view.findViewById(R.id.tvEndDate);
        btnStartDate  = view.findViewById(R.id.btnStartDate);
        btnEndDate    = view.findViewById(R.id.btnEndDate);
        btnSave       = view.findViewById(R.id.btnSave);

        List<String> countries = CountryCurrencyHelper.getCountries();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item, countries);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCountry.setAdapter(adapter);

        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                String country = countries.get(pos);
                tvCurrency.setText("Currency: " + CountryCurrencyHelper.getCurrencyCode(country));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnSave.setOnClickListener(v -> saveTrip());

        // Trip saved successfully → go back
        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved))
                Navigation.findNavController(requireView()).popBackStack();
        });

        // Overlap detected → show warning dialog
        viewModel.getOverlapFound().observe(getViewLifecycleOwner(), overlap -> {
            if (Boolean.TRUE.equals(overlap) && pendingTrip != null) {
                showOverlapDialog();
            }
        });
    }

    private void showDatePicker(boolean isStart) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.SafarPayDatePicker)
                .setTitleText(isStart ? "Select Start Date" : "Select End Date")
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(new Date(sel));
            if (isStart) { startDate = date; tvStartDate.setText(date); }
            else          { endDate   = date; tvEndDate.setText(date); }
        });
        picker.show(getParentFragmentManager(), "date_picker");
    }

    private void showOverlapDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("⚠ Date Overlap")
                .setMessage("You already have a trip during these dates. An active trip will be replaced.\n\nDo you want to continue?")
                .setPositiveButton("Yes, Continue", (d, w) -> viewModel.saveTrip(pendingTrip))
                .setNegativeButton("Cancel", (d, w) -> pendingTrip = null)
                .show();
    }

    private void saveTrip() {
        String name = etTripName.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(budgetStr)) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startDate == null || endDate == null) {
            Toast.makeText(getContext(), "Please select both dates", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endDate.compareTo(startDate) <= 0) {
            Toast.makeText(getContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }
        double budget = Double.parseDouble(budgetStr);
        if (budget <= 0) {
            Toast.makeText(getContext(), "Budget must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        String country  = spinnerCountry.getSelectedItem().toString();
        String currency = CountryCurrencyHelper.getCurrencyCode(country);
        String uid      = FirebaseAuth.getInstance().getUid();

        pendingTrip = new Trip(name, country, currency, startDate, endDate, budget, uid);
        viewModel.checkOverlapThenSave(pendingTrip);
    }
}