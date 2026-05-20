package com.safarpay.ui.budget;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.safarpay.R;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.data.local.entity.Expense;
import com.safarpay.data.repository.ExpenseRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.Executors;

public class BudgetFragment extends Fragment {

    private BudgetViewModel viewModel;
    private TextView tvTotalBudget, tvTotalSpent, tvRemaining, tvWarning;
    private PieChart pieChart;
    private LinearLayout llCategoryBars;
    private Button btnExportPDF;
    private ExpenseRepository expenseRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        tvTotalBudget  = view.findViewById(R.id.tvTotalBudget);
        tvTotalSpent   = view.findViewById(R.id.tvTotalSpent);
        tvRemaining    = view.findViewById(R.id.tvRemaining);
        tvWarning      = view.findViewById(R.id.tvWarning);
        pieChart       = view.findViewById(R.id.pieChart);
        btnExportPDF   = view.findViewById(R.id.btnExportPDF);
        llCategoryBars = view.findViewById(R.id.llCategoryBars);
        expenseRepository = new ExpenseRepository(requireActivity().getApplication());

        setupPieChart();

        btnExportPDF.setOnClickListener(v -> exportToPDF());

        viewModel.getActiveTrip().observe(getViewLifecycleOwner(), trip -> {
            if (trip == null) return;
            tvTotalBudget.setText(String.format(Locale.getDefault(), "PKR %.0f", trip.budgetPKR));
            viewModel.getTotalSpent(trip.id).observe(getViewLifecycleOwner(), spent -> {
                double s = (spent == null) ? 0 : spent;
                double rem = trip.budgetPKR - s;
                tvTotalSpent.setText(String.format(Locale.getDefault(), "PKR %.0f", s));
                tvRemaining.setText(String.format(Locale.getDefault(), "PKR %.0f", rem));
                tvWarning.setVisibility((s / trip.budgetPKR) >= 0.8 ? View.VISIBLE : View.GONE);
            });
            viewModel.loadCategorySpending(trip.id);
        });

        viewModel.getCategorySpending().observe(getViewLifecycleOwner(), data -> {
            updatePieChart(data);
            updateCategoryBars(data);
        });
    }

    private void exportToPDF() {
        Trip trip = viewModel.getActiveTrip().getValue();
        if (trip == null) {
            Toast.makeText(getContext(), "No active trip", Toast.LENGTH_SHORT).show();
            return;
        }

        Context context = getContext();
        if (context == null) return;

        btnExportPDF.setEnabled(false);
        Toast.makeText(context, "Generating PDF...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            String fileName = "SafarPay_" + trip.name.replaceAll("[^a-zA-Z0-9_-]", "_")
                    + "_" + System.currentTimeMillis() + ".pdf";
            try {
                List<Expense> expenses = expenseRepository.getExpensesForTripSync(trip.id);
                Map<String, Double> categories = new LinkedHashMap<>();
                double totalSpent = 0;
                if (expenses != null) {
                    for (Expense e : expenses) {
                        totalSpent += e.amountPKR;
                        double catTotal = categories.getOrDefault(e.category, 0.0);
                        categories.put(e.category, catTotal + e.amountPKR);
                    }
                }

                PdfDocument document = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);

                Paint titlePaint = new Paint();
                titlePaint.setColor(Color.BLACK);
                titlePaint.setTextSize(22f);
                titlePaint.setFakeBoldText(true);

                Paint bodyPaint = new Paint();
                bodyPaint.setColor(Color.DKGRAY);
                bodyPaint.setTextSize(13f);

                int y = 50;
                page.getCanvas().drawText("SafarPay Trip Report", 40, y, titlePaint);
                y += 30;
                page.getCanvas().drawText("Trip: " + trip.name, 40, y, bodyPaint);
                y += 20;
                page.getCanvas().drawText("Destination: " + trip.destination, 40, y, bodyPaint);
                y += 20;
                page.getCanvas().drawText("Dates: " + trip.startDate + " to " + trip.endDate, 40, y, bodyPaint);
                y += 20;
                page.getCanvas().drawText(String.format(Locale.getDefault(), "Budget: PKR %.0f", trip.budgetPKR), 40, y, bodyPaint);
                y += 20;
                page.getCanvas().drawText(String.format(Locale.getDefault(), "Spent: PKR %.0f", totalSpent), 40, y, bodyPaint);
                y += 20;
                page.getCanvas().drawText(String.format(Locale.getDefault(), "Remaining: PKR %.0f", trip.budgetPKR - totalSpent), 40, y, bodyPaint);

                y += 35;
                bodyPaint.setFakeBoldText(true);
                page.getCanvas().drawText("Category Breakdown", 40, y, bodyPaint);
                bodyPaint.setFakeBoldText(false);

                y += 20;
                if (categories.isEmpty()) {
                    page.getCanvas().drawText("No expenses found for this trip.", 40, y, bodyPaint);
                } else {
                    for (Map.Entry<String, Double> entry : categories.entrySet()) {
                        if (y > 790) break;
                        page.getCanvas().drawText(
                                String.format(Locale.getDefault(), "%s: PKR %.0f", entry.getKey(), entry.getValue()),
                                40,
                                y,
                                bodyPaint
                        );
                        y += 18;
                    }
                }

                document.finishPage(page);

                boolean saved = savePdfToDownloads(context, document, fileName);
                document.close();

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnExportPDF.setEnabled(true);
                    if (saved) {
                        Toast.makeText(requireContext(), "PDF saved to Downloads", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to save PDF", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnExportPDF.setEnabled(true);
                    Toast.makeText(requireContext(), "PDF export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean savePdfToDownloads(Context context, PdfDocument document, String fileName) {
        OutputStream outputStream = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;
                outputStream = context.getContentResolver().openOutputStream(uri);
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                    return false;
                }
                File file = new File(downloadsDir, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream == null) return false;
            document.writeTo(outputStream);
            outputStream.flush();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(38f);
        pieChart.setTransparentCircleRadius(43f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Spending");
        pieChart.animateY(1000);
    }

    private void updatePieChart(Map<String, Double> data) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (e.getValue() > 0) entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
        }
        if (entries.isEmpty()) return;
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(3f);
        pieChart.setData(new PieData(dataSet));
        pieChart.invalidate();
    }

    private void updateCategoryBars(Map<String, Double> data) {
        llCategoryBars.removeAllViews();
        viewModel.getActiveTrip().getValue();
        Trip trip = viewModel.getActiveTrip().getValue();
        if (trip == null) return;

        String[] cats = {"Food", "Transport", "Hotel", "Shopping", "Activities"};
        for (String cat : cats) {
            double spent = data.getOrDefault(cat, 0.0);
            if (spent <= 0) continue;
            int pct = (int) ((spent / trip.budgetPKR) * 100);

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView label = new TextView(getContext());
            label.setText(cat + "  PKR " + String.format(Locale.getDefault(), "%.0f", spent));
            label.setTextSize(13f);
            row.addView(label);

            ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(pct);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24);
            pb.setLayoutParams(lp);
            row.addView(pb);
            llCategoryBars.addView(row);
        }
    }
}
