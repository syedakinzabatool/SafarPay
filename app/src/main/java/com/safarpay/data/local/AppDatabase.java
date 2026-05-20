package com.safarpay.data.local;

import android.content.Context;
import android.util.Log;
import java.io.File;
import androidx.room.*;
import com.safarpay.data.local.dao.*;
import com.safarpay.data.local.entity.*;

@Database(entities = {Trip.class, Expense.class, ExchangeRate.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TripDao tripDao();
    public abstract ExpenseDao expenseDao();
    public abstract ExchangeRateDao exchangeRateDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                "safarpay_db")
                            .fallbackToDestructiveMigration()
                            .build();
                    } catch (IllegalStateException ise) {
                        // Room failed to open DB due to schema identity mismatch. Delete DB file and retry.
                        Log.w("AppDatabase", "Room open failed, attempting to delete corrupt DB and recreate", ise);
                        try {
                            File dbFile = context.getApplicationContext().getDatabasePath("safarpay_db");
                            if (dbFile.exists()) {
                                boolean deleted = dbFile.delete();
                                Log.i("AppDatabase", "Deleted safarpay_db: " + deleted);
                            }
                        } catch (Exception e) {
                            Log.e("AppDatabase", "Could not delete database file", e);
                        }
                        // Retry build
                        INSTANCE = Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                "safarpay_db")
                            .fallbackToDestructiveMigration()
                            .build();
                    }
                }
            }
        }
        return INSTANCE;
    }
}
