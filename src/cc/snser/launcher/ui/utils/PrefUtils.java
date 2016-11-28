package cc.snser.launcher.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;
import cc.snser.launcher.App;
import cc.snser.launcher.Constant;

import com.btime.launcher.util.XLog;

public class PrefUtils {
    private static final String TAG = "Launcher.PrefUtils";

    private static final boolean DEBUG_LOG = false;

    public static SharedPreferences getSharedPreferences(Context context, String name, int mode) {
        return context.getSharedPreferences(name, mode | 0x0004);
    }

    public static SharedPreferences getHttpPrefFile() {
        return getHttpPrefFile(App.getApp());
    }

    public static SharedPreferences getHttpPrefFile(Context context) {
        return getSharedPreferences(context, "ht", Context.MODE_PRIVATE);
    }

    public static String getStringPref(Context context, String name, String def) {
        return getStringPref(context, Constant.LAUNCHER_PREF_FILE, name, def);
    }

    public static void setStringPref(Context context, String name, String value) {
        setStringPref(context, Constant.LAUNCHER_PREF_FILE, name, value);
    }

    public static void removePref(Context context, String name) {
        removePref(context, Constant.LAUNCHER_PREF_FILE, name);
    }

    public static int getIntPref(Context context, String name, int def) {
        return getIntPref(context, Constant.LAUNCHER_PREF_FILE, name, def);
    }

    public static void setIntPref(Context context, String name, int value) {
        setIntPref(context, Constant.LAUNCHER_PREF_FILE, name, value);
    }

    public static boolean getBooleanPref(Context context, String name,
            boolean def) {
        return getBooleanPref(context, Constant.LAUNCHER_PREF_FILE, name, def);
    }

    public static void setBooleanPref(Context context, String name,
            boolean value) {
        setBooleanPref(context, Constant.LAUNCHER_PREF_FILE, name, value);
    }

    public static long getLongPref(Context context, String name, long def) {
        return getLongPref(context, Constant.LAUNCHER_PREF_FILE, name, def);
    }

    public static void setLongPref(Context context, String name, long value) {
        setLongPref(context, Constant.LAUNCHER_PREF_FILE, name, value);
    }

    public static float getFloatPref(Context context, String name, float def) {
        return getFloatPref(context, Constant.LAUNCHER_PREF_FILE, name, def);
    }

    public static void setFloatPref(Context context, String name, float value) {
        setFloatPref(context, Constant.LAUNCHER_PREF_FILE, name, value);
    }

    public static boolean containsPref(Context context, String name) {
        return containsPref(context, Constant.LAUNCHER_PREF_FILE, name);
    }

    public static String getStringPref(Context context, String prefName, String name, String def) {
        SharedPreferences prefs = getSharedPreferences(context,prefName, Context.MODE_PRIVATE);
        return prefs.getString(name, def);
    }

    public static void setStringPref(Context context, String prefName, String name, String value) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "setStringPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences.Editor editPrefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE)
                .edit();
        editPrefs.putString(name, value);
        editPrefs.commit();
    }

    public static void removePref(Context context, String prefName, String name) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "removePref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences.Editor editPrefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE)
                .edit();
        editPrefs.remove(name);
        editPrefs.commit();
    }

    public static int getIntPref(Context context, String prefName, String name, int def) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "getIntPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences prefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE);
        return prefs.getInt(name, def);
    }

    public static void setIntPref(Context context, String prefName, String name, int value) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "setIntPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences.Editor editPrefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE)
                .edit();
        editPrefs.putInt(name, value);
        editPrefs.commit();
    }

    public static boolean getBooleanPref(Context context, String prefName, String name,
            boolean def) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "getBooleanPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences prefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE);
        return prefs.getBoolean(name, def);
    }

    public static void setBooleanPref(Context context, String prefName, String name,
            boolean value) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "setBooleanPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences.Editor editPrefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE)
                .edit();
        editPrefs.putBoolean(name, value);
        editPrefs.commit();
    }

    public static long getLongPref(Context context, String prefName, String name, long def) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "getLongPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences prefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE);
        return prefs.getLong(name, def);
    }

    public static void setLongPref(Context context, String prefName, String name, long value) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "setLongPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences.Editor editPrefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE)
                .edit();
        editPrefs.putLong(name, value);
        editPrefs.commit();
    }

    public static float getFloatPref(Context context, String prefName, String name, float def) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "getFloatPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences prefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE);
        return prefs.getFloat(name, def);
    }

    public static void setFloatPref(Context context, String prefName, String name, float value) {
        if (DEBUG_LOG) {
            XLog.d(TAG, "setFloatPref " + name + ", " + System.currentTimeMillis());
        }
        SharedPreferences.Editor editPrefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE)
                .edit();
        editPrefs.putFloat(name, value);
        editPrefs.commit();
    }

    public static boolean containsPref(Context context, String prefName, String name) {
        SharedPreferences prefs = getSharedPreferences(context,
                prefName, Context.MODE_PRIVATE);
        return prefs.contains(name);
    }
}
