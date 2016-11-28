package com.btime.launcher;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.btime.launcher.app.PackageEventReceiver;
import com.btime.launcher.report.ReportService;
import com.btime.launcher.util.AppWidgetUtils;
import com.btime.settings.externalcall.SettingsServiceHelper;

public class CarOSLauncherBase {
    private static CarOSLauncherBase sInstance;
    
    private PackageEventReceiver mPackageEventReceiver;
    
    //记一下状态，当前rom开机launcher会Loading两次，后续可以去掉这个标记位
    private boolean mReportInited = false;
    
    private CarOSLauncherBase() {
    }
    
    public static CarOSLauncherBase getInstance() {
        if (sInstance == null) {
            sInstance = new CarOSLauncherBase();
        }
        return sInstance;
    }
    
    public void init(Context context) {
        //只在主进程里初始化一次
        if (getProgressPid(context, context.getPackageName()) == android.os.Process.myPid()) {
            initAppWidget(context);
            //initWallpaper(context);
            SettingsServiceHelper.getIntance().init(context);
            registerReceiver(context);
        }
    }
    
    public void unInit(Context context) {
        //只在主进程里清理
        if (getProgressPid(context, context.getPackageName()) == android.os.Process.myPid()) {
            unregisterReceiver(context);
        }
    }
    
/*    private void initWallpaper(Context context) {
        try {
            WallpaperManager.getInstance(context).setResource(R.drawable.default_workspace_bg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    
    private void initAppWidget(Context context) {
        AppWidgetUtils.grantBindAppWidgetPermission(context, context.getPackageName());
    }
    
    private void registerReceiver(Context context) {
        context.registerReceiver(mPackageEventReceiver = new PackageEventReceiver(), PackageEventReceiver.INTENT_FILTER);
    }
    
    private void unregisterReceiver(Context context) {
        if (mPackageEventReceiver != null) {
            context.unregisterReceiver(mPackageEventReceiver);
            mPackageEventReceiver = null;
        }
    }
    
    public void initReport(Context context) {
        if (!mReportInited) {
            context.startService(new Intent(context, ReportService.class));
            mReportInited = true;
        }
    }
    
    public static int getProgressPid(Context context, String progressName) {
        if (!TextUtils.isEmpty(progressName)) {
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            final List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (RunningAppProcessInfo process : processes) {
                if (progressName.equals(process.processName) && process.pkgList != null) {
                    return process.pid;
                }
            }
        }
        return -1;
    }
    
    public void printStartLog() {
        
    }
    
}
