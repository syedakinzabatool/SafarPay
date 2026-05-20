package com.safarpay.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "exchange_rates")
public class ExchangeRate {
    @PrimaryKey
    @NonNull
    public String currencyCode;
    public double rateFromPKR; // how many of this currency = 1 PKR
    public long lastUpdated;   // Unix timestamp

    public ExchangeRate(@NonNull String currencyCode, double rateFromPKR, long lastUpdated) {
        this.currencyCode = currencyCode;
        this.rateFromPKR = rateFromPKR;
        this.lastUpdated = lastUpdated;
    }
}
