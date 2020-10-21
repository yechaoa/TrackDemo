package com.yechaoa.trackdemo.track;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by yechao on 2020/9/17.
 * Describe :
 */

public class SensorsDataPrivate {
    private static java.util.List<String> mIgnoredActivities;

    private static Activity mCurrentActivity;
    private static long mBeginTime;

    static {
        mIgnoredActivities = new ArrayList<>();
    }

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss" + ".SSS", java.util.Locale.CHINA);

    public static void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        mIgnoredActivities.add(activity.getCanonicalName());
    }

    public static void removeIgnoredActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mIgnoredActivities.contains(activity.getCanonicalName())) {
            mIgnoredActivities.remove(activity.getCanonicalName());
        }
    }

    public static void mergeJSONObject(final JSONObject source, JSONObject dest) throws JSONException {
        Iterator<String> superPropertiesIterator = source.keys();
        while (superPropertiesIterator.hasNext()) {
            String key = superPropertiesIterator.next();
            Object value = source.get(key);
            if (value instanceof Date) {
                synchronized (mDateFormat) {
                    dest.put(key, mDateFormat.format((Date) value));
                }
            } else {
                dest.put(key, value);
            }
        }
    }

    @TargetApi(11)
    private static String getToolbarTitle(Activity activity) {
        try {
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                if (!TextUtils.isEmpty(actionBar.getTitle())) {
                    return actionBar.getTitle().toString();
                }
            } else {
                if (activity instanceof AppCompatActivity) {
                    AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
                    androidx.appcompat.app.ActionBar supportActionBar = appCompatActivity.getSupportActionBar();
                    if (supportActionBar != null) {
                        if (!TextUtils.isEmpty(supportActionBar.getTitle())) {
                            return supportActionBar.getTitle().toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return String 当前页面 title
     */
    @SuppressWarnings("all")
    private static String getActivityTitle(Activity activity) {
        String activityTitle = null;

        if (activity == null) {
            return null;
        }

        try {
            activityTitle = activity.getTitle().toString();

            if (Build.VERSION.SDK_INT >= 11) {
                String toolbarTitle = getToolbarTitle(activity);
                if (!TextUtils.isEmpty(toolbarTitle)) {
                    activityTitle = toolbarTitle;
                }
            }

            if (TextUtils.isEmpty(activityTitle)) {
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager != null) {
                    ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                    if (activityInfo != null) {
                        activityTitle = activityInfo.loadLabel(packageManager).toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activityTitle;
    }

    /**
     * Track 页面浏览事件
     *
     * @param activity Activity
     */
    @Keep
    private static void trackAppViewScreen(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            if (mIgnoredActivities.contains(activity.getClass().getCanonicalName())) {
                return;
            }

            JSONObject properties = new JSONObject();

            //获取页面的参数
            if (activity.toString().contains("SecondActivity")) {
                String userId = activity.getIntent().getStringExtra("userId");
                properties.put("userId", userId);
            }

            properties.put("activity", activity.getClass().getCanonicalName());
//            properties.put("$title", getActivityTitle(activity));
            SensorsDataAPI.getInstance().track("$AppViewScreen", properties, mBeginTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Activity getCurrentActivity() {
        return mCurrentActivity;
    }


    /**
     * 获取item layout的content，多个子view的情况下 拼接文本
     */
    private static String traverseViewContent(StringBuilder stringBuilder, android.view.View root) {
        try {
            if (root == null) {
                return stringBuilder.toString();
            }

            if (root instanceof ViewGroup) {
                final int childCount = ((ViewGroup) root).getChildCount();
                for (int i = 0; i < childCount; ++i) {
                    final android.view.View child = ((ViewGroup) root).getChildAt(i);

                    if (child.getVisibility() != android.view.View.VISIBLE) {
                        continue;
                    }
                    if (child instanceof ViewGroup) {
                        traverseViewContent(stringBuilder, child);
                    } else {
                        String viewText = getElementContent(child);
                        if (!TextUtils.isEmpty(viewText)) {
                            stringBuilder.append(viewText);
                        }
                    }
                }
            } else {
                stringBuilder.append(getElementContent(root));
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return stringBuilder.toString();
        }
    }

    /**
     * @param adapterView ExpandableListView
     */
    public static void trackAdapterView(AdapterView<?> adapterView, android.view.View view, int groupPosition, int childPosition) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("element_type", adapterView.getClass().getCanonicalName());
            jsonObject.put("element_id", getViewId(adapterView));
            if (childPosition > -1) {
                jsonObject.put("element_position", String.format(java.util.Locale.CHINA, "%d:%d", groupPosition, childPosition));
            } else {
                jsonObject.put("element_position", String.format(java.util.Locale.CHINA, "%d", groupPosition));
            }
            StringBuilder stringBuilder = new StringBuilder();
            String viewText = traverseViewContent(stringBuilder, view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put("element_element", viewText);
            }
            Activity activity = getActivityFromView(adapterView);
            if (activity != null) {
                jsonObject.put("activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().trackClick("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param adapterView GridView ListView
     */
    public static void trackAdapterView(AdapterView<?> adapterView, android.view.View view, int position) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("element_type", adapterView.getClass().getCanonicalName());
            jsonObject.put("element_id", getViewId(adapterView));
            jsonObject.put("element_position", String.valueOf(position));
            StringBuilder stringBuilder = new StringBuilder();
            String viewText = traverseViewContent(stringBuilder, view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put("element_element", viewText);
            }
            Activity activity = getActivityFromView(adapterView);
            if (activity != null) {
                jsonObject.put("activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().trackClick("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * View 被点击，自动埋点
     *
     * @param view View
     */
    @Keep
    public static void trackViewOnClick(android.view.View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("element_type", view.getClass().getCanonicalName());
            jsonObject.put("element_id", getViewId(view));
            jsonObject.put("element_content", getElementContent(view));

            Activity activity = getActivityFromView(view);
            if (activity != null) {
                jsonObject.put("activity", activity.getClass().getCanonicalName());
            }

            SensorsDataAPI.getInstance().trackClick("$AppClick", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Delegate view OnClickListener
     *
     * @param context Context
     * @param view    View
     */
    @TargetApi(15)
    @SuppressWarnings("all")
    protected static void delegateViewsOnClickListener(final Context context, final android.view.View view) {
        if (context == null || view == null) {
            return;
        }

        if (view instanceof AdapterView) {
            if (view instanceof android.widget.Spinner) {
                AdapterView.OnItemSelectedListener onItemSelectedListener = ((android.widget.Spinner) view).getOnItemSelectedListener();
                if (onItemSelectedListener != null && !(onItemSelectedListener instanceof WrapperAdapterViewOnItemSelectedListener)) {
                    ((android.widget.Spinner) view).setOnItemSelectedListener(new WrapperAdapterViewOnItemSelectedListener(onItemSelectedListener));
                }
            } else if (view instanceof ExpandableListView) {
                try {
                    Class viewClazz = Class.forName("android.widget.ExpandableListView");
                    //Child
                    Field mOnChildClickListenerField = viewClazz.getDeclaredField("mOnChildClickListener");
                    if (!mOnChildClickListenerField.isAccessible()) {
                        mOnChildClickListenerField.setAccessible(true);
                    }
                    ExpandableListView.OnChildClickListener onChildClickListener =
                            (ExpandableListView.OnChildClickListener) mOnChildClickListenerField.get(view);
                    if (onChildClickListener != null &&
                            !(onChildClickListener instanceof WrapperOnChildClickListener)) {
                        ((ExpandableListView) view).setOnChildClickListener(
                                new WrapperOnChildClickListener(onChildClickListener));
                    }

                    //Group
                    Field mOnGroupClickListenerField = viewClazz.getDeclaredField("mOnGroupClickListener");
                    if (!mOnGroupClickListenerField.isAccessible()) {
                        mOnGroupClickListenerField.setAccessible(true);
                    }
                    ExpandableListView.OnGroupClickListener onGroupClickListener =
                            (ExpandableListView.OnGroupClickListener) mOnGroupClickListenerField.get(view);
                    if (onGroupClickListener != null &&
                            !(onGroupClickListener instanceof WrapperOnGroupClickListener)) {
                        ((ExpandableListView) view).setOnGroupClickListener(
                                new WrapperOnGroupClickListener(onGroupClickListener));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (view instanceof android.widget.ListView || view instanceof GridView) {
                AdapterView.OnItemClickListener onItemClickListener = ((AdapterView) view).getOnItemClickListener();
                if (onItemClickListener != null && !(onItemClickListener instanceof WrapperAdapterViewOnItemClick)) {
                    ((AdapterView) view).setOnItemClickListener(new WrapperAdapterViewOnItemClick(onItemClickListener));
                }
            }
        }
//        else if (view instanceof TabLayout) {
//            final TabLayout.OnTabSelectedListener onTabSelectedListener = getOnTabSelectedListener(view);
//            if (onTabSelectedListener != null && !(onTabSelectedListener instanceof WrapperOnTabSelectedListener)) {
//                ((TabLayout) view).addOnTabSelectedListener(new WrapperOnTabSelectedListener(onTabSelectedListener));
//            }
//        }
        else {
            //获取当前 view 设置的 OnClickListener
            final android.view.View.OnClickListener listener = getOnClickListener(view);

            //判断已设置的 OnClickListener 类型，如果是自定义的 WrapperOnClickListener，说明已经被 hook 过，防止重复 hook
            if (listener != null && !(listener instanceof WrapperOnClickListener)) {
                //替换成自定义的 WrapperOnClickListener
                view.setOnClickListener(new WrapperOnClickListener(listener));
            } else if (view instanceof CompoundButton) {
                final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = getOnCheckedChangeListener(view);
                if (onCheckedChangeListener != null &&
                        !(onCheckedChangeListener instanceof WrapperOnCheckedChangeListener)) {
                    ((CompoundButton) view).setOnCheckedChangeListener(
                            new WrapperOnCheckedChangeListener(onCheckedChangeListener));
                }
            } else if (view instanceof RadioGroup) {
                final RadioGroup.OnCheckedChangeListener radioOnCheckedChangeListener =
                        getRadioGroupOnCheckedChangeListener(view);
                if (radioOnCheckedChangeListener != null &&
                        !(radioOnCheckedChangeListener instanceof WrapperRadioGroupOnCheckedChangeListener)) {
                    ((RadioGroup) view).setOnCheckedChangeListener(
                            new WrapperRadioGroupOnCheckedChangeListener(radioOnCheckedChangeListener));
                }
            } else if (view instanceof RatingBar) {
                final RatingBar.OnRatingBarChangeListener onRatingBarChangeListener =
                        ((RatingBar) view).getOnRatingBarChangeListener();
                if (onRatingBarChangeListener != null &&
                        !(onRatingBarChangeListener instanceof WrapperOnRatingBarChangeListener)) {
                    ((RatingBar) view).setOnRatingBarChangeListener(
                            new WrapperOnRatingBarChangeListener(onRatingBarChangeListener));
                }
            } else if (view instanceof android.widget.SeekBar) {
                final android.widget.SeekBar.OnSeekBarChangeListener onSeekBarChangeListener =
                        getOnSeekBarChangeListener(view);
                if (onSeekBarChangeListener != null &&
                        !(onSeekBarChangeListener instanceof WrapperOnSeekBarChangeListener)) {
                    ((android.widget.SeekBar) view).setOnSeekBarChangeListener(
                            new WrapperOnSeekBarChangeListener(onSeekBarChangeListener));
                }
            }
        }

        //如果 view 是 ViewGroup，需要递归遍历子 View 并 hook
        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    android.view.View childView = viewGroup.getChildAt(i);
                    //递归
                    delegateViewsOnClickListener(context, childView);
                }
            }
        }
    }

    @SuppressWarnings("all")
    private static TabLayout.OnTabSelectedListener getOnTabSelectedListener(android.view.View view) {
        try {
            Class viewClazz = Class.forName("com.google.android.material.tabs.TabLayout");
            Field mOnTabSelectedListenerField = viewClazz.getDeclaredField("currentVpSelectedListener");
            if (!mOnTabSelectedListenerField.isAccessible()) {
                mOnTabSelectedListenerField.setAccessible(true);
            }
            return (TabLayout.OnTabSelectedListener) mOnTabSelectedListenerField.get(view);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    @SuppressWarnings("all")
    private static android.widget.SeekBar.OnSeekBarChangeListener getOnSeekBarChangeListener(android.view.View view) {
        try {
            Class viewClazz = Class.forName("android.widget.SeekBar");
            Field mOnCheckedChangeListenerField = viewClazz.getDeclaredField("mOnSeekBarChangeListener");
            if (!mOnCheckedChangeListenerField.isAccessible()) {
                mOnCheckedChangeListenerField.setAccessible(true);
            }
            return (android.widget.SeekBar.OnSeekBarChangeListener) mOnCheckedChangeListenerField.get(view);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("all")
    private static RadioGroup.OnCheckedChangeListener getRadioGroupOnCheckedChangeListener(android.view.View view) {
        try {
            Class viewClazz = Class.forName("android.widget.RadioGroup");
            Field mOnCheckedChangeListenerField = viewClazz.getDeclaredField("mOnCheckedChangeListener");
            if (!mOnCheckedChangeListenerField.isAccessible()) {
                mOnCheckedChangeListenerField.setAccessible(true);
            }
            return (RadioGroup.OnCheckedChangeListener) mOnCheckedChangeListenerField.get(view);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 CheckBox 设置的 OnCheckedChangeListener
     *
     * @param view
     * @return
     */
    @SuppressWarnings("all")
    private static CompoundButton.OnCheckedChangeListener getOnCheckedChangeListener(android.view.View view) {
        try {
            Class viewClazz = Class.forName("android.widget.CompoundButton");
            Field mOnCheckedChangeListenerField = viewClazz.getDeclaredField("mOnCheckedChangeListener");
            if (!mOnCheckedChangeListenerField.isAccessible()) {
                mOnCheckedChangeListenerField.setAccessible(true);
            }
            return (CompoundButton.OnCheckedChangeListener) mOnCheckedChangeListenerField.get(view);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 View 当前设置的 OnClickListener
     *
     * @param view View
     * @return View.OnClickListener
     */
    @SuppressWarnings({"all"})
    @TargetApi(15)
    private static android.view.View.OnClickListener getOnClickListener(android.view.View view) {
        boolean hasOnClick = view.hasOnClickListeners();
        if (hasOnClick) {
            try {
                Class viewClazz = Class.forName("android.view.View");
                Method listenerInfoMethod = viewClazz.getDeclaredMethod("getListenerInfo");
                if (!listenerInfoMethod.isAccessible()) {
                    listenerInfoMethod.setAccessible(true);
                }
                Object listenerInfoObj = listenerInfoMethod.invoke(view);
                Class listenerInfoClazz = Class.forName("android.view.View$ListenerInfo");
                Field onClickListenerField = listenerInfoClazz.getDeclaredField("mOnClickListener");
                if (!onClickListenerField.isAccessible()) {
                    onClickListenerField.setAccessible(true);
                }
                return (android.view.View.OnClickListener) onClickListenerField.get(listenerInfoObj);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获取 view 的 android:id 对应的字符串
     *
     * @param view View
     * @return String
     */
    private static String getViewId(android.view.View view) {
        String idString = null;
        try {
            if (view.getId() != android.view.View.NO_ID) {
                idString = view.getContext().getResources().getResourceEntryName(view.getId());
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

    /**
     * 获取 View 上显示的文本
     *
     * @param view View
     * @return String
     */
    private static String getElementContent(android.view.View view) {
        if (view == null) {
            return null;
        }

        String text = null;
        if (view instanceof Button) {
            text = ((Button) view).getText().toString();
        } else if (view instanceof ActionMenuItemView) {
            text = ((ActionMenuItemView) view).getText().toString();
        } else if (view instanceof android.widget.TextView) {
            text = ((android.widget.TextView) view).getText().toString();
        } else if (view instanceof ImageView) {
            if (null == view.getContentDescription()) {
                text = "";
            } else {
                text = view.getContentDescription().toString();
            }
        } else if (view instanceof RadioGroup) {
            try {
                RadioGroup radioGroup = (RadioGroup) view;
                Activity activity = getActivityFromView(view);
                if (activity != null) {
                    int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                    RadioButton radioButton = activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        text = radioButton.getText().toString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (view instanceof RatingBar) {
            text = String.valueOf(((RatingBar) view).getRating());
        } else if (view instanceof android.widget.SeekBar) {
            text = String.valueOf(((android.widget.SeekBar) view).getProgress());
        } else if (view instanceof ViewGroup) {
            text = traverseViewContent(new StringBuilder(), view);
        }
        return text;
    }

    /**
     * 获取 View 所属 Activity
     *
     * @param view View
     * @return Activity
     */
    private static Activity getActivityFromView(android.view.View view) {
        Activity activity = null;
        if (view == null) {
            return null;
        }

        try {
            Context context = view.getContext();
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activity;
    }


    @SuppressWarnings("SameParameterValue")
    private static ViewGroup getRootViewFromActivity(Activity activity, boolean decorView) {
        if (decorView) {
            return (ViewGroup) activity.getWindow().getDecorView();
        } else {
            return activity.findViewById(android.R.id.content);
        }
    }

    /**
     * 注册 Application.ActivityLifecycleCallbacks
     *
     * @param application Application
     */
    @TargetApi(14)
    public static void registerActivityLifecycleCallbacks(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

            @Override
            public void onActivityCreated(final Activity activity, android.os.Bundle bundle) {
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        delegateViewsOnClickListener(activity, rootView);
                    }
                };
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                mBeginTime = System.currentTimeMillis();
                mCurrentActivity = activity;
                //trackAppViewScreen(activity);

                //添加视图树监听器
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                trackAppViewScreen(activity);
            }

            @Override
            public void onActivityStopped(Activity activity) {
                //移除
                final ViewGroup rootView = getRootViewFromActivity(activity, true);
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, android.os.Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public static java.util.Map<String, Object> getDeviceInfo(Context context) {
        final java.util.Map<String, Object> deviceInfo = new HashMap<>();
        {
//            deviceInfo.put("$lib", "Android");
//            deviceInfo.put("$lib_version", SensorsDataAPI.SDK_VERSION);
//            deviceInfo.put("$os", "Android");
            deviceInfo.put("os_version", Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            deviceInfo.put("manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
            if (TextUtils.isEmpty(Build.MODEL)) {
                deviceInfo.put("model", "UNKNOWN");
            } else {
                deviceInfo.put("model", Build.MODEL.trim());
            }

            try {
                final PackageManager manager = context.getPackageManager();
                final PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
                deviceInfo.put("app_version", packageInfo.versionName);

                int labelRes = packageInfo.applicationInfo.labelRes;
                deviceInfo.put("app_name", context.getResources().getString(labelRes));
            } catch (final Exception e) {
                e.printStackTrace();
            }

            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            deviceInfo.put("screen_height", displayMetrics.heightPixels);
            deviceInfo.put("screen_width", displayMetrics.widthPixels);

            return Collections.unmodifiableMap(deviceInfo);
        }
    }

    /**
     * 获取 Android ID
     *
     * @param mContext Context
     * @return String
     */
    @SuppressLint("HardwareIds")
    public static String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return androidID;
    }

    private static void addIndentBlank(StringBuilder sb, int indent) {
        try {
            for (int i = 0; i < indent; i++) {
                sb.append('\t');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String formatJson(String jsonStr) {
        try {
            if (null == jsonStr || "".equals(jsonStr)) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            char last;
            char current = '\0';
            int indent = 0;
            boolean isInQuotationMarks = false;
            for (int i = 0; i < jsonStr.length(); i++) {
                last = current;
                current = jsonStr.charAt(i);
                switch (current) {
                    case '"':
                        if (last != '\\') {
                            isInQuotationMarks = !isInQuotationMarks;
                        }
                        sb.append(current);
                        break;
                    case '{':
                    case '[':
                        sb.append(current);
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent++;
                            addIndentBlank(sb, indent);
                        }
                        break;
                    case '}':
                    case ']':
                        if (!isInQuotationMarks) {
                            sb.append('\n');
                            indent--;
                            addIndentBlank(sb, indent);
                        }
                        sb.append(current);
                        break;
                    case ',':
                        sb.append(current);
                        if (last != '\\' && !isInQuotationMarks) {
                            sb.append('\n');
                            addIndentBlank(sb, indent);
                        }
                        break;
                    default:
                        sb.append(current);
                }
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}