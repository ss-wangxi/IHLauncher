package com.btime.launcher.util;

import java.lang.reflect.Method;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public class AppWidgetUtils {
    public static boolean hasBindAppWidgetPermission(Context context, String packageName) {
        boolean hasPermission = false;
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            Method method = manager.getClass().getMethod("hasBindAppWidgetPermission", String.class);
            Object ret = method.invoke(manager, packageName);
            if (ret instanceof Boolean) {
                hasPermission = (boolean)ret;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasPermission;
    }
    
    public static void setBindAppWidgetPermission(Context context, String packageName, boolean permission) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            Method method = manager.getClass().getMethod("setBindAppWidgetPermission", String.class, boolean.class);
            method.invoke(manager, packageName, permission);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void grantBindAppWidgetPermission(Context context, String packageName) {
        if (!hasBindAppWidgetPermission(context, packageName)) {
            setBindAppWidgetPermission(context, packageName, true);
        }
    }

}
