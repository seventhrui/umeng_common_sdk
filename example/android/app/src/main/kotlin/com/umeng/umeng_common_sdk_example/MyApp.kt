package com.umeng.umeng_common_sdk_example

import android.app.Application
import android.util.Log
import com.umeng.commonsdk.UMConfigure

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("UMENG", "--->>> FlutterApplication: onCreate enter")
        UMConfigure.setLogEnabled(true)
        UMConfigure.preInit(this, "5e3f96f3cb23d2a070000048", "Umeng")
    }
}