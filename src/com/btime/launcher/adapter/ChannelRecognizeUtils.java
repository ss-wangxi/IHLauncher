package com.btime.launcher.adapter;

import android.content.Context;
import android.os.Build;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

public class ChannelRecognizeUtils {
    public static final int CHANNEL_UNKNOWN = -1;
    /**
     * 手机渠道_默认值
     */
    public static final int CHANNEL_PHONE_DEFAULT = 0x101;
    /**
     * btime渠道
     */
    public static final int CHANNEL_C601 = 0x201;
    
    private static int sCurrentChannel = CHANNEL_UNKNOWN;
    
    
    private static final String CHANNEL_CAR_PROPERTY_KEY = "os.car";
    
    private static final String CHANNEL_NAVIBAR_ORIENTATION_KEY = "os.navbar.orientation";
    public static final String CHANNEL_NAVIBAR_ORIENTATION_VALUE_LEFT = "1";
    public static final String CHANNEL_NAVIBAR_ORIENTATION_VALUE_RIGHT = "0";
    
    /**
     * 获取当前设备渠道
     * @param context
     * @return
     */
    public static int getCurrentChannel(Context context) {
        if (sCurrentChannel == CHANNEL_UNKNOWN) {
            final String carProperty = getCarProperty();
            final int screenWidth = ScreenDimensUtils.getScreenRealWidth(context);
            final int screenHeight = ScreenDimensUtils.getScreenRealHeight(context);
            final String model = Build.MODEL;
            sCurrentChannel = CHANNEL_C601;
            XLog.d("Snser", "initCurrentChannel CHANNEL_C601");
        }
        return sCurrentChannel;
    }
    
    private static String getCarProperty() {
        return System.getProperty(CHANNEL_CAR_PROPERTY_KEY, "");
    }
    
    public static String getNavibarOrientation() {
        return System.getProperty(CHANNEL_NAVIBAR_ORIENTATION_KEY, CHANNEL_NAVIBAR_ORIENTATION_VALUE_RIGHT);
    }
    
}
