package com.btime.launcher.app;

import android.content.Intent;
import cc.snser.launcher.App;

public class BroadcastHelper {
    private BroadcastHelper() {
    }
    
    private static class SingletonHolder {
        public static BroadcastHelper sInstance = new BroadcastHelper();
    }
    
    public static BroadcastHelper getIntance() {
        return SingletonHolder.sInstance;
    }
    
    public boolean filterStartIntent(AppType type) {
        return false;
    }
    
    public boolean sendBroadcast(Intent intent) {
        App.getAppContext().sendBroadcast(intent);
        return true;
    }
}
