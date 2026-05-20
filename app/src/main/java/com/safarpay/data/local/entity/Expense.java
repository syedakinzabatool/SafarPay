package com.safarpay.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses",
    foreignKeys = @ForeignKey(entity = Trip.class,
        parentColumns = "id",
        childColumns = "tripId",
        onDelete = ForeignKey.CASCADE),
    indices = @Index(value = "tripId"))
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int tripId;
    public double amount;
    public double amountPKR;
    public String currency;
    public String category; // Food | Transport | Hotel | Shopping | Activities
    public String note;
    public String date;

    public Expense() {}

    @Ignore
    public Expense(int tripId, double amount, double amountPKR, String currency,
                   String category, String note, String date) {
        this.tripId = tripId;
        this.amount = amount;
        this.amountPKR = amountPKR;
        this.currency = currency;
        this.category = category;
        this.note = note;
        this.date = date;
    }
}
