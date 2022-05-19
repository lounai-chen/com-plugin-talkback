package com.plugin.talkback;

import android.graphics.Bitmap;

public class AppConstants {
    public final static boolean ENT_VERSION = true;
    public final static Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;

    //用户名和密码的正则表达式
    public static final int NAME_MAX_LENGTH = 512;	//频道和用户名称的最大长度
    public static final String EX_CHANNELNAME = "[ \\-=\\w\\#\\[\\]\\{\\}\\(\\)\\@\\|]+";	//从服务器copy过来，与服务器保持一致
    public static final String EX_PASSWORD = "\\w{6,32}+";				//6-32位，不含空格
    public static final String EX_CHANNEL_PASSWORD = "^\\d{4}$";				//4-16位，不含空格
    public static final String EX_NICK = "[-=\\w\\[\\]\\{\\}\\(\\)\\@\\|\\. ]+";		//允许点和空格
    public static final String EX_VERIFY_CODE = "^\\d{4}$";		//4位数字

    public static final String FILE_DIR = "Totalk";
}
