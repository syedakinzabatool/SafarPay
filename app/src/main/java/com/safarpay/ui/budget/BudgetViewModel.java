package com.safarpay.ui.budget;

import android.app.Application;
import androidx.lifecycle.*;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.data.repository.ExpenseRepository;
import com.safarpay.data.repository.TripRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class BudgetViewModel extends AndroidViewModel {

    private final TripRepository tripRepo;
    private final ExpenseRepository expenseRepo;
    private final LiveData<Trip> activeTrip;
    private final MutableLiveData<Map<String, Double>> categorySpending = new MutableLiveData<>();

    public BudgetViewModel(Application app) {
        super(app);
        tripRepo    = new TripRepository(app);
        expenseRepo = new ExpenseRepository(app);
        activeTrip  = tripRepo.getActiveTrip();
    }

    public LiveData<Trip> getActiveTrip() { return activeTrip; }
    public LiveData<Double> getTotalSpent(int tripId) { return expenseRepo.getTotalSpentPKR(tripId); }
    public MutableLiveData<Map<String, Double>> getCategorySpending() { return categorySpending; }

    public void loadCategorySpending(int tripId) {
        String[] cats = {"Food", "Transport", "Hotel", "Shopping", "Activities"};
        Executors.newSingleThreadExecutor().execute(() -> {
            Map<String, Double> map = new HashMap<>();
            for (String c : cats) map.put(c, expenseRepo.getCategorySpent(tripId, c));
            categorySpending.postValue(map);
        });
    }
}
