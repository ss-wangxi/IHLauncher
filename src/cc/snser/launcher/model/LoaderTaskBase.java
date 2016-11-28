package cc.snser.launcher.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.SystemClock;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.apps.ActionUtils;
import cc.snser.launcher.apps.model.HiddenApplication;
import cc.snser.launcher.model.LauncherModel.Callbacks;
import cc.snser.launcher.model.LauncherModelCommons.AppHideListColumnIndex;

import com.btime.launcher.util.XLog;

import java.net.URISyntaxException;
import java.util.ArrayList;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public abstract class LoaderTaskBase implements Runnable {
    private static final String TAG = "Launcher.Model.LoaderTask";

    protected final LauncherModel mLauncherModel;

    protected Context mContext;
    protected final boolean mIsLaunching;
    protected Handler mOnFinishHandler;
    protected final boolean mFlushAllApps;

    protected boolean mStopped;

    protected LoaderTaskBase(LauncherModel launcherModel, Context context, boolean isLaunching, Handler handler, boolean flushAllApps) {
        mLauncherModel = launcherModel;

        mContext = context;
        mIsLaunching = isLaunching;
        mOnFinishHandler = handler;
        mFlushAllApps = flushAllApps;
    }

    boolean isLaunching() {
        return mIsLaunching;
    }

    Context getContext() {
        return mContext;
    }

    Handler getOnFinishHandler() {
        return mOnFinishHandler;
    }

    void stopLocked() {
        synchronized (LoaderTaskBase.this) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "Loader Task is stopped");
            }
            mStopped = true;
            this.notify();
        }
    }

    /**
     * Gets the callbacks object.  If we've been stopped, or if the launcher object
     * has somehow been garbage collected, return null instead.  Pass in the Callbacks
     * object that was around when the deferred message was scheduled, and if there's
     * a new Callbacks object around then also return null.  This will save us from
     * calling onto it with data that will be ignored.
     */
    public Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
        synchronized (mLauncherModel.mLock) {
            if (mStopped) {
                return null;
            }

            if (mLauncherModel.mCallbacks == null) {
                return null;
            }

            final Callbacks callbacks = mLauncherModel.mCallbacks.get();
            if (callbacks != oldCallbacks) {
                return null;
            }
            if (callbacks == null) {
                XLog.w(TAG, "no mLauncherModel.mCallbacks");
                return null;
            }

            return callbacks;
        }
    }

    protected void loadHiddenApps() {
        mLauncherModel.mHiddenApplications.clear();

        final long t = LOGD_ENABLED ? SystemClock.uptimeMillis() : 0;

        final Context context = mContext;
        final ContentResolver contentResolver = context.getContentResolver();

        final ArrayList<Long> itemsToRemove = new ArrayList<Long>();

        final Cursor c = contentResolver.query(LauncherSettings.AppHideList.getContentUri(true), null,
                null, null, null);

        if (c == null) {
            return;
        }

        int count = 0;

        try {
            AppHideListColumnIndex index = new AppHideListColumnIndex(c);

            String intentDescription;
            Intent intent;
            HiddenApplication hiddenApplication;

            while (!mStopped && c.moveToNext()) {
                try {
                    hiddenApplication = new HiddenApplication();
                    hiddenApplication.id = c.getLong(index.idIndex);

                    intentDescription = c.getString(index.intentIndex);
                    try {
                        intent = ActionUtils.parseIntent(intentDescription);
                    } catch (URISyntaxException e) {
                        XLog.printStackTrace(e);
                        //XLog.w(TAG, "Incorrect intent description: " + intentDescription, e);
                        itemsToRemove.add(hiddenApplication.id);
                        continue;
                    }
                    hiddenApplication.intent = intent;

                    mLauncherModel.mHiddenApplications.add(hiddenApplication);
                    count++;
                } catch (Exception e) {
                    XLog.printStackTrace(e);
                    //XLog.w(TAG, "Hidden apps loading interrupted:", e);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (mStopped) {
            return;
        }

        if (!itemsToRemove.isEmpty()) {
        	for (long id : itemsToRemove) {
        		DbManager.deleteHiddenApplicationFromDatabase(context, id, false, true);
        	}
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "load all " + count + " hidden apps from db in " + (SystemClock.uptimeMillis() - t)
                    + "ms");
        }
    }
}

