package com.safarpay.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.safarpay.data.local.AppDatabase;
import com.safarpay.data.local.dao.TripDao;
import com.safarpay.data.local.entity.Trip;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripRepository {
    private final TripDao tripDao;
    private final FirebaseFirestore db;
    private final ExecutorService executor;

    public TripRepository(Application app) {
        AppDatabase database = AppDatabase.getInstance(app);
        tripDao = database.tripDao();
        db = FirebaseFirestore.getInstance();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<Trip> getActiveTrip()       { return tripDao.getActiveTrip(); }
    public LiveData<List<Trip>> getAllTrips()    { return tripDao.getAllTrips(); }

    /** Async check: calls callback with true if overlapping trip found */
    public void checkOverlap(String startDate, String endDate, OverlapCallback callback) {
        executor.execute(() -> {
            int count = tripDao.countOverlappingTrips(startDate, endDate);
            callback.onResult(count > 0);
        });
    }

    public interface OverlapCallback { void onResult(boolean hasOverlap); }

    public void insertTrip(Trip trip, Runnable onComplete) {
        executor.execute(() -> {
            tripDao.deactivateAllTrips();
            trip.isActive = 1;
            long id = tripDao.insert(trip);
            trip.id = (int) id;
            syncTripToFirestore(trip);
            if (onComplete != null) onComplete.run();
        });
    }

    public void updateTrip(Trip trip, Runnable onComplete) {
        executor.execute(() -> {
            tripDao.update(trip);
            syncTripToFirestore(trip);
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteTrip(Trip trip) { executor.execute(() -> tripDao.delete(trip)); }

    public void setActiveTrip(int tripId) {
        executor.execute(() -> {
            tripDao.deactivateAllTrips();
            tripDao.activateTrip(tripId);
        });
    }

    private void syncTripToFirestore(Trip trip) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid); data.put("name", trip.name);
        data.put("destination", trip.destination); data.put("localCurrency", trip.localCurrency);
        data.put("startDate", trip.startDate); data.put("endDate", trip.endDate);
        data.put("budgetPKR", trip.budgetPKR); data.put("isActive", trip.isActive);
        db.collection("trips").document(uid + "_" + trip.id).set(data);
    }
}
