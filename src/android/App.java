package com.plugin.talkback;

import static com.kylindev.pttlib.LibConstants.INTERPTT_SERVICE;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.Gravity;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ProcessUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.Contact;
import com.kylindev.pttlib.service.model.User;

import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public class App extends Application {

    private static InterpttService mService;

    public static InterpttService getService() {
        return mService;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (ProcessUtils.isMainProcess()) {
            // blankj
            Utils.init(this);
            LogUtils.getConfig().setLogSwitch(true);
            ToastUtils.getDefaultMaker().setGravity(Gravity.CENTER, 0, 0);
            ToastUtils.getDefaultMaker().setMode(ToastUtils.MODE.DARK);

            // Rxjava2异常 全局捕获RxJava2下onNext()中的异常
            if (!com.plugin.talkback.BuildConfig.DEBUG) {
                RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        LogUtils.e("RxJava catch global exception", throwable);
                    }
                });
            }

            // 开机启动
            IntentFilter bootIf = new IntentFilter();
            bootIf.addAction("android.intent.action.BOOT_COMPLETED");
            bootIf.addCategory("android.intent.category.HOME");
            bootIf.addCategory("android.intent.category.DEFAULT");
            registerReceiver(new com.plugin.talkback.BootCompleteReceiver(), bootIf);

            pttService();
        }
    }

    protected boolean mServiceBind = false;
    protected Intent mServiceIntent = null;
    protected ServiceConnection mServiceConnection = null;

    protected void initServiceConnection() {
        mServiceIntent = new Intent(this, InterpttService.class);;

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InterpttService.LocalBinder localBinder = (InterpttService.LocalBinder) service;
                mService = localBinder.getService();

                mService.registerObserver(serviceObserver);

                autoLogin();
            }

            //此方法调用时机：This is called when the connection with the service has been unexpectedly disconnected
            //-- that is, its process crashed. Because it is running in our same process, we should never see this happen.
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
                mServiceConnection = null;
                stopService(mServiceIntent);

                //App.setService(null);

                //此函数只有在service被异常停止时才会调用，如被系统或其他软件强行停止
                AppUtils.relaunchApp(true);
            }
        };
    }

    /**
     * 初始化 PTT service
     */
    private void pttService() {
        initServiceConnection();

        if (!com.plugin.talkback.AppCommonUtil.isServiceRunning(this, INTERPTT_SERVICE)) {
            startService(mServiceIntent);
        }

        mServiceBind = bindService(mServiceIntent, mServiceConnection, 0);

        com.plugin.talkback.PttWidgetTimeProvider.updateTime(getBaseContext());
    }


    private static short maxVolume = 0;
    private static User curTalkingUser;
    private static Runnable runnable = () -> {
        com.plugin.talkback.PttWidgetProvider.updateVolume(Utils.getApp(), (short) 0);
        com.plugin.talkback.PttWidgetProvider.updatePtt(Utils.getApp(), null, 0, null, 0, false);

        com.plugin.talkback.PttWidgetTimeProvider.updateVolume(Utils.getApp(), (short) 0);
        com.plugin.talkback.PttWidgetTimeProvider.updatePtt(Utils.getApp(), null, 0, null, 0, false);
    };

    ////////////////////////////////
    private static BaseServiceObserver serviceObserver = new BaseServiceObserver() {

        @Override
        public void onConnectionStateChanged(InterpttService.ConnState state) throws RemoteException {
            LogUtils.e("onConnectionStateChanged", state);
            switch (state) {
                case CONNECTION_STATE_CONNECTING:
                    //ToastUtils.showShort("正在登录对讲服务器...");
                    com.plugin.talkback.PttWidgetProvider.updateLogin(mService.getBaseContext(), null, 0, null, 0, 0);
                    com.plugin.talkback.PttWidgetTimeProvider.updateLogin(mService.getBaseContext(), null, 0, null, 0, 0);
                    break;
                case CONNECTION_STATE_SYNCHRONIZING:
                    break;
                case CONNECTION_STATE_CONNECTED:
                    //ToastUtils.showLong("登录对讲服务器 success");
                    User user = mService.getCurrentUser();
                    com.plugin.talkback.PttWidgetProvider.updateLogin(mService.getBaseContext(), user.name, user.iId, user.getChannel().name, user.getChannel().id, 1);
                    com.plugin.talkback.PttWidgetTimeProvider.updateLogin(mService.getBaseContext(), user.name, user.iId, user.getChannel().name, user.getChannel().id, 1);
                    break;
                case CONNECTION_STATE_DISCONNECTED:
                    ToastUtils.showLong("对讲服务器 disconnected，请检查网络或账号密码。");
                    com.plugin.talkback.PttWidgetProvider.updateLogin(mService.getBaseContext(), null, 0, null, 0, -1);
                    com.plugin.talkback.PttWidgetTimeProvider.updateLogin(mService.getBaseContext(), null, 0, null, 0, -1);
                    break;
            }

            if (mService != null) {
                mService.userPressUp();
            }

            //可靠通知lcd直播指示
            if (state != CONNECTION_STATE_CONNECTED) {
            }
        }

        @Override
        public void onPermissionDenied(String reason, int denyType) throws RemoteException {
            ToastUtils.showLong(reason);
        }

        @Override
        public void onCurrentChannelChanged() throws RemoteException {
            if (mService == null) {
                return;
            }

            if (mService.getConnectionState() == CONNECTION_STATE_CONNECTED) {
                //如果按着ptt时切换频道，则需先放弃讲话
                mService.userPressUp();
            }
        }

        @Override
        public void onChannelAdded(Channel channel) throws RemoteException {
        }

        @Override
        public void onChannelRemoved(Channel channel) throws RemoteException {
        }

        //某个频道信息有变化
        @Override
        public void onChannelUpdated(Channel channel) throws RemoteException {
        }

        @Override
        public void onUserUpdated(User user) throws RemoteException {
        }

        /**
         * 别人或自己正在讲话
         *
         * @param user
         * @param talk
         * @param recordUrl
         * @param duration
         * @param seqId
         */
        @Override
        public void onUserTalkingChanged(User user, boolean talk, String recordUrl, int duration, long seqId) throws RemoteException {
            LogUtils.e("正在讲话", "name: " + user.name, "iId: " + user.iId, "c.name: " + user.getChannel().name, "c.id: " + user.getChannel().id);
            curTalkingUser = user;
        }

        /**
         * 别人讲话或自己讲话，音量变化
         *
         * @param volume
         */
        @Override
        public void onNewVolumeData(short volume) throws RemoteException {
            if (maxVolume < volume) {
                maxVolume = volume;
            }
            LogUtils.e("音量 " + volume, "音量max " + maxVolume);

            if (curTalkingUser == null)  return;

            if (user_id == curTalkingUser.iId) {
                if (volume == 0) {
                    // 自己不讲话了，就立即变灰色
                    com.plugin.talkback.PttWidgetProvider.updateVolume(Utils.getApp(), (short) 0);
                    com.plugin.talkback.PttWidgetProvider.updatePtt(Utils.getApp(), null, 0, null, 0, false);

                    com.plugin.talkback.PttWidgetTimeProvider.updateVolume(Utils.getApp(), (short) 0);
                    com.plugin.talkback.PttWidgetTimeProvider.updatePtt(Utils.getApp(), null, 0, null, 0, false);
                } else {
                    com.plugin.talkback.PttWidgetProvider.updateVolume(Utils.getApp(), volume);
                    com.plugin.talkback.PttWidgetProvider.updatePtt(Utils.getApp(), curTalkingUser.name, curTalkingUser.iId, curTalkingUser.getChannel().name, curTalkingUser.getChannel().id, user_id == curTalkingUser.iId);

                    com.plugin.talkback.PttWidgetTimeProvider.updateVolume(Utils.getApp(), volume);
                    com.plugin.talkback.PttWidgetTimeProvider.updatePtt(Utils.getApp(), curTalkingUser.name, curTalkingUser.iId, curTalkingUser.getChannel().name, curTalkingUser.getChannel().id, user_id == curTalkingUser.iId);
                }
            } else {
                com.plugin.talkback.PttWidgetProvider.updateVolume(Utils.getApp(), (short) (volume + 1));  // 防止0 图标变为灰色 R.drawable.ic_play_video_gray
                com.plugin.talkback.PttWidgetProvider.updatePtt(Utils.getApp(), curTalkingUser.name, curTalkingUser.iId, curTalkingUser.getChannel().name, curTalkingUser.getChannel().id, user_id == curTalkingUser.iId);

                com.plugin.talkback.PttWidgetTimeProvider.updateVolume(Utils.getApp(), (short) (volume + 1));  // 防止0 图标变为灰色 R.drawable.ic_play_video_gray
                com.plugin.talkback.PttWidgetTimeProvider.updatePtt(Utils.getApp(), curTalkingUser.name, curTalkingUser.iId, curTalkingUser.getChannel().name, curTalkingUser.getChannel().id, user_id == curTalkingUser.iId);

                ThreadUtils.getMainHandler().removeCallbacks(runnable);

                // 出现 volume 为0之后，还会跟上几个不为0的
                ThreadUtils.getMainHandler().postDelayed(runnable, 400L);
            }
        }

        @Override
        public void onMicStateChanged(InterpttService.MicState micState) throws RemoteException {
            LogUtils.e(micState);
        }

        @Override
        public void onHeadsetStateChanged(InterpttService.HeadsetState s) throws RemoteException {
        }

        @Override
        public void onScoStateChanged(int s) throws RemoteException {
        }

        @Override
        public void onTargetHandmicStateChanged(BluetoothDevice device, InterpttService.HandmicState s) throws RemoteException {
        }

        @Override
        public void onTalkingTimerTick(int seconds) throws RemoteException {
            String time = String.format("%02d", seconds / 60) + ":" + String.format("%02d", seconds % 60);
            LogUtils.e(time);
        }

        @Override
        public void onTalkingTimerCanceled() throws RemoteException {
        }

        @Override
        public void onUserAdded(final User u) throws RemoteException {
            LogUtils.e(GsonUtils.toJson(u));
        }

        @Override
        public void onUserRemoved(final User user) throws RemoteException {
            LogUtils.e(GsonUtils.toJson(user));
        }

        @Override
        public void onLeDeviceScanStarted(boolean start) throws RemoteException {
        }

        /**
         *
         * @param str e.g. 加入频道推扒队
         */
        @Override
        public void onShowToast(final String str) throws RemoteException {
            LogUtils.e(str);
            ToastUtils.showLong(str);
        }

        @Override
        public void onInvited(final Channel channel) throws RemoteException {
            LogUtils.e(GsonUtils.toJson(channel));
        }

        @Override
        public void onUserSearched(User user) throws RemoteException {
        }

        @Override
        public void onPendingMemberChanged() throws RemoteException {
        }

        @Override
        public void onApplyContactReceived(boolean add, Contact contact) throws RemoteException {
        }

        @Override
        public void onPendingContactChanged() throws RemoteException {
        }

        @Override
        public void onContactChanged() throws RemoteException {
        }

        @Override
        public void onSynced() throws RemoteException {
        }

        @Override
        public void onVoiceToggleChanged(boolean on) throws RemoteException {
            LogUtils.e(on);
        }

        @Override
        public void onPlayRouteChanged(int i) throws RemoteException {
        }

        @Override
        public void onLocOnChanged(boolean on) throws RemoteException {
        }

        @Override
        public void onListenChanged(boolean listen) throws RemoteException {
            LogUtils.e(listen);
        }

        @Override
        public void onApplyOrderResult(int uid, int cid, String phone, boolean success, long seqId) throws RemoteException {
        }

        @Override
        public void onUserOrderCall(final User user, boolean talk, String number) {
            LogUtils.e(GsonUtils.toJson(user), talk, number);
        }

        @Override
        public void onGeneralMessageGot(final int fromId, final int targetType, final int targetId, final int type, final String content) throws RemoteException {
        }

        @Override
        public void onEntUpdated() throws RemoteException{
        }

        @Override
        public void onCastingChanged(String url, boolean start, int uid, int cid) {
        }
    };


//    public static final String PTT_CONFIG_FILE = "/storage/emulated/0/ptt_config";

    public static String ent_id;
    public static int user_id;
    public static String password;
    public static String server;

    /**
     * 登录
     */
    public static void autoLogin() {
        if (mService == null) return;

        // 读取新的配置文件
//        try {
//            JSONObject ptt_config = new JSONObject(FileIOUtils.readFile2String(PTT_CONFIG_FILE));
//            // 转json
//            ent_id = ptt_config.getString("ent_id");
//            user_id = ptt_config.getInt("user_id");
//            password = ptt_config.getString("password");
//            server = ptt_config.getString("server");
//            LogUtils.e("配置信息", ent_id, user_id, password, server);
//        } catch (Exception e) {
//            LogUtils.e("读取配置信息失败！", e);
//        }

        if (!TextUtils.isEmpty(server)) {
            mService.login(
                    server,
                    "59638",
                    "59638",
                    com.plugin.talkback.AppConstants.ENT_VERSION ? 1 : 0,
                    ent_id,
                    String.valueOf(user_id),
                    password);
        }
    }
}
