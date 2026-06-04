package com.fyp.facesync;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    // We no longer use a static final BASE_URL string here.

    public static Retrofit getClient(String baseUrl) {
        // If the URL has changed, we clear the old retrofit instance
        if (retrofit != null && !retrofit.baseUrl().toString().equals(baseUrl)) {
            retrofit = null;
        }

        if (retrofit == null) {
            // Added the longer timeouts here to fix your "Network Error" during registration
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}