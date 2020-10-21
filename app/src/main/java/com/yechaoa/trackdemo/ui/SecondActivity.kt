package com.yechaoa.trackdemo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yechaoa.trackdemo.R
import com.yechaoa.trackdemo.track.SensorsDataAPI
import kotlinx.android.synthetic.main.activity_second.*
import org.json.JSONObject

class SecondActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        button2.setOnClickListener {
            val jsonObject = JSONObject()
            jsonObject.put("element_type", "androidx.constraintlayout.widget.ConstraintLayout")
            jsonObject.put("element_id", "自定义id")
            jsonObject.put("element_content", "自定义内容")
            jsonObject.put("id", 1234)
            jsonObject.put("activity", this.javaClass.canonicalName)
            SensorsDataAPI.getInstance().trackClick("AppClick", jsonObject)
        }
    }
}