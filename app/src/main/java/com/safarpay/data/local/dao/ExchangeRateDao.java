package com.safarpay.data.local.dao;

import androidx.room.*;
import com.safarpay.data.local.entity.ExchangeRate;
import java.util.List;

@Dao
public interface ExchangeRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(ExchangeRate rate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ExchangeRate> rates);

    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code LIMIT 1")
    ExchangeRate getRateByCode(String code);

    @Query("SELECT * FROM exchange_rates")
    List<ExchangeRate> getAllRates();
}
