package com.safarpay.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.safarpay.data.local.AppDatabase;
import com.safarpay.data.local.dao.ExpenseDao;
import com.safarpay.data.local.entity.Expense;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseRepository {
    private final ExpenseDao expenseDao;
    private final ExecutorService executor;

    public ExpenseRepository(Application app) {
        expenseDao = AppDatabase.getInstance(app).expenseDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Expense>> getExpensesForTrip(int tripId) {
        return expenseDao.getExpensesForTrip(tripId);
    }

    public LiveData<Double> getTotalSpentPKR(int tripId) {
        return expenseDao.getTotalSpentPKR(tripId);
    }

    public LiveData<List<Expense>> getExpensesByCategory(int tripId, String category) {
        return expenseDao.getExpensesByCategory(tripId, category);
    }

    public LiveData<List<Expense>> searchExpenses(int tripId, String query) {
        return expenseDao.searchExpenses(tripId, query);
    }

    public void insertExpense(Expense expense, Runnable onComplete) {
        executor.execute(() -> {
            expenseDao.insert(expense);
            if (onComplete != null) onComplete.run();
        });
    }

    public void updateExpense(Expense expense, Runnable onComplete) {
        executor.execute(() -> {
            expenseDao.update(expense);
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteExpense(Expense expense, Runnable onComplete) {
        executor.execute(() -> {
            expenseDao.delete(expense);
            if (onComplete != null) onComplete.run();
        });
    }

    public void getExpenseById(int id, java.util.function.Consumer<Expense> callback) {
        executor.execute(() -> callback.accept(expenseDao.getExpenseById(id)));
    }

    public double getCategorySpent(int tripId, String category) {
        return expenseDao.getCategorySpent(tripId, category);
    }

    public List<Expense> getExpensesForTripSync(int tripId) {
        return expenseDao.getExpensesForTripSync(tripId);
    }
}
