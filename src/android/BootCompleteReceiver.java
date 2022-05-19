package com.plugin.talkback;

import static com.kylindev.pttlib.LibConstants.ACTION_AUTO_LAUNCH;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.blankj.utilcode.util.LogUtils;
import com.kylindev.pttlib.service.InterpttService;

public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        boolean auto = true;  // 自启动

        if (auto) {
            //延时一段时间再启动
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                        Intent serviceIntent = new Intent(context, InterpttService.class);

                        //自动启动service，在Service的实现里判断，如果是自动启动的，则自动登录
                        serviceIntent.setAction(ACTION_AUTO_LAUNCH);

                        if (Build.VERSION.SDK_INT >= 26) {
                            context.startForegroundService(serviceIntent);
                        }
                        else {
                            context.startService(serviceIntent);
                        }
                        LogUtils.e("对讲 开机自启动");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
