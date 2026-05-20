package com.safarpay.ui.converter;

import android.app.Application;
import androidx.lifecycle.*;
import com.safarpay.data.repository.ExchangeRateRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConverterViewModel extends AndroidViewModel {

    private final ExchangeRateRepository repo;
    private final MutableLiveData<String> result = new MutableLiveData<>("");
    private final MutableLiveData<String> rateInfo = new MutableLiveData<>("");
    private double currentRate = 1.0;
    private boolean swapped = false;

    public ConverterViewModel(Application app) {
        super(app);
        repo = new ExchangeRateRepository(app);
    }

    public LiveData<String> getResult()   { return result; }
    public LiveData<String> getRateInfo() { return rateInfo; }

    public void fetchAndConvert(String fromCurrency, String toCurrency, String amountStr) {
        repo.getRate(fromCurrency, toCurrency, new ExchangeRateRepository.RateCallback() {
            @Override public void onSuccess(double rate, long lastUpdated) {
                currentRate = rate;
                convert(amountStr, rate, lastUpdated, false);
            }
            @Override public void onError(String msg) {
                rateInfo.postValue("⚠ " + msg);
            }
        });
    }

    private void convert(String amountStr, double rate, long lastUpdated, boolean cached) {
        try {
            double amount = Double.parseDouble(amountStr);
            double converted = amount * rate;
            result.postValue(String.format(Locale.getDefault(), "%.4f", converted));
            String time = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                .format(new Date(lastUpdated));
            rateInfo.postValue((cached ? "⚠ Cached: " : "Rate updated ") + time);
        } catch (NumberFormatException e) {
            result.postValue("");
        }
    }
}
