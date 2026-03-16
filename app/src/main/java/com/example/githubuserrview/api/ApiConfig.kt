package com.example.githubuserrview.api

import com.example.githubuserrview.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiConfig {
    companion object {
        fun getApiService(accessToken: String? = null): ApiService {

            //untuk mengetahui hasil respon (pd Logcat) saat melakukan request via retrofit:
            val loggingInterceptor = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            } else {
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE)
            }
            //fungsi if diatas untuk memastikan pesan log nya hanya akan tampil pada mode debug

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "GitHubUserRview")

                    if (!accessToken.isNullOrBlank()) {
                        requestBuilder.header("Authorization", "Bearer $accessToken")
                    }

                    chain.proceed(requestBuilder.build())
                }
                .addInterceptor(loggingInterceptor)
                .build()

            //untuk membuat instance retrofit:
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}
