package com.yechaoa.trackdemo.track;

import android.app.Application;
import android.util.Log;

import androidx.annotation.Keep;

import com.yechaoa.trackdemo.ui.SecondActivity;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by yechao on 2020/9/17.
 * Describe :
 */

@Keep
public class SensorsDataAPI {

    private final String TAG = this.getClass().getSimpleName();
    public static final String SDK_VERSION = "1.0.0";
    private static SensorsDataAPI INSTANCE;
    private static final Object mLock = new Object();
    private static java.util.Map<String, Object> mDeviceInfo;
    private String mDeviceId;

    @Keep
    @SuppressWarnings("UnusedReturnValue")
    public static SensorsDataAPI init(Application application) {
        synchronized (mLock) {
            if (null == INSTANCE) {
                INSTANCE = new SensorsDataAPI(application);
            }
            return INSTANCE;
        }
    }

    @Keep
    public static SensorsDataAPI getInstance() {
        return INSTANCE;
    }

    private SensorsDataAPI(Application application) {
        mDeviceId = SensorsDataPrivate.getAndroidID(application.getApplicationContext());
        mDeviceInfo = SensorsDataPrivate.getDeviceInfo(application.getApplicationContext());
        SensorsDataPrivate.registerActivityLifecycleCallbacks(application);
    }

    /**
     * 指定不采集哪个 Activity 的页面浏览事件
     *
     * @param activity Activity
     */
    public void ignoreAutoTrackActivity(Class<?> activity) {
        SensorsDataPrivate.ignoreAutoTrackActivity(activity);
    }

    /**
     * 恢复采集某个 Activity 的页面浏览事件
     *
     * @param activity Activity
     */
    public void removeIgnoredActivity(Class<?> activity) {
        SensorsDataPrivate.removeIgnoredActivity(activity);
    }

    /**
     * track 事件 （页面）
     *
     * @param eventName  String 事件名称
     * @param properties JSONObject 事件自定义属性
     */
    public void track(@androidx.annotation.NonNull String eventName, @androidx.annotation.Nullable JSONObject properties, long beginTime) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("event", eventName);
//            jsonObject.put("device_id", mDeviceId);

            JSONObject sendProperties = new JSONObject(mDeviceInfo);

            if (properties != null) {
                SensorsDataPrivate.mergeJSONObject(properties, sendProperties);
            }

            jsonObject.put("extras", sendProperties);
            jsonObject.put("beginTime", beginTime);
            jsonObject.put("endTime", System.currentTimeMillis());
            jsonObject.put("pageId", SensorsDataPrivate.getCurrentActivity().getClass().getCanonicalName());
            jsonObject.put("sessionId", UUID.randomUUID().toString().replace("-", ""));

            Log.i(TAG, SensorsDataPrivate.formatJson(jsonObject.toString()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * track 点击事件
     *
     * @param eventName  String 事件名称
     * @param properties JSONObject 事件自定义属性
     */
    public void trackClick(@androidx.annotation.NonNull String eventName, @androidx.annotation.Nullable JSONObject properties) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("event", eventName);
//            jsonObject.put("device_id", mDeviceId);

            JSONObject sendProperties = new JSONObject(mDeviceInfo);


            String act = properties.get("activity").toString();
            //获取页面的参数
            if (act.contains("SecondActivity")) {
                SecondActivity activity = (SecondActivity) SensorsDataPrivate.getCurrentActivity();
                String userId = activity.getIntent().getStringExtra("userId");
                properties.put("userId", userId);
            }

            if (properties != null) {
                SensorsDataPrivate.mergeJSONObject(properties, sendProperties);
            }

            jsonObject.put("extras", sendProperties);
            jsonObject.put("eventTime", System.currentTimeMillis());
            jsonObject.put("sessionId", UUID.randomUUID().toString().replace("-", ""));

            Log.i(TAG, SensorsDataPrivate.formatJson(jsonObject.toString()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}