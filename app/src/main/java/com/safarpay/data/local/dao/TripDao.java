package com.safarpay.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.safarpay.data.local.entity.Trip;
import java.util.List;

@Dao
public interface TripDao {
    @Insert long insert(Trip trip);
    @Update void update(Trip trip);
    @Delete void delete(Trip trip);

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    LiveData<Trip> getActiveTrip();

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    Trip getActiveTripSync();

    @Query("SELECT * FROM trips ORDER BY id DESC")
    LiveData<List<Trip>> getAllTrips();

    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<Trip> getAllTripsSync();

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    Trip getTripById(int id);

    @Query("UPDATE trips SET isActive = 0 WHERE isActive = 1")
    void deactivateAllTrips();

    @Query("UPDATE trips SET isActive = 1 WHERE id = :id")
    void activateTrip(int id);

    @Query("SELECT COUNT(*) FROM trips WHERE (startDate <= :endDate AND endDate >= :startDate)")
    int countOverlappingTrips(String startDate, String endDate);
}
