package com.btime.launcher.app;

import java.util.ArrayList;
import java.util.List;

import com.btime.launcher.util.XLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PackageEventReceiver extends BroadcastReceiver {
    
    public static final IntentFilter INTENT_FILTER = new IntentFilter();
    public static final String ACTION_PACKAGE_DIED = "android.intent.action.PACKAGE_DIED"; 
    
    static {
        INTENT_FILTER.addAction(Intent.ACTION_PACKAGE_ADDED);
        INTENT_FILTER.addAction(Intent.ACTION_PACKAGE_REMOVED);
        INTENT_FILTER.addAction(Intent.ACTION_PACKAGE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_PACKAGE_REPLACED);
        INTENT_FILTER.addAction(PackageEventReceiver.ACTION_PACKAGE_DIED);
        INTENT_FILTER.addDataScheme("package");
    }
    
    private static List<IPackageEventCallback> sCallbacks = new ArrayList<IPackageEventCallback>();
    
    public static interface IPackageEventCallback {
        public void onPackageEvent(Context context, String action, String pkgname);
    }
    
    public static void registerPackageEventCallback(IPackageEventCallback callback) {
        if (callback != null) {
            synchronized (sCallbacks) {
                sCallbacks.add(callback);
            }
        }
    }
    
    public static void unregisterPackageEventCallback(IPackageEventCallback callback) {
        if (callback != null) {
            synchronized (sCallbacks) {
                sCallbacks.remove(callback);
            }
        }
    }
    
    private static void notifyPackageEventCallback(Context context, String action, String pkg) {
        synchronized (sCallbacks) {
            for (IPackageEventCallback callback : sCallbacks) {
                if (callback != null) {
                    callback.onPackageEvent(context, action, pkg);
                }
            }
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String pkg = intent.getData() == null ? null : intent.getData().getSchemeSpecificPart();
        if (action != null && pkg != null) {
            switch (action) {
                case Intent.ACTION_PACKAGE_ADDED:
                    onPackageAdded(context, pkg);
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    onPackageRemoved(context, pkg);
                    break;
                case Intent.ACTION_PACKAGE_CHANGED:
                    onPackageChanged(context, pkg);
                    break;
                case Intent.ACTION_PACKAGE_REPLACED:
                    onPackageReplaced(context, pkg);
                    break;
                case PackageEventReceiver.ACTION_PACKAGE_DIED:
                    onPackageDied(context, pkg);
                    break;
                default:
                    break;
            }
        }
    }
    
    private void onPackageAdded(Context context, String pkg) {
        XLog.d("Snser", "PackageEventReceiver onPackageAdded pkg=" + pkg);
        notifyPackageEventCallback(context, Intent.ACTION_PACKAGE_ADDED, pkg);
    }
    
    private void onPackageRemoved(Context context, String pkg) {
        XLog.d("Snser", "PackageEventReceiver onPackageRemoved pkg=" + pkg);
        notifyPackageEventCallback(context, Intent.ACTION_PACKAGE_REMOVED, pkg);
    }
    
    private void onPackageChanged(Context context, String pkg) {
        XLog.d("Snser", "PackageEventReceiver onPackageChanged pkg=" + pkg);
        notifyPackageEventCallback(context, Intent.ACTION_PACKAGE_CHANGED, pkg);
    }
    
    private void onPackageReplaced(Context context, String pkg) {
        XLog.d("Snser", "PackageEventReceiver onPackageReplaced pkg=" + pkg);
        notifyPackageEventCallback(context, Intent.ACTION_PACKAGE_REPLACED, pkg);
    }
    
    private void onPackageDied(Context context, String pkg) {
        XLog.d("Snser", "PackageEventReceiver onPackageDied pkg=" + pkg);
        notifyPackageEventCallback(context, PackageEventReceiver.ACTION_PACKAGE_DIED, pkg);
    }

}
