package cc.snser.launcher.model;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import cc.snser.launcher.App;
import cc.snser.launcher.DeferredHandler;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.apps.model.HiddenApplication;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.apps.model.workspace.LauncherAppWidgetInfo;
import cc.snser.launcher.apps.model.workspace.LauncherWidgetViewInfo;

import com.btime.launcher.util.XLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public abstract class LauncherModel {

    private static final boolean DEBUG_LOADERS = LOGD_ENABLED;
    public static final int MSG_LOADER_TASK_FINISHED = 0;
    public static final int MSG_HOME_CLASSIGY = 10;
    private static final String TAG = "Launcher.Model";
    
    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    // We start off with everything not loaded.  After that, we assume that
    // our monitoring of the package manager provides all updates and we never
    // need to do a requery.  These are only ever touched from the loader thread.
    public boolean mAllAppsLoaded;

    public final ArrayList<HiddenApplication> mHiddenApplications = new ArrayList<HiddenApplication>();

    public WeakReference<Callbacks> mCallbacks;

    public final App mApp;
    protected final DeferredHandler mHandler;
    protected final Object mLock = new Object();

    public LoaderTaskBase mLoaderTask;

    // TODO: 重构：考虑所有方法改名：加上mcb前缀(Model Callback)

    public static interface Callbacks {
        public boolean setLoadOnResume();
        public void setIgnoreOnResumeNeedsLoad(boolean ignoreOnResumeNeedsLoad);
        public int getCurrentWorkspaceScreen();
        public void startBindingInHome();
        public void bindItems(ArrayList<HomeItemInfo> shortcuts, int start, int end);
        public void bindAppWidget(LauncherAppWidgetInfo info, boolean forceUpdateToDb);
        public void bindWidgetView(LauncherWidgetViewInfo info, boolean forceUpdateToDb);
        public void finishBindingInHome();
        public void bindAppsRemoved(List<? extends AppInfo> apps, boolean permanent);
        public void bindAppsAdded(List<? extends HomeItemInfo> apps, boolean currentScreen, boolean classify);
        public void bindAppsUpdated(List<? extends AppInfo> apps, Map<ComponentName, ComponentName> modifiedMapping);
        public void bindNewInstalledApps(List<? extends AppInfo> apps);
        public boolean isFirst();
        public int  getCurrentScreens();
        public void addScreen();
        //TODO 单层板添加
        /**
         * 在UI线程上下文中调用
         * @param homeItemInfos
         */
        public void bindMissedItem(ArrayList<? extends HomeItemInfo> homeItemInfos);
        public void onFolderScreensAdded(ArrayList<Integer> screenIdxs);
    }

    private LauncherModelReceiver mLauncherModelReceiver;
    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private ContentObserver mFavoritesObserver;

    public LauncherModel(App app, IconCache iconCache, DeferredHandler handler) {
        mApp = app;
        mHandler = handler;
    }

    public void register() {
        if (mLauncherModelReceiver == null) {
            mLauncherModelReceiver = new LauncherModelReceiver(this);

            try {
                // Register intent receivers
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_PACKAGE_ADDED);
                filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
/*                if (!(this instanceof SecondLayerLauncherModel)) {
                	filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
                }*/
                filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
                filter.addDataScheme("package");
                //Snser定制launcher，暂不监听app安装/卸载/更新广播 added by snsermail@gmail.com
                //mApp.registerReceiver(mLauncherModelReceiver, filter);
            } catch (Throwable e) {
                // ignore
            }
            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
                filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
                //Snser定制launcher，暂不监听app安装/卸载/更新广播 added by snsermail@gmail.com
                //mApp.registerReceiver(mLauncherModelReceiver, filter);
            } catch (Throwable e) {
                // ignore
            }
        }

        if (mFavoritesObserver == null) {
            mFavoritesObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    loadWorkspace(App.getApp(), null);
                }
            };

            try {
                // Register for changes to the favorites
            	ContentResolver resolver = mApp.getContentResolver();
            	resolver.registerContentObserver(LauncherSettings.Favorites.getContentUri(mApp, true), 
            			true, mFavoritesObserver);
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    public void onTerminate(App app) {
        if (mLauncherModelReceiver != null) {
            try {
                app.unregisterReceiver(mLauncherModelReceiver);
            } catch (Throwable e) {
                // ignore
            }
        }

        if (mFavoritesObserver != null) {
            try {
                ContentResolver resolver = app.getContentResolver();
                resolver.unregisterContentObserver(mFavoritesObserver);
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            //Callbacks previous = mCallbacks == null ? null : mCallbacks.get();
            //if (LOGD_ENABLED) {
              //  XLog.d(TAG, "initialize previous: " + previous + ", new: " + callbacks);
            //}
            //if (previous instanceof Launcher && previous != callbacks) {
                //((Launcher) previous).finish();
            //}
            mCallbacks = new WeakReference<Callbacks>(callbacks);
        }
    }

    public void addHiddenApplication(HiddenApplication hiddenApplication, boolean sync) {
        mHiddenApplications.add(hiddenApplication);
        DbManager.addHiddenApplicationToDatabase(mApp, hiddenApplication, false, sync);
    }

    public void syncRemoveApp(ComponentName component) {
        removeHiddenApplication(component, true);
    }

    public void removeHiddenApplication(ComponentName component, boolean sync) {
        final List<HiddenApplication> hiddenApplications = this.mHiddenApplications;
        for (int i = hiddenApplications.size() - 1; i >= 0; i--) {
            HiddenApplication hiddenApplication = hiddenApplications.get(i);
            if (hiddenApplication.intent.getComponent().equals(component)) {
                hiddenApplications.remove(i);
                DbManager.deleteHiddenApplicationFromDatabase(mApp, hiddenApplication.id, false, sync);
            }
        }
    }

    public boolean isApplicationHidden(ComponentName component) {
        final List<HiddenApplication> hiddenApplications = this.mHiddenApplications;
        for (int i = hiddenApplications.size() - 1; i >= 0; i--) {
            HiddenApplication hiddenApplication = hiddenApplications.get(i);
            if (hiddenApplication.intent.getComponent().equals(component)) {
                return true;
            }
        }
        return false;
    }

    private List<PackageUpdatedTaskBase> tasks = new ArrayList<PackageUpdatedTaskBase>();

    public void excuteAllPackageUpdate() {
        Iterator<PackageUpdatedTaskBase> itor = tasks.iterator();
        while (itor.hasNext()) {
            PackageUpdatedTaskBase task = itor.next();
           DaemonThread.postThreadTask(task);
           itor.remove();
        }
    }

    public void enqueuePackageUpdated(PackageUpdatedTaskBase task) {
        if (!mAllAppsLoaded) {
            tasks.add(task);
        } else {
            DaemonThread.postThreadTask(task);
        }
    }

    public void startLoader(Context context, boolean isLaunching, boolean flushAllApps) {
        startLoader(context, isLaunching, null, true, false, flushAllApps);
    }

    public void startLoader(Context context, Handler handler, boolean flushAllApps) {
        startLoader(context, false, handler, true, false, flushAllApps);
    }

    public void loadWorkspace(Context context, Handler handler) {
        startLoader(context, false, handler, true, false, false);
    }

    public void loadAllApps(Context context) {
        startLoader(context, false, null, false, true, false);
    }

    private void startLoader(Context context, boolean isLaunching, Handler handler,
            boolean loadWorkspace, boolean loadAllApps, boolean flushAllApps) {
        synchronized (mLock) {
            if (DEBUG_LOADERS) {
                XLog.d(TAG, "startLoader isLaunching=" + isLaunching + ",loadWorkspace=" + loadWorkspace
                    + ",loadAllApps=" + loadAllApps + ",flushAllApps="
                    + flushAllApps + ",mCallbacks=" + mCallbacks + ",mLoaderTask=" + mLoaderTask);
            }

            // Don't bother to start the thread if we know it's not going to do anything
            if (loadAllApps || (mCallbacks != null && mCallbacks.get() != null)) {
                // If there is already one running, tell it to stop.
                LoaderTaskBase oldTask = mLoaderTask;
                if (oldTask != null) {
                    if (oldTask.isLaunching()) {
                        // don't downgrade isLaunching if we're already running
                        isLaunching = true;
                    }
                    if (oldTask.getOnFinishHandler() != null) {
                        handler = oldTask.getOnFinishHandler();
                    }
                    oldTask.stopLocked();
                }

                mLoaderTask = createLoaderTask(context, isLaunching, handler, flushAllApps);
                DaemonThread.postThreadTask(mLoaderTask);
            }
        }
    }

    public boolean stopLoader(Context context) {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                if (context == mLoaderTask.getContext()) {
                    mLoaderTask.stopLocked();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isLoading() {
        synchronized (mLock) {
            return mLoaderTask != null;
        }
    }

    protected abstract LoaderTaskBase createLoaderTask(Context context, boolean isLaunching, Handler handler, boolean flushAllApps);
    protected abstract PackageUpdatedTaskBase createPackageUpdatedTask(int op, String[] packages);

    public static final Comparator<AppInfo> APP_NAME_COMPARATOR = new Comparator<AppInfo>() {
        @Override
        public final int compare(AppInfo a, AppInfo b) {
        	//bug:java7 reference：http://dertompson.com/2012/11/23/sort-algorithm-changes-in-java-7/ 
        	if(a.getTitle() == null && b.getTitle() == null){
        		return 0;
        	}
        	
        	if(a.getTitle() == null) return -1;
        	if(b.getTitle() == null) return 1;
        	
            return Utils.COLLATOR.compare(a.getTitle(), b.getTitle());
        }
    };
    
    public static final Comparator<AppInfo> APP_LASTUSETIME_COMPARATOR = new Comparator<AppInfo>(){
    	@Override
    	public final int compare(AppInfo lhs, AppInfo rhs){
    		int lhsCalledNum = lhs.getCalledNum();
            int rhsCalledNum = rhs.getCalledNum();
            if (lhsCalledNum < rhsCalledNum) {
                return 1;
            } else if (lhsCalledNum > rhsCalledNum) {
                return -1;
            } else {
                long lhsLastCallTime = lhs.getLastCalledTime();
                long rhsLastCallTime = rhs.getLastCalledTime();
                if (lhsLastCallTime < rhsLastCallTime) {
                    return 1;
                } else if (lhsLastCallTime > rhsLastCallTime) {
                    return -1;
                } else {
                    return 0;
                }
            }
    	}
    };

    public abstract void removeApplicationInfo(Context context, AppInfo appInfo);

    public abstract void updateAppCalledNum(Intent intent);

    public abstract AppInfo getApplicationInfo(Intent intent);

    public abstract AppInfo getApplicationInfo(ComponentName componentName);

    public abstract ArrayList<AppInfo> getAllApplicationInfos();

    public abstract ArrayList<AppInfo> getAllVisibleApplicationInfos();

    public abstract ArrayList<AppInfo> getAllDesktopVisibleApplicationInfos();

    public abstract int getAllApplicationInfosCount() ;

    public abstract int getAppWidgetHostId();

    public abstract Uri getContentAppWidgetResetUri();
    
    protected static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }
}