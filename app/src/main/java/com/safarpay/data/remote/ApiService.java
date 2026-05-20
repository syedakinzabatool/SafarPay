package com.safarpay.data.remote;

import com.safarpay.data.remote.model.ExchangeRateResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    // ExchangeRate-API: GET /v6/{api_key}/latest/{base}
    // Replace YOUR_API_KEY with your actual key from exchangerate-api.com (free tier available)
    @GET("v6/{apiKey}/latest/{base}")
    Call<ExchangeRateResponse> getLatestRates(
        @Path("apiKey") String apiKey,
        @Path("base") String baseCurrency
    );
}
