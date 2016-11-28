package com.btime.launcher.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppServiceReceiver extends BroadcastReceiver {

    private static final String ACTION_START_APP = "com.caros.launcher.action.START_APP";
    private static final String EXTRA_APP_TYPE = "com.caros.launcher.extra.APP_TYPE";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final int appTypeCode = intent.getIntExtra(EXTRA_APP_TYPE, -1);
        if (ACTION_START_APP.equals(action)) {
            final AppType appType = filterAppTypeCode(appTypeCode);
            if (appType != null) {
                AppController.getInstance().startApp(appType);
            }
        }
    }
    
    private AppType filterAppTypeCode(int appType) {
        if (appType == AppType.TYPE_SETTINGS_WLAN_ON.value()) {
            return AppType.TYPE_SETTINGS_WLAN_ON;
        }
        return null;
    }

}
