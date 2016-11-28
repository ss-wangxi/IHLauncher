
package cc.snser.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import cc.snser.launcher.ui.utils.PrefUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.PathUtils;

import java.io.File;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * 桌面启动时候初始化用线程
 * @author yangkai
 */
public class LauncherCreateThread extends Thread {

    private static final String TAG = "Launcher.LauncherCreateThread";

    private Context mContext;

    private Launcher mLauncher;

    private boolean mFirstStart;

    private Object mLoadWallpaperLock = new Object();

    public LauncherCreateThread(Context context) {
        super("launcher-create-thread");
        mContext = context;
    }

    public void startLoadWallpaper(Launcher launcher, boolean firstStart) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "startLoadWallpaper");
        }

        synchronized (mLoadWallpaperLock) {
            mLauncher = launcher;
            mFirstStart = firstStart;

            mLoadWallpaperLock.notify();
        }
    }

    private boolean mStopped = false;

    public void shutDown() {
        if (mStopped) {
            return;
        }

        synchronized (mLoadWallpaperLock) {
            mLoadWallpaperLock.notify();
        }

        mStopped = true;
    }

    @Override
    public void run() {
        prepareFreference(mContext);

//        while (!mStopped) {
//            if (LOGD_ENABLED) {
//                XLog.d(TAG, "run  while (true)");
//            }
//        }

//        mLauncher.runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                mLauncher.getModel().regist(mApp);
//            }
//        });
        checkNoMediaFile();

        mStopped = true;
    }

    /**
     * check if no media file exist, if not we new one
     */
    private static void checkNoMediaFile() {
        try {
            if (Utils.isExternalStorageWritable()) {
                // add a no-image file
                String noMediaPath = PathUtils.getLauncherExternalStoreBase(".nomedia");
                File noMediaFile = new File(noMediaPath);
                if (!noMediaFile.exists()) {
                    noMediaFile.mkdirs();
                    noMediaFile.createNewFile();
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void prepareFreference(Context context) {
        //SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences pref = PrefUtils.getSharedPreferences(context, Constant.LAUNCHER_PREF_FILE, Context.MODE_PRIVATE);
        pref.getString("null", "");
    }
}