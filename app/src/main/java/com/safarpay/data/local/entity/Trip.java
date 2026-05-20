package com.safarpay.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips")
public class Trip {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String destination;
    public String localCurrency;
    public String startDate;
    public String endDate;
    public double budgetPKR;
    public int isActive; // 0 = inactive, 1 = active
    public String userId;

    public Trip() {}

    @Ignore
    public Trip(String name, String destination, String localCurrency,
                String startDate, String endDate, double budgetPKR, String userId) {
        this.name = name;
        this.destination = destination;
        this.localCurrency = localCurrency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budgetPKR = budgetPKR;
        this.userId = userId;
        this.isActive = 0;
    }
}
