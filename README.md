# TrackDemo Android 全埋点

> 本文参考《Android全埋点解决方案》一书，并在实操中查漏补缺。

# 前言
为什么选择`全埋点`呢，因为传统的`手动埋点`虽然简单、扩展性强，但弊端也很明显：
- 开发时间成本较高
- 改动的时间成本也较高
- 容易出现漏埋、埋错的情况
- 代码侵入性强

但也不是说全埋点就一定没有弊端，比如扩展性较差。
经过调研，实际上都是以`全埋点为主`、`手动埋点为辅`的情况，从而达到比较理想的埋点效果。

> 本文内容可能稍微有点长，但是很简单，别太长不看啊。

# 页面
一般来说我们需要的数据就是，用户在哪个页面干了什么，也就是页面和事件，现在来说页面。

### 原理
通过`生命周期`可以计算出时长数据，以及页面对象。

### Activity
页面有两个核心的需求数据：
- 浏览时长
- 页面唯一标示

这两个数据都挺好拿的，`Application`有一个`registerActivityLifecycleCallbacks`接口可以监测到activity的`生命周期`。

有了生命周期，我们在`onActivityResumed`里面记录一下开始时间，然后在`onActivityPaused`中获取当前时间，就是整个页面的`浏览时间`；

在生命周期方法中是有`activity对象`的，这样也可以拿到`全路径`作为唯一标示；

示例：

```
public static void registerActivityLifecycleCallbacks(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                mBeginTime = System.currentTimeMillis();
            }

            @Override
            public void onActivityPaused(Activity activity) {
                trackAppViewScreen(activity);
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
```
在`onActivityPaused`的时候我们调用了一下`trackAppViewScreen`方法，并传入当前`activity`，来看看`trackAppViewScreen`方法。

```
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
            SensorsDataAPI.getInstance().track("$AppViewScreen", properties, mBeginTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```
我们在`trackAppViewScreen`方法中创建了JSONObject对象`properties`，用来添加我们需要埋点的数据，比如页面的唯一标示`key`我们用`activity`表示，并取全路径作为`value`。

这里有一点需要注意的，我们除了可以收集一些固定参数之外，activity中`intent`的参数也是可以获取的，比如其他页面跳转到这个页面传的参数，我们同样可以获取到并作为埋点的参数使用的。

就像上面的SecondActivity，当MainActivity跳转到SecondActivity时传的userId是可以通过`getIntent`获取到的。

最后调用了`SensorsDataAPI`类的`track`方法，继续看

```
    public void track(@NonNull String eventName, @Nullable JSONObject properties, long beginTime) {
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
```
这里也很简单，先后创建了两个JSONObject，一个是最外层的`jsonObject` ，一个是作为参数使用的`sendProperties`，然后又把传过来的参数合并到sendProperties中，然后sendProperties作为`extras`的`value`使用。

`endTime`结束时间就取当前时间。

`sessionId`表示是这个埋点的唯一标示，看自己需求，非必须。

最后调用了`Log`打印出来，来看一下最后完整的数据：

```
{
	"event": "$AppViewScreen",
	"extras": {
		"app_name": "TrackDemo",
		"screen_width": 1440,
		"screen_height": 2621,
		"app_version": "1.0",
		"os_version": "10",
		"model": "Android SDK built for x86",
		"manufacturer": "Google",
		"activity": "com.yechaoa.trackdemo.ui.MainActivity"
	},
	"beginTime": 1603279291751,
	"endTime": 1603279293759,
	"pageId": "com.yechaoa.trackdemo.ui.MainActivity",
	"sessionId": "5dbb96807e634b6498f897784972ade3"
}
```
可以看到除了我们必要的参数之外，还有一些附加参数，比如`手机型号、系统版本`等等。

### Fragment
上面是Activity的埋点，关于`fragment`书中并没有讲解，不过我们也可以按照`生命周期`的方式来处理，比如在`BaseFragment`中进行统一埋点，又或者单独处理，正好演示一下`手动埋点`的操作。

示例：

```
    private var mBeginTime = 0L

    override fun onResume() {
        super.onResume()

        mBeginTime = System.currentTimeMillis()
    }
```
首先在`onResume`中记录一下开始时间。

```
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        val blankFragment = this

        if (hidden) {
            val activity = activity as SecondActivity
            val jsonObject = JSONObject()
            jsonObject.put("useActivity", true)
            jsonObject.put("fragment", activity.javaClass.canonicalName + blankFragment.javaClass.canonicalName + "-custom"
            )
            SensorsDataAPI.getInstance().track("AppViewScreen", jsonObject, mBeginTime)
        }
    }
```

然后在`onHiddenChanged`中判断显示与否进行埋点，自定义数据，然后调用`track`方法进行埋点。

唯一标示的key用fragment表示，value用当前引用的`activity全路径`，加上`fragment的全路径`，最后加上自定义的参数，即可作为`唯一标示`。


