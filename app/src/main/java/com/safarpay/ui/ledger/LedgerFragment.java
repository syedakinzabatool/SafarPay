package com.safarpay.ui.ledger;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.safarpay.R;
import com.safarpay.adapter.ExpenseAdapter;
import com.safarpay.data.local.entity.Expense;

public class LedgerFragment extends Fragment {

    private LedgerViewModel viewModel;
    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private EditText etSearch;
    private ChipGroup chipGroup;
    private View layoutEmptyLedger;
    private Button btnAddFirstExpense;
    private String selectedCategory = "All";
    private int activeTripId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_ledger, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LedgerViewModel.class);

        recyclerView = view.findViewById(R.id.recyclerExpenses);
        etSearch     = view.findViewById(R.id.etSearch);
        chipGroup    = view.findViewById(R.id.chipGroup);
        layoutEmptyLedger = view.findViewById(R.id.layoutEmptyLedger);
        btnAddFirstExpense = view.findViewById(R.id.btnAddFirstExpense);

        adapter = new ExpenseAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        adapter.attachSwipe(recyclerView);

        adapter.setOnItemClickListener(e -> {
            Bundle args = new Bundle();
            args.putInt("expenseId", e.id);
            Navigation.findNavController(requireView()).navigate(R.id.action_ledger_to_addExpense, args);
        });

        btnAddFirstExpense.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_ledger_to_addExpense));

        adapter.setOnItemSwipeListener((e, pos) -> {
            viewModel.deleteExpense(e);
            Snackbar.make(requireView(), "Expense deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> viewModel.restoreExpense(e))
                .show();
        });

        // Chip filters
        String[] cats = {"All", "Food", "Transport", "Hotel", "Shopping", "Activities"};
        for (String cat : cats) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChecked(cat.equals("All"));
            chip.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    selectedCategory = cat;
                    loadExpenses();
                }
            });
            chipGroup.addView(chip);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b2, int c) {
                if (activeTripId != -1) {
                    if (s.toString().isEmpty()) loadExpenses();
                    else viewModel.searchExpenses(activeTripId, s.toString())
                        .observe(getViewLifecycleOwner(), expenses -> {
                            adapter.setExpenses(expenses);
                            updateEmptyState(expenses);
                        });
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getActiveTrip().observe(getViewLifecycleOwner(), trip -> {
            if (trip != null) { activeTripId = trip.id; loadExpenses(); }
        });
    }

    private void loadExpenses() {
        if (activeTripId == -1) return;
        viewModel.getExpenses(activeTripId, selectedCategory)
            .observe(getViewLifecycleOwner(), expenses -> {
                adapter.setExpenses(expenses);
                updateEmptyState(expenses);
            });
    }

    private void updateEmptyState(java.util.List<Expense> expenses) {
        boolean isEmpty = expenses == null || expenses.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyLedger.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
