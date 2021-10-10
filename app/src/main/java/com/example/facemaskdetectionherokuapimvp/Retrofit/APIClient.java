package com.example.facemaskdetectionherokuapimvp.Retrofit;



import com.example.facemaskdetectionherokuapimvp.Utils.Constants;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIClient {
    private static Retrofit retrofit = null;
    static OkHttpClient.Builder httpClient;

    public static Retrofit getClient() {
        httpClient = new OkHttpClient.Builder();
        // 1. set Connection timeout
        httpClient.connectTimeout(1, TimeUnit.MINUTES);

        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.detectionBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();



        return retrofit;
    }
}
