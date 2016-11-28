package com.btime.launcher.util;

import android.content.Context;
import android.util.Log;

public class XLog {
    private static boolean enabled = true;
    
    public static void init(Context context) {
    }
    
    public static void setEnabled(boolean enabled) {
        XLog.enabled = enabled;
    }
    
    public static void v(String tag, String msg) {
        if (enabled) {
            Log.v(tag, msg);
        }
    }
    
    public static void d(String tag, String msg) {
        if (enabled) {
            Log.d(tag, msg);
        }
    }
    
    public static void i(String tag, String msg) {
        if (enabled) {
            Log.i(tag, msg);
        }
    }
    
    public static void w(String tag, String msg) {
        if (enabled) {
            Log.w(tag, msg);
        }
    }
    
    public static void e(String tag, String msg) {
        if (enabled) {
            Log.e(tag, msg);
        }
    }
    
    public static void e(String tag, String msg, Throwable tr) {
        if (enabled) {
            Log.e(tag, msg, tr);
        }
    }
    
    public static void printStackTrace(Throwable tr) {
        if (enabled) {
            tr.printStackTrace();
        }
    }
    
}
