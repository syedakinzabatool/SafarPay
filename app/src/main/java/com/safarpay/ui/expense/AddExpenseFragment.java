package com.safarpay.ui.expense;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.safarpay.R;
import com.safarpay.data.local.entity.Expense;
import com.safarpay.data.local.entity.Trip;
import java.text.SimpleDateFormat;
import java.util.*;

public class AddExpenseFragment extends Fragment {

    private EditText etAmount, etNote;
    private Spinner spinnerCategory;
    private TextView tvConversion, tvDate;
    private Button btnSave, btnPickDate;
    private ExpenseViewModel viewModel;
    private Trip activeTrip;
    private String selectedDate;
    private int editExpenseId = 0;

    private final String[] categories = {"Food", "Transport", "Hotel", "Shopping", "Activities"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        etAmount       = view.findViewById(R.id.etAmount);
        etNote         = view.findViewById(R.id.etNote);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        tvConversion   = view.findViewById(R.id.tvConversion);
        tvDate         = view.findViewById(R.id.tvDate);
        btnSave        = view.findViewById(R.id.btnSave);
        btnPickDate    = view.findViewById(R.id.btnPickDate);

        // Set default date
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText(selectedDate);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item, categories);
        catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        // Check if editing
        if (getArguments() != null) {
            editExpenseId = getArguments().getInt("expenseId", 0);
            if (editExpenseId != 0) {
                viewModel.loadExpense(editExpenseId, expense -> {
                    if (expense != null) requireActivity().runOnUiThread(() -> populateFields(expense));
                });
            }
        }

        viewModel.getActiveTrip().observe(getViewLifecycleOwner(), trip -> {
            activeTrip = trip;
            if (trip != null) viewModel.fetchRate(trip.localCurrency);
        });

        viewModel.getConversionDisplay().observe(getViewLifecycleOwner(), tvConversion::setText);
        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) Navigation.findNavController(requireView()).popBackStack();
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b2, int c) {
                if (activeTrip != null)
                    viewModel.updateConversion(s.toString(), activeTrip.localCurrency);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnPickDate.setOnClickListener(v -> {
            com.google.android.material.datepicker.MaterialDatePicker<Long> picker =
                    com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                            .setTheme(R.style.SafarPayDatePicker)
                            .setTitleText("Select Expense Date").build();
            picker.addOnPositiveButtonClickListener(sel -> {
                selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(sel));
                tvDate.setText(selectedDate);
            });
            picker.show(getParentFragmentManager(), "expense_date");
        });

        btnSave.setOnClickListener(v -> saveExpense());
    }

    private void populateFields(Expense e) {
        etAmount.setText(String.valueOf(e.amount));
        etNote.setText(e.note);
        tvDate.setText(e.date);
        selectedDate = e.date;
        List<String> cats = Arrays.asList(categories);
        int idx = cats.indexOf(e.category);
        if (idx >= 0) spinnerCategory.setSelection(idx);
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), "Enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (activeTrip == null) {
            Toast.makeText(getContext(), "No active trip found", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr);
        double rate   = viewModel.getCurrentRate();
        double pkr    = rate > 0 ? amount / rate : amount;

        Expense expense = new Expense(
                activeTrip.id, amount, pkr, activeTrip.localCurrency,
                spinnerCategory.getSelectedItem().toString(),
                etNote.getText().toString().trim(), selectedDate
        );
        expense.id = editExpenseId;
        viewModel.saveExpense(expense);
    }
}