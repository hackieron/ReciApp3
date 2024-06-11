package com.foodknalledge.reci_app

import android.app.Application
import com.google.firebase.FirebaseApp

class ReciApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
