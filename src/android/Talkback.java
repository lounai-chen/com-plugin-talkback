package com.plugin.talkback;

import static com.kylindev.pttlib.LibConstants.INTERPTT_SERVICE;
import static com.kylindev.pttlib.service.InterpttService.ConnState;
import static com.kylindev.pttlib.service.InterpttService.ConnState.CONNECTION_STATE_CONNECTED;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.kylindev.pttlib.service.BaseServiceObserver;
import com.kylindev.pttlib.service.InterpttService;
import com.kylindev.pttlib.service.InterpttService.HandmicState;
import com.kylindev.pttlib.service.InterpttService.HeadsetState;
import com.kylindev.pttlib.service.InterpttService.LocalBinder;
import com.kylindev.pttlib.service.InterpttService.MicState;
import com.kylindev.pttlib.service.model.Channel;
import com.kylindev.pttlib.service.model.Contact;
import com.kylindev.pttlib.service.model.User;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class Talkback extends CordovaPlugin {

    /**
     * The InterpttService instance that drives this activity's data.
     */
    protected InterpttService mService;
    protected Intent mServiceIntent = null;

    protected boolean mServiceBind = false;        //有时在unbindService时报错java.lang.IllegalArgumentException:  Service not registered，因此增加此变量unbind之前判断

    /**
     * Management of service connection state.
     */
    protected ServiceConnection mServiceConnection = null;

    protected void initServiceConnection() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder localBinder = (LocalBinder) service;
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
                cordova.getContext().stopService(mServiceIntent);

                //App.setService(null);

                //此函数只有在service被异常停止时才会调用，如被系统或其他软件强行停止
                cordova.getActivity().finish();
            }
        };
    }

    @Override
    protected void pluginInitialize() {

        super.pluginInitialize();
        //可能是首次运行，也可能是用户重新launch
        //因此，先检查service是否在运行，如果是，则直接bind以获取mService实例；如果没有，则startService，再bind
        mServiceIntent = new Intent(cordova.getContext(), InterpttService.class);

        if (!com.plugin.talkback.AppCommonUtil.isServiceRunning(cordova.getContext(), INTERPTT_SERVICE)) {
            cordova.getContext().startService(mServiceIntent);
        }

        initServiceConnection();
        mServiceBind = cordova.getContext().bindService(mServiceIntent, mServiceConnection, 0);
//        requestPrevilege();
    }

    @Override
    public void onDestroy() {

        // Unbind to service
        //为保证service不被杀死，activity在back按键时，只pause，不destroy。
        //那么，如果发现destroy，则应检查是否是用户关闭的。如果不是，则应重新启动activity
        //此时，说明activity不是用户退出的，而是被系统或其他应用杀死的。
        //应通知service，让其稍后重启activity
        LogUtils.e("解绑服务！");
        if (mService != null) {
            mService.unregisterObserver(serviceObserver);
            if (mServiceConnection != null) {
                if (mServiceBind) {
                    cordova.getContext().unbindService(mServiceConnection);
                }
                mServiceConnection = null;
            }

            mService = null;
        }

        super.onDestroy();
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            boolean selecting = mService!=null && mService.isSelectingContact();
//            if (selecting) {
//                mService.cancelSelect();
//                return true;
//            }
//        }
//        else {
//            if (mService != null) {
//                int savedCode = mService.getPttKeycode();
//                if (savedCode!=0 && savedCode==keyCode) {
//                    //至此，说明需要响应ptt事件
//
//                    mService.userPressDown();
//                    return true;
//                }
//            }
//            else {
//                return false;
//            }
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if (mService != null) {
//            int savedCode = mService.getPttKeycode();
//            if (savedCode != 0 && savedCode == keyCode) {
//                mService.userPressUp();
//                super.onKeyUp(keyCode, event);
//                return true;
//            }
//        }
//
//        return super.onKeyUp(keyCode, event);
//    }

    private static int rp = 0;

    private void requestPrevilege() {
        new RxPermissions(cordova.getActivity()).requestEach(
                Manifest.permission.RECEIVE_BOOT_COMPLETED,

                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,

                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ).subscribe(permission -> {
            if (permission.name.equals("android.permission.READ_EXTERNAL_STORAGE")) {
                if (!permission.granted) {
                    if (rp < 3) {
                        ToastUtils.showLong("请授予存储空间读取权限！");
                        ThreadUtils.getMainHandler().postDelayed(() -> AppUtils.relaunchApp(), 2_000l);
                    } else {
                        ToastUtils.showLong("请手动授予存储空间读取权限！");
                        ThreadUtils.getMainHandler().postDelayed(() -> AppUtils.exitApp(), 2_000l);
                    }
                    rp ++;
                    return;
                }

                if (TextUtils.isEmpty(server)) {
                    ToastUtils.showLong("读取登录信息失败！");
                    return;
                }

                autoLogin();
            }
        });
    }

    private String ent_id;
    private int user_id;
    private String password;
    private String server;

    /**
     * 登录
     */
    private void autoLogin() {
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

    /**
     * 停止服务
     */
    private void stopPttService() {
        if (mServiceBind) {
            cordova.getContext().unbindService(mServiceConnection);
            mServiceBind = false;
        }

        if (mService != null) {
            mService.unregisterObserver(serviceObserver);

            //首先停止音频，因为当前可能正在讲话
            mService.userPressUp();

            //先断开连接。此时可能已经处于断开状态了，若已断开，则无需再次调用断开，但需要停止重连
            InterpttService.ConnState connState = mService.getConnectionState();
            //加上connecting判断，否则在3g连接、然后连上一个实际不能上网的wifi时，无法退出，因为mService.disconnect超时
            if (connState == ConnState.CONNECTION_STATE_DISCONNECTED || connState == ConnState.CONNECTION_STATE_CONNECTING) {
                //若已断开，无需先调用disconnect，再等待disconnected回调，直接退出即可

            } else {
               /*new Thread(new Runnable() {
                   @Override
                   public void run() {
                       mService.disconnect();
                   }
               }).start();*/
                //如果新线程断开，有时候退出不成功，所以直接disconnect。
                //但是有新的问题：退出时会toast提示：连接失败，请重试
                mService.disconnect();
            }

            //记录用户意图停止
            mService.appWantQuit();
        }

        if (com.plugin.talkback.AppCommonUtil.isServiceRunning(cordova.getContext(), INTERPTT_SERVICE)) {
            cordova.getContext().stopService(mServiceIntent);
        }

        LogUtils.w("is Ptt service running", com.plugin.talkback.AppCommonUtil.isServiceRunning(cordova.getContext(), INTERPTT_SERVICE));
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LogUtils.e("调用coolMethod！");
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        else if(action.equals(("login"))){
            ent_id = args.getString(0);
            user_id = args.getInt(1);
            password = args.getString(2);
            server = args.getString(3);
            if (mService == null) {
                //如果之前没有启动并bind mService，则需先bind，在onServiceConnected里开始connect
                if (mServiceConnection == null) {
                    initServiceConnection();
                }

                mServiceBind = cordova.getContext().bindService(mServiceIntent, mServiceConnection, 0);
            } else {
                autoLogin();
            }
            return true;
        }
        else if(action.equals(("enterChannel"))){
            int channelId = args.getInt(0);
            if (mService != null) {
                mService.enterChannel(channelId);  // 进入频道
            }
            return true;
        }
        else if(action.equals(("joinChannel"))){
            int channelId = args.getInt(0);
            String pwd = args.getString(1);
            String comment = args.getString(2);
            if (mService != null) {
                mService.joinChannel(channelId, pwd, comment);  // 加入频道
            }
            return true;
        }
        else if(action.equals(("quitChannel"))){
            int channelId = args.getInt(0);
            if (mService != null) {
                mService.quitChannel(channelId);  // 退出频道
            }
            return true;
        }
        else if(action.equals("channelList")){
            if(mService != null){
                List<Channel> channelList = mService.getChannelList();
                if (channelList != null && !channelList.isEmpty()){
                    callbackContext.success(GsonUtils.toJson(channelList));
                }
            }
            return true;
        }
        else if(action.equals("pttKeyDown")){
            if (mService != null) {
                mService.userPressDown();
            }
            return true;
        }
        else if(action.equals("pttKeyUp")){
            if (mService != null) {
                mService.userPressUp();
            }
            return true;
        }
        else if(action.equals("cancelSelect")){
            boolean selecting = mService!=null && mService.isSelectingContact();
            if (selecting) {
                mService.cancelSelect();
            }
            return true;
        }
        else if(action.equals("userList")){
            if (mService != null) {
                List<User> userList = mService.getUserList();
                if (userList != null && !userList.isEmpty()){
                    callbackContext.success(GsonUtils.toJson(userList));
                }
            }
            return true;
        }
        else if(action.equals("quitApp")){
            stopPttService();
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    ////////////////////////////////
    private BaseServiceObserver serviceObserver = new BaseServiceObserver() {

        @Override
        public void onConnectionStateChanged(ConnState state) throws RemoteException {
            LogUtils.e(state);
            switch (state) {
                case CONNECTION_STATE_CONNECTING:
                    ToastUtils.showShort("正在登录对讲服务器...");
                    break;
                case CONNECTION_STATE_SYNCHRONIZING:
                    break;
                case CONNECTION_STATE_CONNECTED:
                    ToastUtils.showLong("登录对讲服务器 success");
                    break;
                case CONNECTION_STATE_DISCONNECTED:
                    ToastUtils.showLong("对讲服务器 disconnected，请检查网络或账号密码。");
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
        }

        /**
         * 别人讲话或自己讲话，音量变化
         *
         * @param volume
         */
        @Override
        public void onNewVolumeData(short volume) throws RemoteException {
        }

        @Override
        public void onMicStateChanged(MicState micState) throws RemoteException {
            LogUtils.e(micState);
        }

        @Override
        public void onHeadsetStateChanged(HeadsetState s) throws RemoteException {
        }

        @Override
        public void onScoStateChanged(int s) throws RemoteException {
        }

        @Override
        public void onTargetHandmicStateChanged(BluetoothDevice device, HandmicState s) throws RemoteException {
        }

        @Override
        public void onTalkingTimerTick(int seconds) throws RemoteException {
        }

        @Override
        public void onTalkingTimerCanceled() throws RemoteException {
        }

        @Override
        public void onUserAdded(final User u) throws RemoteException {
        }

        @Override
        public void onUserRemoved(final User user) throws RemoteException {
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
        }

        @Override
        public void onInvited(final Channel channel) throws RemoteException {
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
        }

        @Override
        public void onPlayRouteChanged(int i) throws RemoteException {
        }

        @Override
        public void onLocOnChanged(boolean on) throws RemoteException {
        }

        @Override
        public void onListenChanged(boolean listen) throws RemoteException {
        }

        @Override
        public void onApplyOrderResult(int uid, int cid, String phone, boolean success, long seqId) throws RemoteException {
        }

        @Override
        public void onUserOrderCall(final User user, boolean talk, String number) {
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
}
