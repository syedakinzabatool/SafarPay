package com.safarpay.ui.trip;

import android.app.Application;
import androidx.lifecycle.*;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.data.repository.TripRepository;

public class TripViewModel extends AndroidViewModel {
    private final TripRepository repo;
    private final MutableLiveData<Boolean> saved        = new MutableLiveData<>();
    private final MutableLiveData<Boolean> overlapFound = new MutableLiveData<>();

    public TripViewModel(Application app) { super(app); repo = new TripRepository(app); }

    public LiveData<Boolean> getSaved()        { return saved; }
    public LiveData<Boolean> getOverlapFound() { return overlapFound; }
    public LiveData<java.util.List<Trip>> getAllTrips() { return repo.getAllTrips(); }

    public void checkOverlapThenSave(Trip trip) {
        repo.checkOverlap(trip.startDate, trip.endDate, hasOverlap -> {
            if (hasOverlap) {
                overlapFound.postValue(true);
            } else {
                doSave(trip);
            }
        });
    }

    /** Force save even if overlap (user confirmed) */
    public void saveTrip(Trip trip) { doSave(trip); }

    private void doSave(Trip trip) {
        if (trip.id == 0) {
            repo.insertTrip(trip, () -> saved.postValue(true));
        } else {
            repo.updateTrip(trip, () -> saved.postValue(true));
        }
    }
}