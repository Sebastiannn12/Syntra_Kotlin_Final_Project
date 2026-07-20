package com.example.androidappsample

import android.app.Application
import com.example.androidappsample.network.RetrofitClient

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
    }
}
