package com.plugin.talkback;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import com.blankj.utilcode.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppCommonUtil {
    public static boolean isServiceRunning(Context mContext,String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        //200 安全起见，此值选大点，以免不够
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(200);

        if (!(serviceList.size()>0)) {
            return false;
        }

        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className)) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    public static boolean validUserId(String str) {
        //这里设计手机号登录时加区号的问题，要放宽id检查，还不能直接用企业版规则
        if (com.plugin.talkback.AppConstants.ENT_VERSION) {
            return (matchPattern(str, "^[a-zA-Z0-9_]*$"));	//字母，数字，下划线
        }
        else {
            return validPhone(str) || validTotalkId(str);
        }
    }

    //7位滔滔号，100万到1000万-1
    public static boolean validTotalkId(String str) {
        if (str == null) return false;

        if (! str.matches("\\d+")) {
            return false;
        }

        int id = 0;
        try {
            id = Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }

        return (id>1000000 && id<=9999999);
    }

    public static boolean validEntId(String str) {
        if (str == null || str.length()==0) {
            return false;
        }

        if (! str.matches("\\d+")) {
            return false;
        }

        int id = 0;
        try {
            id = Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }

        return (id>=10000 && id<=99999);
    }

    public static boolean validChannelName(String str) {
        return (matchPattern(str, com.plugin.talkback.AppConstants.EX_CHANNELNAME) && str.length()<= com.plugin.talkback.AppConstants.NAME_MAX_LENGTH);
    }

    public static boolean validPwd(String str) {
        return (matchPattern(str, com.plugin.talkback.AppConstants.EX_PASSWORD));
    }

    public static boolean validChannelPwd(String str) {
        return (matchPattern(str, com.plugin.talkback.AppConstants.EX_CHANNEL_PASSWORD) && !hasChinese(str) || str.length()==0);
    }

    public static boolean validNick(String str) {
        return (matchPattern(str, com.plugin.talkback.AppConstants.EX_NICK) && str.length()<= com.plugin.talkback.AppConstants.NAME_MAX_LENGTH);
    }

    public static boolean validPhone(String str) {
        if (str==null || str.length()==0) {
            return false;
        }
        try {
            long val = Long.valueOf(str);
            val = val / 10000;
            return (val > 1000 && val < 10000000);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean validCode(String str) {
        return (matchPattern(str, com.plugin.talkback.AppConstants.EX_VERIFY_CODE));
    }

    public static boolean validChannelId(String str) {
        if (str == null) return false;

        if (! str.matches("\\d+")) {
            return false;
        }

        int id = 0;
        try {
            id = Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }

        return (id>0 && id<999999);
    }

    private static boolean matchPattern(String source, String ex) {
        if (source == null)
            return false;

        Pattern pattern = Pattern.compile(ex);
        Matcher matcher = pattern.matcher(source);

        return matcher.matches();
    }

    /**
     * 中文识别
     */
    public static boolean hasChinese(String source) {
        String reg_charset = "([\\u4E00-\\u9FA5]*+)";
        Pattern p = Pattern.compile(reg_charset);
        Matcher m = p.matcher(source);
        boolean hasChinese = false;
        while (m.find()) {
            if(!"".equals(m.group(1))){
                hasChinese=true;
            }
        }

        return hasChinese;
    }

    public static void showToast(Context c, int strId) {
        if (c == null) {
            c = Utils.getApp();
        }

        if (c != null) {
            Toast t = Toast.makeText(c, strId, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        }
    }

    public static void showToast(Context c, String str) {
        if (c == null) {
            c = Utils.getApp();
        }

        if (c != null) {
            Toast t = Toast.makeText(c, str, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        }
    }


    public static void checkUpdate(boolean force) {

    }

    public static String getAppVersionName() {
        String versionName;// 版本

        Context c = Utils.getApp();
        try {
            if (c != null) {
                PackageManager pm = c.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(c.getPackageName(), PackageManager.GET_CONFIGURATIONS);
                versionName = "V" + pi.versionName;// 获取在AndroidManifest.xml中配置的版本号
            } else {
                versionName = "";
            }
        } catch (NameNotFoundException e) {
            versionName = "";
        }

        return versionName;
    }

    public static int wifiIp(Context context) {
        WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiMan.getConnectionInfo();

        return info.getIpAddress();
    }

    public static boolean existSDCard() {
        boolean flag = false;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            flag = true;
        }
        return flag;
    }

    public static String getSdCardDirectory() {
        File path = Environment.getExternalStorageDirectory();
        return path.getPath();
    }

    public static Animation createTalkingAnimation() {
        Animation a = new AlphaAnimation((float)1.0,(float)0.2); // Change alpha from fully visible to invisible
        a.setDuration(300);
        a.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        a.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        a.setRepeatMode(Animation.REVERSE); //

        return a;
    }

    public static Animation createRotateAnimation() {
        //参数1：从哪个旋转角度开始
        //参数2：转到什么角度
        //后4个参数用于设置围绕着旋转的圆的圆心在哪里
        //参数3：确定x轴坐标的类型，有ABSOLUT绝对坐标、RELATIVE_TO_SELF相对于自身坐标、RELATIVE_TO_PARENT相对于父控件的坐标
        //参数4：x轴的值，0.5f表明是以自身这个控件的一半长度为x轴
        //参数5：确定y轴坐标的类型
        //参数6：y轴的值，0.5f表明是以自身这个控件的一半长度为x轴
        RotateAnimation a = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF,0.5f,
                Animation.RELATIVE_TO_SELF,0.5f);
        a.setDuration(5000);
        a.setRepeatCount(-1);
        a.setInterpolator(new LinearInterpolator());	//匀速

        return a;
    }

    /**
     * 得到设备屏幕的宽度
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 得到设备屏幕的高度
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static String getRandomStr(int len) {
        //字符源，可以根据需要删减
        String generateSource = "0123456789abcdefghigklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String rtnStr = "";
        for (int i = 0; i < len; i++) {
            //循环随机获得当次字符，并移走选出的字符
            String nowStr = String.valueOf(generateSource.charAt((int) Math.floor(Math.random() * generateSource.length())));
            rtnStr += nowStr;
            generateSource = generateSource.replaceAll(nowStr, "");
        }
        return rtnStr;
    }

    /**
     * 十六进制转换字符串
     * @return String 对应的字符串
     */
    public static String hexStr2Str(String hexStr)
    {
        String str = "0123456789abcdef";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;

        for (int i = 0; i < bytes.length; i++)
        {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    public static boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static String getAppDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + com.plugin.talkback.AppConstants.FILE_DIR+ "/";
    }

    /**
     * 递归创建文件夹
     *
     * @param file
     * @return 创建失败返回""
     */
    public static String createFile(File file) {
        try {
            if (! file.getParentFile().exists()) {
                createDir(file.getParentFile().getAbsolutePath());
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 递归创建文件夹
     *
     * @param dirPath
     * @return 创建失败返回""
     */
    private static String createDir(String dirPath) {
        try {
            File file = new File(dirPath);
            if (file.getParentFile().exists()) {
                file.mkdir();
                return file.getAbsolutePath();
            } else {
                createDir(file.getParentFile().getAbsolutePath());
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dirPath;
    }

    //把fileUri转换成ContentUri
    public static Uri getImageContentUri(Context context, File imageFile){
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = null;
        cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            cursor.close();
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                if (cursor != null) {
                    cursor.close();
                }
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
        }
    }


    public static byte[] fileToBytes(String fullName) {
        if (fullName == null || fullName.length()==0) {
            return null;
        }

        byte[] buffer = null;
        try
        {
            File file = new File(fullName);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1)
            {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        return buffer;
    }

    public static byte[] getThumbBytes(byte[] data) {
        Bitmap bm = null;
        if (data!=null && data.length > 0) {
            //ByteString->Bitmap
            bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            int width = bm.getWidth();
            int height = bm.getHeight();
            boolean horiz = (width>height);	//宽>高，认为是横着的图片
            int bigger =  horiz ? width : height;
            int wantBigger = 160;	//160时，输出size 30KB，80时，8KB
            float scale = ((float) wantBigger) / bigger;
            // 取得想要缩放的matrix参数
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            // 得到新的图片
            Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (newbm != null) {
                newbm.compress(Bitmap.CompressFormat.PNG, 30, baos);
            }
            byte[] bytes = baos.toByteArray();
            return bytes;
        }

        return null;
    }


    @SuppressLint("SimpleDateFormat")
    private static String getDay(String time) {
        String showDay = null;
        String nowTime = returnTime();
        try {
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            Date now = df.parse(nowTime);
            Date date = df.parse(time);
            long diff = now.getTime() - date.getTime();
            long day = diff / (24 * 60 * 60 * 1000);

            if (day >= 365) {
                showDay = time.substring(0, 10);
            }
            //zcx change,只要不是同一天，就显示日期
//            else if (day >= 1 && day < 365) {
//                showDay = time.substring(5, 10);
//            }
            else {
                String today = nowTime.substring(0, 10);
                String timeDay = time.substring(0, 10);
                if (!today.equals(timeDay)) {
                    showDay = time.substring(0, 5);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return showDay;
    }

    @SuppressLint("SimpleDateFormat")
    private static String returnTime() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        String date = sDateFormat.format(new Date());
        return date;
    }

    //把xxx秒转成01:22:33这种时分秒形式
    public static String msToString(int ms) {
        DecimalFormat fmt = new DecimalFormat("00");

        int seconds = ms / 1000 + 1;

        if (seconds < 60) {
            return fmt.format(0) + ":" + fmt.format(seconds);
        }
        else if (seconds < 3600) {
            return fmt.format(seconds/60) + ":" + fmt.format(seconds%60);
        }
        else {
            //一小时以上
            int remainSecs = seconds % 3600;
            return fmt.format(seconds/3600) + ":" + fmt.format(remainSecs/60) + ":" + fmt.format(remainSecs%60);
        }
    }

    //bitmap变圆形
    public static Bitmap roundBitmap(Bitmap bitmap)
    {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int left = 0, top = 0, right = width, bottom = height;
        float roundPx = height/2;
        if (width > height) {
            left = (width - height)/2;
            top = 0;
            right = left + height;
            bottom = height;
        } else if (height > width) {
            left = 0;
            top = (height - width)/2;
            right = width;
            bottom = top + width;
            roundPx = width/2;
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int color = 0xff424242;
        Paint paint = new Paint();
        Rect rect = new Rect(left, top, right, bottom);
        RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }


    /**
     * 将彩色图转换为灰度图
     * @param img 位图
     * @return  返回转换好的位图
     */
    public static Bitmap convertGreyImg(Bitmap img) {
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高

        int []pixels = new int[width * height]; //通过位图的大小创建像素点数组

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for(int i = 0; i < height; i++)  {
            for(int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey  & 0x00FF0000 ) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, com.plugin.talkback.AppConstants.BITMAP_CONFIG);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }
}