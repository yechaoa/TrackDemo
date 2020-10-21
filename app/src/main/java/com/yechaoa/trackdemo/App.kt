package com.yechaoa.trackdemo

import android.app.Application
import com.yechaoa.trackdemo.track.SensorsDataAPI

/**
 * Created by yechao on 2020/10/21.
 * Describe :
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        //初始化埋点
        SensorsDataAPI.init(this)
    }
}