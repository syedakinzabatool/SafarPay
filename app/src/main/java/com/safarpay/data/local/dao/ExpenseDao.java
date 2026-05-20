package com.safarpay.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.safarpay.data.local.entity.Expense;
import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert long insert(Expense expense);
    @Update void update(Expense expense);
    @Delete void delete(Expense expense);

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesForTrip(int tripId);

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    List<Expense> getExpensesForTripSync(int tripId);

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    List<Expense> getAllExpensesSync();

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    Expense getExpenseById(int id);

    @Query("SELECT SUM(amountPKR) FROM expenses WHERE tripId = :tripId")
    LiveData<Double> getTotalSpentPKR(int tripId);

    @Query("SELECT SUM(amountPKR) FROM expenses WHERE tripId = :tripId AND category = :category")
    double getCategorySpent(int tripId, String category);

    @Query("SELECT * FROM expenses WHERE tripId = :tripId AND (:category = 'All' OR category = :category) ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByCategory(int tripId, String category);

    @Query("SELECT * FROM expenses WHERE tripId = :tripId AND note LIKE '%' || :query || '%'")
    LiveData<List<Expense>> searchExpenses(int tripId, String query);
}
