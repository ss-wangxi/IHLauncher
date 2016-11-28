package com.btime.launcher.report.gps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cc.snser.launcher.App;

import com.btime.launcher.util.XLog;

import android.app.AlarmManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

public class GPSTimeManager {
    private static final boolean ENABLED = true;
    
    private static long sLastAdjustSystemTimeMills = -1;
    
    /**
     * 用GPS时间校准本地时间
     * @param location
     */
    public static void handleAdjustSystemTime(Location location) {
        if (ENABLED && sLastAdjustSystemTimeMills < 0 && location != null) {
            if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
                final long newSystemTime = location.getTime();
                if (setSystemTime(newSystemTime)) {
                    sLastAdjustSystemTimeMills = newSystemTime;
                }
            }
        }
    }
    
    public static boolean setSystemTime(long timeMills) {
        try {
            AlarmManager am = (AlarmManager)App.getAppContext().getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.setTime(timeMills);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean succ = Math.abs(System.currentTimeMillis() - timeMills) < 10 * 1000;
        XLog.d("GPSTimeManager", "setSystemTime timeMills=" + timeMills + " timeDisp=" + formatTime(timeMills) + " succ=" + succ);
        //ToastUtils.showMessage(App.getAppContext(), "onLocationChanged setSystemTime " + (succ ? "succ" : "fail"));
        return succ;
    }
    
    private static String formatTime(long timeMillsUTC) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timeMillsUTC));
    }

}
