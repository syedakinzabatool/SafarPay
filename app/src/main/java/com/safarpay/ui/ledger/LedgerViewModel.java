package com.safarpay.ui.ledger;

import android.app.Application;
import androidx.lifecycle.*;
import com.safarpay.data.local.entity.Expense;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.data.repository.ExpenseRepository;
import com.safarpay.data.repository.TripRepository;
import java.util.List;

public class LedgerViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepo;
    private final TripRepository tripRepo;
    private final LiveData<Trip> activeTrip;
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public LedgerViewModel(Application app) {
        super(app);
        expenseRepo = new ExpenseRepository(app);
        tripRepo    = new TripRepository(app);
        activeTrip  = tripRepo.getActiveTrip();
    }

    public LiveData<Trip> getActiveTrip() { return activeTrip; }
    public LiveData<Boolean> getDeleted() { return deleted; }

    public LiveData<List<Expense>> getExpenses(int tripId, String category) {
        return expenseRepo.getExpensesByCategory(tripId, category);
    }

    public LiveData<List<Expense>> searchExpenses(int tripId, String query) {
        return expenseRepo.searchExpenses(tripId, query);
    }

    public void deleteExpense(Expense e) {
        expenseRepo.deleteExpense(e, () -> deleted.postValue(true));
    }

    public void restoreExpense(Expense e) {
        expenseRepo.insertExpense(e, null);
    }
}