**以上即为页面埋点的主要代码，以及一些关键的代码细节，最后附Demo地址。**


*别忘了在Application中初始化埋点：*

```
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        //初始化埋点
        SensorsDataAPI.init(this)
    }
}
```

# 事件
一般来说就是`点击事件`，书中的解决方案挺多的，今天现在说说比较简单的，即`代理模式`。

### 原理
拦截系统的点击事件，然后替换成我们自己的点击事件，然后在自己的点击事件中进行埋点操作。

通过获取页面的`根布局`，然后`递归遍历`出所有的view，并代理它们的`click`事件。

示例：

```
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
```

- 在`onActivityCreated`中初始化代理方法，
- 在`onActivityResumed`中添加代理事件，
- 在`onActivityStopped`中移除代理事件。

我们在看看这个代理事件是怎么代理的：

```
    protected static void delegateViewsOnClickListener(final Context context, final android.view.View view) {
        if (context == null || view == null) {
            return;
        }

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
```

可以看到除了`click`之外还有`check`等事件，其实原理都是想通的，我们来挑一个`click`来看看。

先获取`OnClickListener`，怎么获取呢，看`getOnClickListener`方法：

```
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
```
通过反射拿到`OnClickListener`，然后再判断是否被代理，如果没有代理，就换成我们自己的`Listener`

```
 view.setOnClickListener(new WrapperOnClickListener(listener));
```

看一下我们自定义的`WrapperOnClickListener`

```
/*public*/ class WrapperOnClickListener implements android.view.View.OnClickListener {
    private android.view.View.OnClickListener source;

    WrapperOnClickListener(android.view.View.OnClickListener source) {
        this.source = source;
    }

    @Override
    public void onClick(android.view.View view) {
        //调用原有的 OnClickListener
        try {
            if (source != null) {
                source.onClick(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //插入埋点代码
        SensorsDataPrivate.trackViewOnClick(view);
    }
}
```

很简单，也是实现系统的`OnClickListener `方法，然后在执行click的时候`插入埋点代码`。

然后看一下`trackViewOnClick`方法：

```
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
```
比较简单，但是有两个参数是需要注意的：
- `element_type` 控件的类型，比如TextView、Button
- `element_id` 控件的id，页面全路径 + 控件id即可表示唯一标示了

然后就是`trackClick`方法了

```
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
```

跟页面的埋点基本差不太多，来看看数据：

```
{
	"event": "$AppClick",
	"extras": {
		"app_name": "TrackDemo",
		"screen_width": 1440,
		"screen_height": 2621,
		"app_version": "1.0",
		"os_version": "10",
		"model": "Android SDK built for x86",
		"manufacturer": "Google",
		"element_type": "androidx.appcompat.widget.AppCompatButton",
		"element_id": "button",
		"element_content": "点击传值跳转",
		"activity": "com.yechaoa.trackdemo.ui.MainActivity"
	},
	"eventTime": 1603279293756,
	"sessionId": "b8d1aa32039a4fb1b2ece7772d60cd0e"
}
```

可以看到数据都是正常的，但是`element_content`字段并不能太过依赖，为什么呢，因为这个是获取的控件文本，不是所有的控件都有文本的，比如没有描述的`ImageView、CheckBox`等。

这些都是系统的控件，那如果是我们`自定义View`怎么办呢，正好演示一下事件的手动埋点。

```
        button2.setOnClickListener {
            val jsonObject = JSONObject()
            jsonObject.put("element_type", "androidx.constraintlayout.widget.ConstraintLayout")
            jsonObject.put("element_id", "自定义id")
            jsonObject.put("element_content", "自定义内容")
            jsonObject.put("id", 1234)
            jsonObject.put("activity", this.javaClass.canonicalName)
            SensorsDataAPI.getInstance().trackClick("AppClick", jsonObject)
        }
```

看一下数据：

```
 {
    	"event":"AppClick",
    	"extras":{
    		"app_name":"TrackDemo",
    		"screen_width":1440,
    		"screen_height":2621,
    		"app_version":"1.0",
    		"os_version":"10",
    		"model":"Android SDK built for x86",
    		"manufacturer":"Google",
    		"element_type":"androidx.constraintlayout.widget.ConstraintLayout",
    		"element_id":"自定义id",
    		"element_content":"自定义内容",
    		"id":1234,
    		"activity":"com.yechaoa.trackdemo.ui.SecondActivity",
    		"userId":"111"
    	},
    	"eventTime":1603283095128,
    	"sessionId":"addbc3d8335244328fcd352221a7a11d"
    }
```
加入`自定义view`监测不到的情况下，就可以用这种方式来手动埋点。


除了正常的单个控件的点击事件之外，还有列表的`item click`事件、还有`RatingBar`等等，限于篇幅就不细说了，原理都是相通的，具体可以查看Demo。
