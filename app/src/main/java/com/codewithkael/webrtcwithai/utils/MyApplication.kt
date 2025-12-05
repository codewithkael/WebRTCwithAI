package com.codewithkael.webrtcwithai.utils

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.util.UUID

@HiltAndroidApp
class MyApplication : Application() {
    companion object {
        val TAG: String = "MyApplication"
        var UserID: String = UUID.randomUUID().toString().substring(0, 2)
    }
}