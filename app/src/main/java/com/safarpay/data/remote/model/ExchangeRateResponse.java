package com.safarpay.data.remote.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ExchangeRateResponse {
    @SerializedName("result")
    public String result;

    @SerializedName("base_code")
    public String baseCode;

    @SerializedName("conversion_rates")
    public Map<String, Double> conversionRates;
}
