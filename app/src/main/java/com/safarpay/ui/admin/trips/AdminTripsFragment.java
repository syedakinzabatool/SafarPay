package com.safarpay.ui.admin.trips;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.safarpay.R;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminTripsFragment extends Fragment {
    private FirebaseFirestore db;
    private AdminTripAdapter adapter;
    private RecyclerView recyclerTrips;
    private TextView tvMetricTotal;
    private TextView tvMetricActive;
    private TextView tvMetricBudget;
    private TextView tvEmptyTrips;
    private EditText etTripSearch;
    private Spinner spinnerSort;
    private Button btnAll;
    private Button btnActive;
    private Button btnCompleted;

    private final List<TripItem> allTrips = new ArrayList<>();
    private final Map<String, String> userDirectory = new HashMap<>();
    private String currentFilter = "all";

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle b) {
        return i.inflate(R.layout.fragment_admin_trips, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        db = FirebaseFirestore.getInstance();

        recyclerTrips = view.findViewById(R.id.recyclerTrips);
        tvMetricTotal = view.findViewById(R.id.tvMetricTotalTrips);
        tvMetricActive = view.findViewById(R.id.tvMetricActiveTrips);
        tvMetricBudget = view.findViewById(R.id.tvMetricTotalBudget);
        tvEmptyTrips = view.findViewById(R.id.tvEmptyTrips);
        etTripSearch = view.findViewById(R.id.etTripSearch);
        spinnerSort = view.findViewById(R.id.spinnerSortTrips);
        btnAll = view.findViewById(R.id.btnAll);
        btnActive = view.findViewById(R.id.btnActive);
        btnCompleted = view.findViewById(R.id.btnCompleted);

        adapter = new AdminTripAdapter(new ArrayList<>(), this::showTripDetailsBottomSheet);
        recyclerTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerTrips.setAdapter(adapter);

        setupSortSpinner();
        setupSearchAndFilters();
        setActiveFilterButton("all");

        loadOwnerDirectoryAndTrips();
    }

    private void loadOwnerDirectoryAndTrips() {
        db.collection("users").get()
                .addOnSuccessListener(snap -> {
                    userDirectory.clear();
                    for (DocumentSnapshot userDoc : snap.getDocuments()) {
                        String uid = userDoc.getId();
                        String name = userDoc.getString("name");
                        String email = userDoc.getString("email");
                        String display = !TextUtils.isEmpty(name)
                                ? name
                                : (!TextUtils.isEmpty(email) ? email : uid);
                        userDirectory.put(uid, display);
                    }
                })
                .addOnCompleteListener(task -> loadTrips());
    }

    private void setupSortSpinner() {
        List<String> sortOptions = new ArrayList<>();
        sortOptions.add("Latest");
        sortOptions.add("Highest Budget");
        sortOptions.add("Lowest Budget");
        sortOptions.add("Name A-Z");

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                sortOptions
        );
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndRender();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupSearchAndFilters() {
        etTripSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndRender();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnAll.setOnClickListener(v -> {
            currentFilter = "all";
            setActiveFilterButton(currentFilter);
            applyFiltersAndRender();
        });

        btnActive.setOnClickListener(v -> {
            currentFilter = "active";
            setActiveFilterButton(currentFilter);
            applyFiltersAndRender();
        });

        btnCompleted.setOnClickListener(v -> {
            currentFilter = "completed";
            setActiveFilterButton(currentFilter);
            applyFiltersAndRender();
        });
    }

    private void setActiveFilterButton(String filter) {
        styleFilterButton(btnAll, "all".equals(filter));
        styleFilterButton(btnActive, "active".equals(filter));
        styleFilterButton(btnCompleted, "completed".equals(filter));
    }

    private void styleFilterButton(Button button, boolean selected) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selected
                ? ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                : ContextCompat.getColor(requireContext(), R.color.colorBackground)
        ));
        button.setTextColor(ContextCompat.getColor(
            requireContext(), selected ? android.R.color.white : R.color.colorTextSecondary
        ));
    }

    private void loadTrips() {
        db.collection("trips").get().addOnSuccessListener(snap -> {
            allTrips.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                TripItem item = new TripItem();
                item.id = doc.getId();
                item.name = safe(doc.getString("name"), "Untitled Trip");
                item.destination = safe(doc.getString("destination"), "Unknown Destination");
                item.budgetPkr = resolveBudgetPkr(doc);
                item.active = parseActive(doc.get("isActive"));
                item.createdAt = asLong(doc.get("createdAt"));
                item.startDate = safe(doc.getString("startDate"), "-");
                item.endDate = safe(doc.getString("endDate"), "-");
                item.ownerUid = resolveOwnerUid(doc);
                item.owner = resolveOwner(doc, item.ownerUid);
                item.totalSpentPkr = asDouble(doc.get("totalSpentPKR"));
                allTrips.add(item);
            }
            updateHeaderMetrics();
            applyFiltersAndRender();
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Could not load trips", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHeaderMetrics() {
        int total = allTrips.size();
        int activeCount = 0;
        double totalBudget = 0;

        for (TripItem t : allTrips) {
            if (t.active) activeCount++;
            totalBudget += t.budgetPkr;
        }

        tvMetricTotal.setText(String.valueOf(total));
        tvMetricActive.setText(String.valueOf(activeCount));
        tvMetricBudget.setText(String.format(Locale.getDefault(), "PKR %.0f", totalBudget));
    }

    private void applyFiltersAndRender() {
        List<TripItem> filtered = new ArrayList<>();
        String query = etTripSearch.getText() == null ? "" : etTripSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

        for (TripItem t : allTrips) {
            if (!matchesFilter(t)) continue;
            if (!TextUtils.isEmpty(query)) {
                String hay = (t.name + " " + t.destination + " " + t.owner + " " + t.id).toLowerCase(Locale.getDefault());
                if (!hay.contains(query)) continue;
            }
            filtered.add(t);
        }

        sortTrips(filtered);
        adapter.setData(filtered);

        boolean empty = filtered.isEmpty();
        tvEmptyTrips.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerTrips.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matchesFilter(TripItem t) {
        switch (currentFilter) {
            case "active":
                return t.active;
            case "completed":
                return !t.active;
            default:
                return true;
        }
    }

    private void sortTrips(List<TripItem> list) {
        String selected = spinnerSort.getSelectedItem() == null ? "Latest" : spinnerSort.getSelectedItem().toString();

        if ("Highest Budget".equals(selected)) {
            list.sort((a, b) -> Double.compare(b.budgetPkr, a.budgetPkr));
        } else if ("Lowest Budget".equals(selected)) {
            list.sort(Comparator.comparingDouble(a -> a.budgetPkr));
        } else if ("Name A-Z".equals(selected)) {
            list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        } else {
            list.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        }
    }

    private void showTripDetailsBottomSheet(TripItem trip) {
        if (getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        ViewGroup root = requireActivity().findViewById(android.R.id.content);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_admin_trip_detail, root, false);

        ((TextView) content.findViewById(R.id.tvDetailTripName)).setText(trip.name);
        ((TextView) content.findViewById(R.id.tvDetailTripId)).setText(getString(R.string.trip_detail_id, trip.id));
        ((TextView) content.findViewById(R.id.tvDetailDestination)).setText(trip.destination);
        ((TextView) content.findViewById(R.id.tvDetailOwner)).setText(trip.owner);
        ((TextView) content.findViewById(R.id.tvDetailBudget)).setText(String.format(Locale.getDefault(), "PKR %.0f", trip.budgetPkr));
        ((TextView) content.findViewById(R.id.tvDetailSpent)).setText(String.format(Locale.getDefault(), "PKR %.0f", trip.totalSpentPkr));
        ((TextView) content.findViewById(R.id.tvDetailDates)).setText(getString(R.string.trip_detail_dates, trip.startDate, trip.endDate));
        ((TextView) content.findViewById(R.id.tvDetailStatus)).setText(trip.active ? "ACTIVE" : "DONE");

        content.findViewById(R.id.btnCloseTripDetail).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btnDeleteTrip).setOnClickListener(v -> {
            dialog.dismiss();
            confirmAndDeleteTrip(trip);
        });

        dialog.setContentView(content);
        dialog.show();
    }

    private void confirmAndDeleteTrip(TripItem trip) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_trip_title)
                .setMessage(getString(R.string.delete_trip_confirm, trip.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, which) -> deleteTrip(trip))
                .show();
    }

    private void deleteTrip(TripItem trip) {
        db.collection("trips")
                .document(trip.id)
                .delete()
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), R.string.trip_deleted, Toast.LENGTH_SHORT).show();
                    loadTrips();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), R.string.trip_delete_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private String resolveOwnerUid(DocumentSnapshot doc) {
        String userId = doc.getString("userId");
        if (!TextUtils.isEmpty(userId)) return userId;

        String uid = doc.getString("uid");
        if (!TextUtils.isEmpty(uid)) return uid;

        String createdBy = doc.getString("createdBy");
        if (!TextUtils.isEmpty(createdBy)) return createdBy;

        // Backward compatibility with document IDs like {uid}_{localTripId}
        String docId = doc.getId();
        int underscore = docId.indexOf('_');
        if (underscore > 0) {
            return docId.substring(0, underscore);
        }

        return null;
    }

    private String resolveOwner(DocumentSnapshot doc, String ownerUid) {
        String owner = doc.getString("ownerName");
        if (!TextUtils.isEmpty(owner)) return owner;

        owner = doc.getString("ownerEmail");
        if (!TextUtils.isEmpty(owner)) return owner;

        owner = doc.getString("userEmail");
        if (!TextUtils.isEmpty(owner)) return owner;

        owner = doc.getString("createdBy");
        if (!TextUtils.isEmpty(owner)) return owner;

        if (!TextUtils.isEmpty(ownerUid)) {
            String fromDirectory = userDirectory.get(ownerUid);
            if (!TextUtils.isEmpty(fromDirectory)) return fromDirectory;
            return ownerUid;
        }

        return getString(R.string.unknown_owner);
    }

    private String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private boolean parseActive(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String v = ((String) value).trim().toLowerCase(Locale.getDefault());
            return "1".equals(v) || "true".equals(v) || "active".equals(v);
        }
        return false;
    }

    private double asDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                String normalized = ((String) value)
                        .replaceAll("[^0-9.\\-]", "")
                        .trim();
                if (normalized.isEmpty()) return 0;
                return Double.parseDouble(normalized);
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private double resolveBudgetPkr(DocumentSnapshot doc) {
        // Compatibility for mixed schemas in Firestore.
        Object budget = doc.get("budgetPKR");
        if (budget == null) budget = doc.get("budgetPkr");
        if (budget == null) budget = doc.get("budget");
        if (budget == null) budget = doc.get("totalBudget");
        return asDouble(budget);
    }

    private long asLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    public static class TripItem {
        String id;
        String name;
        String destination;
        String ownerUid;
        String owner;
        String startDate;
        String endDate;
        double budgetPkr;
        double totalSpentPkr;
        long createdAt;
        boolean active;
    }
}