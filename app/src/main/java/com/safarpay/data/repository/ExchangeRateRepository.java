package com.safarpay.data.repository;

import android.app.Application;
import com.safarpay.data.local.AppDatabase;
import com.safarpay.data.local.dao.ExchangeRateDao;
import com.safarpay.data.local.entity.ExchangeRate;
import com.safarpay.data.remote.RetrofitClient;
import com.safarpay.data.remote.model.ExchangeRateResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExchangeRateRepository {
    private final ExchangeRateDao dao;
    private final ExecutorService executor;

    public ExchangeRateRepository(Application app) {
        dao = AppDatabase.getInstance(app).exchangeRateDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public interface RateCallback {
        void onSuccess(double rate, long lastUpdated);
        void onError(String msg);
    }

    /** Get PKR→foreign rate. Tries network first, falls back to cache. */
    public void getRate(String fromCurrency, String toCurrency, RateCallback cb) {
        RetrofitClient.getInstance().getApiService()
            .getLatestRates(RetrofitClient.API_KEY, "PKR")
            .enqueue(new Callback<ExchangeRateResponse>() {
                @Override
                public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Double> rates = response.body().conversionRates;
                        long now = System.currentTimeMillis();
                        List<ExchangeRate> list = new ArrayList<>();
                        for (Map.Entry<String, Double> e : rates.entrySet()) {
                            list.add(new ExchangeRate(e.getKey(), e.getValue(), now));
                        }
                        executor.execute(() -> dao.insertAll(list));
                        double fromRate = rates.getOrDefault(fromCurrency, 1.0);
                        double toRate   = rates.getOrDefault(toCurrency, 1.0);
                        double rate = (fromCurrency.equals("PKR")) ? toRate : fromRate;
                        cb.onSuccess(rate, now);
                    } else {
                        fallbackToCache(toCurrency, cb);
                    }
                }
                @Override
                public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                    fallbackToCache(toCurrency, cb);
                }
            });
    }

    private void fallbackToCache(String currency, RateCallback cb) {
        executor.execute(() -> {
            ExchangeRate cached = dao.getRateByCode(currency);
            if (cached != null) {
                cb.onSuccess(cached.rateFromPKR, cached.lastUpdated);
            } else {
                cb.onError("No cached rate available for " + currency);
            }
        });
    }

    public void getAllCachedRates(java.util.function.Consumer<List<ExchangeRate>> cb) {
        executor.execute(() -> cb.accept(dao.getAllRates()));
    }
}
