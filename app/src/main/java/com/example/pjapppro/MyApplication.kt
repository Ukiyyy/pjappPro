package com.example.pjapppro

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {

    var statisticsList = mutableListOf<statistics>()
    var markerList = mutableListOf<markerji>()

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}
