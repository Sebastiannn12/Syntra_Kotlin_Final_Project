package com.example.androidappsample.network

import android.content.Context
import com.example.androidappsample.BuildConfig
import com.example.androidappsample.data.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private lateinit var session: SessionManager

    fun initialize(context: Context) {
        session = SessionManager(context.applicationContext)
    }

    val userApi: UserApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .apply {
                        session.token?.takeIf { it.isNotBlank() }
                            ?.let { header("Authorization", "Bearer $it") }
                    }
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApiService::class.java)
    }
}
