package cc.snser.launcher;

import com.btime.launcher.util.XLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * 桌面在活着的时候，侦听这个类中提供的广播。
 * 所有需要在桌面全生命周期内侦听的广播，都应该注册在这里
 * @author yangkai
 *
 */
public class LauncherReceiver extends BroadcastReceiver {

    private static final String TAG = "Launcher.LauncherReceiver";
    private static final int   MESSAGE_WALLPAPER_CHANGED = 1000;

    private final Launcher mLauncher;
    
    private long mLastWallpaperChanged;
    private boolean mWaitForNextAction;

    LauncherReceiver(Launcher launcher) {
        mLauncher = launcher;
        
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Receive action: " + action);
        }
        try {
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                mLauncher.closeSystemDialogs();

            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            	mLauncher.onScreenOn();

            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mLauncher.onScreenOff();

            } 
        } catch (Throwable e) {
            XLog.e(TAG, "Failed to handle the action: " + action, e);
        }
    }

    /**
     * 取得桌面生命周内需要侦听的广播
     * @return
     */
    IntentFilter getLauncherIntentFilter() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        
        /*
         * 监听电池状态
         * */
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        
        /*
         * wifi
         */
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return filter;
    }
}
