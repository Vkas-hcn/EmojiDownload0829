package com.befool.others.tricks

import android.app.Application

class MyAppGlideModule : Application() {
    companion object {
        lateinit var instance: MyAppGlideModule
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}