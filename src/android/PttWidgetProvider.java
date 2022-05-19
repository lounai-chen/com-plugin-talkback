package com.plugin.talkback;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.blankj.utilcode.util.LogUtils;
import com.huayu.parksecurityapp.R;
import com.kylindev.pttlib.service.model.User;


/**
 * 广播接受者
 */
public class PttWidgetProvider extends AppWidgetProvider {

    /** 接收点击 ll_ptt 的响应事件 */
    public static final String OPEN_CHANNEL = "com.enodetech.ptt.OPEN_CHANNEL";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        LogUtils.e("PttWidgetProvider onUpdate", appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            // 获取AppWidget对应的视图
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_ptt);

            // 更新用户当前用户
            User user = com.plugin.talkback.App.getService().getCurrentUser();
            if (user != null) {
                remoteViews.setImageViewResource(R.id.iv_icon, R.drawable.ic_default_avatar);
                remoteViews.setTextViewText(R.id.tv_my_cname, "当前频道:" + user.getChannel().name);
            } else {
                remoteViews.setImageViewResource(R.id.iv_icon, R.drawable.ic_default_avatar_gray);
                remoteViews.setTextViewText(R.id.tv_my_cname, "用户未登录");
            }

            setClick(context, remoteViews);

            // 调用集合管理器对集合进行更新
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    /**
     * resolve: android widget无法点击问题
     * 参考资料：https://blog.csdn.net/cnnumen/article/details/8285962
     *
     * @param context
     * @param remoteViews
     */
    private static void setClick(Context context, RemoteViews remoteViews) {
        // 设置响应 “按钮(ll_ptt)” 的intent
        /*Intent btIntent = new Intent(context, PttWidgetProvider.class);  // setClass 这个必须要设置，不然点击效果会无效
        btIntent.setAction(OPEN_CHANNEL);
        PendingIntent btPendingIntent = PendingIntent.getBroadcast(context, 0, btIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.ll_ptt, btPendingIntent);*/

        Intent btIntent = new Intent(context, PttWidgetProvider.class);  // setClass 这个必须要设置，不然点击效果会无效
        btIntent.setAction(OPEN_CHANNEL);
        PendingIntent btPendingIntent = PendingIntent.getBroadcast(context, 0, btIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.iv_channel, btPendingIntent);
    }


    /**
     * 在Widget中点击了刷新后我们不能像在App中那样给控件设置一个事件监听来在回掉方法中处理。
     * Widget是依赖广播来实现，因此我们点击了刷新后其实仅仅是发送出来一个广播。
     * 如果我们不去处理广播那么点击事件其实是没有任何意义的。
     * 因此，来看ListWidgetProvider中第二个比较重要的方法onReceive()。这个方法比较简单，只要我们对特定的广播来做相应的处理就可以了。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.e(action);
        if (action.equals(OPEN_CHANNEL)) {
            // 接受“ll_ptt”的点击事件的广播
//            openChannelList(context);
        } else if (action.equals("android.appwidget.action.APPWIDGET_DELETED")) {

        } else if (action.equals("android.appwidget.action.APPWIDGET_DISABLED")) {

        } else if (action.equals("android.appwidget.action.APPWIDGET_ENABLED")) {

        } else if (action.equals("android.appwidget.action.APPWIDGET_UPDATE")) {

        }
        super.onReceive(context, intent);
    }

    /**
     * 更新用户是否已登录
     *
     * @param login 是否已登录：0正在登录，1已登录，-1未登录
     */
    public static void updateLogin(Context context, String userName, int userId, String channelName, int channelId, int login) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_ptt);
        if (1 == login) {
            remoteViews.setImageViewResource(R.id.iv_icon, R.drawable.ic_default_avatar);
            remoteViews.setTextViewText(R.id.tv_my_cname, "当前频道:" + channelName);
        } else if (-1 == login) {
            remoteViews.setImageViewResource(R.id.iv_icon, R.drawable.ic_default_avatar_gray);
            remoteViews.setTextViewText(R.id.tv_my_cname, "用户未登录");
        } else if (0 == login) {
            remoteViews.setImageViewResource(R.id.iv_icon, R.drawable.ic_default_avatar_gray);
            remoteViews.setTextViewText(R.id.tv_my_cname, "用户正在登录...");
        }

        refreshWidget(context, remoteViews);
    }

    /**
     * 更新谁在讲话
     *
     * @param self 是否是自己在讲话
     */
    public static void updatePtt(Context context, String userName, int userId, String channelName, int channelId, boolean self) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_ptt);

        if (!TextUtils.isEmpty(userName)) {
            remoteViews.setViewVisibility(R.id.ll_status, View.VISIBLE);
            remoteViews.setTextViewText(R.id.tv_uid, "用户ID:" + userId);
            remoteViews.setTextViewText(R.id.tv_uname, "用户名:" + userName);
            remoteViews.setTextViewText(R.id.tv_cname, "频道:" + channelName + "[" + channelId + "]");

            if (self) {
                remoteViews.setViewVisibility(R.id.iv_mic, View.VISIBLE);
            } else {
                remoteViews.setViewVisibility(R.id.iv_mic, View.GONE);
            }
        } else {
            remoteViews.setViewVisibility(R.id.ll_status, View.INVISIBLE);
            remoteViews.setViewVisibility(R.id.iv_mic, View.GONE);
        }

        refreshWidget(context, remoteViews);
    }

    /**
     * 更新音量
     */
    public static void updateVolume(Context context, short volume) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_ptt);
        remoteViews.setProgressBar(R.id.pg_volume, 32000, volume, false);

        if (volume > 0) {
            remoteViews.setImageViewResource(R.id.iv_volume, R.drawable.ic_play_video);
        } else {
            remoteViews.setImageViewResource(R.id.iv_volume, R.drawable.ic_play_video_gray);
        }

        refreshWidget(context, remoteViews);
    }

    /**
     * 打开频道列表
     */
//    private void openChannelList(Context context) {
//        ActivityUtils.startActivity(ChannelListActivity.class);
//    }

    /**
     * 刷新Widget
     */
    private static void refreshWidget(Context context, RemoteViews remoteViews) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, PttWidgetProvider.class);

        setClick(context, remoteViews);

        appWidgetManager.updateAppWidget(componentName, remoteViews);
    }

    /**
     * onEnabled启动服务即第一个widget创建的时候执行
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        LogUtils.e("onEnabled");
    }

    /**
     * onDisabled停止服务即最后一个widget删除的时候执行
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        LogUtils.e("onDisabled");
    }

    /**
     * 当 Widget 第一次被添加或者大小发生变化时调用该方法，可以在此控制 Widget 元素的显示和隐藏。
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        LogUtils.e("onAppWidgetOptionsChanged");
    }
}


