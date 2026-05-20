package com.safarpay.ui.admin.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.safarpay.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminAnalyticsFragment extends Fragment {

    private FirebaseFirestore db;
    private BarChart barChart;
    private TextView tvTotalSpent;
    private TextView tvAvgBudget;
    private TextView tvOverBudgetTrips;
    private TextView tvHighRiskTrips;
    private TextView tvNewUsersMonth;
    private TextView tvDeactivatedUsers;
    private TextView tvTopDestinationName;
    private TextView tvTopDestinationMeta;
    private TextView tvNoData;

    private final List<TripRow> tripRows = new ArrayList<>();
    private final Map<String, Integer> destinationCounts = new LinkedHashMap<>();
    private double totalBudgetAllocated = 0;
    private double totalSpent = 0;
    private int overBudgetTrips = 0;
    private int highRiskTrips = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_admin_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        db = FirebaseFirestore.getInstance();

        barChart = view.findViewById(R.id.barChart);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvAvgBudget = view.findViewById(R.id.tvAvgBudget);
        tvOverBudgetTrips = view.findViewById(R.id.tvOverBudgetTrips);
        tvHighRiskTrips = view.findViewById(R.id.tvHighRiskTrips);
        tvNewUsersMonth = view.findViewById(R.id.tvNewUsersMonth);
        tvDeactivatedUsers = view.findViewById(R.id.tvDeactivatedUsers);
        tvTopDestinationName = view.findViewById(R.id.tvTopDestinationName);
        tvTopDestinationMeta = view.findViewById(R.id.tvTopDestinationMeta);
        tvNoData = view.findViewById(R.id.tvNoData);

        styleChart();
        loadAnalytics();
    }

    private void styleChart() {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setNoDataText("No destination analytics yet");
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-35f);
        xAxis.setTextColor(Color.parseColor("#637370"));
        xAxis.setDrawGridLines(false);

        barChart.getAxisLeft().setTextColor(Color.parseColor("#637370"));
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
    }

    private void loadAnalytics() {
        loadUserAnalytics();
        loadTripAnalytics();
    }

    private void loadUserAnalytics() {
        Calendar now = Calendar.getInstance();
        db.collection("users").get().addOnSuccessListener(uSnap -> {
            if (!isAdded()) return;

            int deactivated = 0;
            int newUsersThisMonth = 0;

            for (DocumentSnapshot doc : uSnap.getDocuments()) {
                if (isTruthy(doc.get("disabled"))) {
                    deactivated++;
                }
                long createdAt = asLong(doc.get("createdAt"));
                if (createdAt > 0 && isSameMonth(createdAt, now)) {
                    newUsersThisMonth++;
                }
            }

            tvDeactivatedUsers.setText(String.valueOf(deactivated));
            tvNewUsersMonth.setText(String.valueOf(newUsersThisMonth));
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Could not load user analytics", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTripAnalytics() {
        db.collection("trips").get().addOnSuccessListener(this::collectTripAnalytics)
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Could not load trip analytics", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void collectTripAnalytics(QuerySnapshot snap) {
        if (!isAdded()) return;

        tripRows.clear();
        destinationCounts.clear();
        totalBudgetAllocated = 0;
        totalSpent = 0;
        overBudgetTrips = 0;
        highRiskTrips = 0;

        if (snap == null || snap.isEmpty()) {
            renderTripAnalytics(true);
            return;
        }

        final int[] remaining = {snap.size()};
        for (DocumentSnapshot doc : snap.getDocuments()) {
            TripRow row = new TripRow();
            row.id = doc.getId();
            row.destination = safe(doc.getString("destination"), "Unknown Destination");
            row.budget = resolveBudget(doc);
            tripRows.add(row);

            totalBudgetAllocated += row.budget;
            destinationCounts.merge(row.destination, 1, Integer::sum);

            fetchSpentForTrip(doc, spent -> {
                row.spent = spent;
                totalSpent += spent;
                if (spent > row.budget) overBudgetTrips++;
                if (row.budget > 0 && spent >= row.budget * 0.8) highRiskTrips++;

                remaining[0]--;
                if (remaining[0] == 0) {
                    renderTripAnalytics(false);
                }
            });
        }
    }

    private void renderTripAnalytics(boolean emptyTrips) {
        if (!isAdded()) return;

        double avgBudget = tripRows.isEmpty() ? 0 : (totalBudgetAllocated / tripRows.size());
        tvTotalSpent.setText(String.format(Locale.getDefault(), "PKR %.0f", totalSpent));
        tvAvgBudget.setText(String.format(Locale.getDefault(), "PKR %.0f", avgBudget));
        tvOverBudgetTrips.setText(String.valueOf(overBudgetTrips));
        tvHighRiskTrips.setText(String.valueOf(highRiskTrips));

        updateTopDestinationCard();
        updateDestinationChart();

        boolean showEmpty = emptyTrips || destinationCounts.isEmpty();
        tvNoData.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        barChart.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateTopDestinationCard() {
        if (destinationCounts.isEmpty()) {
            tvTopDestinationName.setText("No trips yet");
            tvTopDestinationMeta.setText("Top destination will appear here");
            return;
        }

        String bestDestination = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : destinationCounts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestDestination = entry.getKey();
            }
        }

        tvTopDestinationName.setText(bestDestination);
        tvTopDestinationMeta.setText(String.format(Locale.getDefault(), "%d trips", bestCount));
    }

    private void updateDestinationChart() {
        if (destinationCounts.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(destinationCounts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            if (i >= 6) break;
            entries.add(new BarEntry(i, e.getValue()));
            labels.add(e.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Trips per destination");
        dataSet.setColor(Color.parseColor("#00897B"));
        dataSet.setValueTextColor(Color.parseColor("#1A1A2E"));
        dataSet.setValueTextSize(11f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.62f);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size());
        barChart.setData(data);
        barChart.animateY(700);
        barChart.invalidate();
    }

    private void fetchSpentForTrip(DocumentSnapshot doc, SpentCallback callback) {
        Object value = doc.get("totalSpentPKR");
        if (value == null) value = doc.get("spentPKR");
        if (value == null) value = doc.get("spent");

        if (value instanceof Number || value instanceof String) {
            callback.onResult(asDouble(value));
            return;
        }

        doc.getReference().collection("expenses").get()
                .addOnSuccessListener(expenses -> {
                    double sum = 0;
                    for (DocumentSnapshot expenseDoc : expenses.getDocuments()) {
                        Object amount = expenseDoc.get("amountPKR");
                        if (amount == null) amount = expenseDoc.get("amount");
                        sum += asDouble(amount);
                    }
                    callback.onResult(sum);
                })
                .addOnFailureListener(e -> callback.onResult(0));
    }

    private double resolveBudget(DocumentSnapshot doc) {
        Object budget = doc.get("budgetPKR");
        if (budget == null) budget = doc.get("budgetPkr");
        if (budget == null) budget = doc.get("budget");
        return asDouble(budget);
    }

    private double asDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                String normalized = ((String) value).replaceAll("[^0-9.\\-]", "").trim();
                if (normalized.isEmpty()) return 0;
                return Double.parseDouble(normalized);
            } catch (Exception ignored) {
            }
        }
        return 0;
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

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if (value instanceof String) {
            String v = ((String) value).trim().toLowerCase(Locale.getDefault());
            return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "y".equals(v);
        }
        return false;
    }

    private boolean isSameMonth(long timestamp, Calendar reference) {
        Calendar value = Calendar.getInstance();
        value.setTimeInMillis(timestamp);
        return value.get(Calendar.YEAR) == reference.get(Calendar.YEAR)
                && value.get(Calendar.MONTH) == reference.get(Calendar.MONTH);
    }

    private String safe(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    private interface SpentCallback {
        void onResult(double spent);
    }

    private static class TripRow {
        String id;
        String destination;
        double budget;
        double spent;
    }
}
