package com.safarpay.ui.expense;

import android.app.Application;
import androidx.lifecycle.*;
import com.safarpay.data.local.entity.Expense;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.data.repository.ExchangeRateRepository;
import com.safarpay.data.repository.ExpenseRepository;
import com.safarpay.data.repository.TripRepository;

public class ExpenseViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepo;
    private final TripRepository tripRepo;
    private final ExchangeRateRepository rateRepo;
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>();
    private final MutableLiveData<String> conversionDisplay = new MutableLiveData<>();
    private double currentRate = 1.0;

    public ExpenseViewModel(Application app) {
        super(app);
        expenseRepo = new ExpenseRepository(app);
        tripRepo    = new TripRepository(app);
        rateRepo    = new ExchangeRateRepository(app);
    }

    public LiveData<Trip> getActiveTrip()          { return tripRepo.getActiveTrip(); }
    public LiveData<Boolean> getSaved()             { return saved; }
    public LiveData<String> getConversionDisplay() { return conversionDisplay; }
    public double getCurrentRate()                  { return currentRate; }

    public void fetchRate(String currency) {
        rateRepo.getRate("PKR", currency, new ExchangeRateRepository.RateCallback() {
            @Override public void onSuccess(double rate, long lastUpdated) {
                currentRate = rate;
            }
            @Override public void onError(String msg) { currentRate = 1.0; }
        });
    }

    public void updateConversion(String amountStr, String currency) {
        try {
            double amount = Double.parseDouble(amountStr);
            double pkr = currentRate > 0 ? amount / currentRate : amount;
            conversionDisplay.postValue(String.format("≈ PKR %.2f", pkr));
        } catch (NumberFormatException e) {
            conversionDisplay.postValue("");
        }
    }

    public void saveExpense(Expense expense) {
        if (expense.id == 0) {
            expenseRepo.insertExpense(expense, () -> saved.postValue(true));
        } else {
            expenseRepo.updateExpense(expense, () -> saved.postValue(true));
        }
    }

    public void loadExpense(int id, java.util.function.Consumer<Expense> cb) {
        expenseRepo.getExpenseById(id, cb);
    }
}
