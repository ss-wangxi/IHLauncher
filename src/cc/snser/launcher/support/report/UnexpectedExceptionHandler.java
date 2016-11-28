
package cc.snser.launcher.support.report;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import cc.snser.launcher.App;
import cc.snser.launcher.Constant;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.ui.utils.PrefConstants;
import cc.snser.launcher.ui.utils.PrefUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.NotificationUtils;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class UnexpectedExceptionHandler implements UncaughtExceptionHandler {
    private static final String TAG = "Launcher.UnexpectedExceptionHandler";

    // temporary we think 5 times restart launcher in 5m is real big error, no
    // need to restart
    private static final long RESTART_CHECK_INTERVAL = 5 * 60 * 1000;

    private static final int RESTART_COUNT_LIMIT = 5;

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private Context mContext;
    
    private static BroadcastReceiver mReceiver;
    static {
    	mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
			}
    		
    	};
    }

    public UnexpectedExceptionHandler() {
    }

    public void init(Context context) {
        this.mContext = App.getApp();
        
        IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);    	
    	mContext.registerReceiver(mReceiver, intentFilter);

        try {
            UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

            if (defaultHandler == null || !defaultHandler.getClass().equals(UnexpectedExceptionHandler.class)) {
                if (LOGD_ENABLED) {
                    XLog.d(TAG, "UnexpectedExceptionHandler is set to " + this);
                }
                mDefaultHandler = defaultHandler;
                Thread.setDefaultUncaughtExceptionHandler(this);
            }
        } catch (Exception e) {
            XLog.e(TAG, "Error while setting the default uncaught exception handler", e);
        }
    }

    public void destroy() {
        try {
            if (mDefaultHandler != null) {
                Thread.setDefaultUncaughtExceptionHandler(mDefaultHandler);
                mDefaultHandler = null;
            }
        } catch (Exception e) {
            XLog.e(TAG, "Error while setting the default uncaught exception handler", e);
        }
        
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // ignore
        }
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            StatManager.handleException(mContext, throwable);
            // 取消所有的notification
            NotificationUtils.cancelAllNotificationBeforeKilled(mContext);

            if (thread != null && thread.getName() != null && thread.getName().startsWith(Constant.DYNAMIC_THEME_THREAD_PREFIX)) {
                // 动态主题的线程出现异常,不做处理,以免Launcher被强制关闭
                return;
            }
            
            if (Constant.CATCH_UNEXPECTED_EXCEPTION) {
                if (needRestartLauncher()) {
                    Intent intent = new Intent(mContext, Launcher.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                } else if (mContext instanceof Activity) {
                    ((Activity) mContext).finish();
                }

                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            } else {
                if (mDefaultHandler != null) {
                    mDefaultHandler.uncaughtException(thread, throwable);
                }
            }
        } catch (Throwable e) {
            // should never happen
            XLog.e(TAG, "Unexpected error", e);
        }
    }
    
    private boolean needRestartLauncher() {
        long lastExceptionTIme = PrefUtils.getLongPref(mContext, PrefConstants.KEY_LAST_EXCEPTION_TIME, -1);
        if (lastExceptionTIme >= 0) {
            long now = System.currentTimeMillis();
            long diff = now - lastExceptionTIme;
            if (diff > 0 && diff < RESTART_CHECK_INTERVAL) {
                int count = PrefUtils.getIntPref(mContext, PrefConstants.KEY_EXCEPTION_COUNT, 0);

                if (++count > RESTART_COUNT_LIMIT) {
                    return false;
                } else {
                    PrefUtils.setIntPref(mContext, PrefConstants.KEY_EXCEPTION_COUNT, count);
                }
            } else {
                initCounting();
            }
        } else {
            initCounting();
        }
        return true;
    }

    private void initCounting() {
        PrefUtils.setLongPref(mContext, PrefConstants.KEY_LAST_EXCEPTION_TIME, System.currentTimeMillis());
        PrefUtils.setIntPref(mContext, PrefConstants.KEY_EXCEPTION_COUNT, 0);
    }
    
    private boolean isAdbdRestored(){
        String adbRestoreFlag = mContext.getFilesDir().getAbsolutePath() + "/.adbdrestored";
        File adbRestoreFlaggFile = new File(adbRestoreFlag);
        if(adbRestoreFlaggFile.exists()){
            return true;
        }
        else{
            return false;
        }
    }

}
