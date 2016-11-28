package cc.snser.launcher.iphone.model;


import android.content.Context;
import cc.snser.launcher.App;
import cc.snser.launcher.Constant;
import cc.snser.launcher.DeferredHandler;
import cc.snser.launcher.IconFsCache;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.iphone.model.LauncherModelIphone.AllAppsList;
import cc.snser.launcher.iphone.model.LauncherModelIphone.AllShortcutsList;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.model.PackageUpdatedTaskBase;
import cc.snser.launcher.model.LauncherModel.Callbacks;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.StringUtils;

import java.util.ArrayList;

/**
 * 将PackageUpdatedTask从LauncherModel中拆解出来
 * <p><b>注：该类是工作在Launcher-Daemon线程中的代码</b></p>
 * @author zhangjing
 *
 */
public class PackageUpdatedTask extends PackageUpdatedTaskBase {

    private static final String TAG = "Launcher.Model.PackageUpdatedTask";

    private static final boolean DEBUG_LOADERS = Constant.LOGD_ENABLED;
    private IconFsCache mIconFsCache;
    private DeferredHandler mDeferHandler;
    private LauncherModelIphone mLauncherModel;

    public PackageUpdatedTask(Context context, int op, final String[] packages, LauncherModelIphone launcherModel) {
        super(launcherModel, op, packages);
        this.mLauncherModel = launcherModel;
        mIconFsCache = IconFsCache.getInstance(context);
        mDeferHandler = mLauncherModel.getHandler();
    }

    @Override
    public void run() {
        try {
            final Callbacks callbacks = getLauncherModelCallback();
            if (callbacks == null) {
                XLog.w(TAG, "Nobody to tell about the new app. Launcher is probably loading.");
                return;
            }
            updateModel();
            updateDbAndNotify(callbacks);
            super.commonRun();
        } catch (Exception e) {
            XLog.e(TAG, "Failed to process the PackageUpdatedTaskIphone.", e);
        }
    }

    private Callbacks getLauncherModelCallback() {
        return mLauncherModel.getCallbacks();
    }

    private void updateModel() {
        AllAppsList allAppsList = mLauncherModel.getAllAppsList();
        AllShortcutsList allShortcutsList = mLauncherModel.getAllShortcutsList();
        final App context = mLauncherModel.mApp;

        final String[] packages = mPackages;
        final int n = packages.length;

        switch (mOp) {
            case OP_ADD:
                for (int i = 0; i < n; i++) {
                    if (DEBUG_LOADERS) {
                        XLog.d(TAG, "begins mAllAppsList.addPackage " + packages[i]);
                    }

                    //是一个推荐位，则尝试查找桌面上已有的推荐位图标并记录下来
                    HomeDesktopItemInfo adItemInfo = allShortcutsList.findAdComponent(packages[i]);

                    if (adItemInfo != null) {
                        if (DEBUG_LOADERS) {
                            XLog.d(TAG, "find replacable ad shortcut" + adItemInfo.getIntent());
                        }
                    }

                    if (adItemInfo != null) {
                        if (DEBUG_LOADERS) {
                            XLog.d(TAG, "find replacable ad shortcut" + adItemInfo.getIntent());
                        }
                    }

                    allAppsList.addPackage(context, packages[i], adItemInfo);//将itemInfo变换后添加到allAppsList里

                    if (DEBUG_LOADERS) {
                        XLog.d(TAG, "ends mAllAppsList.addPackage " + packages[i]);
                    }
                }
                break;

            case OP_UPDATE:
                for (int i = 0; i < n; i++) {
                    if (DEBUG_LOADERS) {
                        XLog.d(TAG, "begins mAllAppsList.updatePackage " + packages[i]);
                    }
                    allAppsList.updatePackage(context, packages[i]);
                    if (DEBUG_LOADERS) {
                        XLog.d(TAG, "ends mAllAppsList.updatePackage " + packages[i]);
                    }
                }
                break;

            case OP_REMOVE:
                for (int i = 0; i < n; i++) {
                    if (DEBUG_LOADERS) {
                        XLog.d(TAG, "begins mAllAppsList.removePackage " + packages[i]);
                    }
                    allAppsList.removePackage(packages[i]);
                    allShortcutsList.removePackage(packages[i]);
                    if (DEBUG_LOADERS) {
                        XLog.d(TAG, "ends mAllAppsList.removePackage " + packages[i]);
                    }
                }
                break;

            case OP_AVAILABLE:
                if (DEBUG_LOADERS) {
                    XLog.d(TAG, "begins mAllAppsList.availablePackages " + StringUtils.join(packages, ","));
                }
                allAppsList.availablePackages(context, packages);
                if (DEBUG_LOADERS) {
                    XLog.d(TAG, "ends mAllAppsList.availablePackages " + StringUtils.join(packages, ","));
                }
                break;
        }
    }

    /**
     * TODO 在Available时有可能出现既删除又添加的情况。这里应该先删除并重新布局，再进行添加，以及删除空屏的操作
     * @param callbacks
     */
    private void updateDbAndNotify(final Callbacks callbacks) {
        AllAppsList allAppsList = mLauncherModel.getAllAppsList();
        AllShortcutsList allShortcutsList = mLauncherModel.getAllShortcutsList();
        final App context = mLauncherModel.mApp;

        ArrayList<HomeDesktopItemInfo> added = null;
        ArrayList<HomeDesktopItemInfo> removed = null;
        ArrayList<HomeDesktopItemInfo> modified = null;
        ArrayList<HomeDesktopItemInfo> availabled = null;
        ArrayList<HomeDesktopItemInfo> removedShortcut = null;

        if (allShortcutsList.removed.size() > 0) {
            removedShortcut = allShortcutsList.removed;
            allShortcutsList.removed = new ArrayList<HomeDesktopItemInfo>();
        }

        if (allAppsList.added.size() > 0) {
            added = allAppsList.added;
            allAppsList.added = new ArrayList<HomeDesktopItemInfo>();
        }
        if (allAppsList.removed.size() > 0) {
            removed = allAppsList.removed;
            allAppsList.removed = new ArrayList<HomeDesktopItemInfo>();
        }
        if (allAppsList.modified.size() > 0) {
            modified = allAppsList.modified;
            allAppsList.modified = new ArrayList<HomeDesktopItemInfo>();
        }
        if (allAppsList.availabled.size() > 0) {
            availabled = allAppsList.availabled;
            allAppsList.availabled = new ArrayList<HomeDesktopItemInfo>();
        }

        if (removedShortcut != null) {
            final ArrayList<HomeDesktopItemInfo> removedFinal = removedShortcut;

            mDeferHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callbacks == getLauncherModelCallback()) {
                        callbacks.bindAppsRemoved(removedFinal, true);
                        ((Launcher) callbacks).bindShortcutRemoved(removedFinal);
                    }
                }
            });
        }

        if (removed != null) {
            final boolean permanent = mOp != OP_UNAVAILABLE;
            final ArrayList<HomeDesktopItemInfo> removedFinal = removed;

            //add comment by ssy:这里是移除已经隐藏了的数据
            for (HomeDesktopItemInfo info : removedFinal) {
                mLauncherModel.syncRemoveApp(info.intent.getComponent());
            }

            //通知UI更新
            mDeferHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callbacks == getLauncherModelCallback()) {
                        callbacks.bindAppsRemoved(removedFinal, permanent);
                    }
                }
            });
            mIconFsCache.deleteCacheFiles(removedFinal);
        }

        if (added != null) {
            final ArrayList<HomeDesktopItemInfo> addedFinal = added;
            mDeferHandler.post(new Runnable() {
                @Override
                public void run() {
                    //替换桌面上推荐位的图标
                    if (callbacks == getLauncherModelCallback()) {
                        callbacks.bindNewInstalledApps(addedFinal);
                        callbacks.bindAppsAdded(addedFinal, false, true);
                    }
                }
            });
            mIconFsCache.saveIconsToCacheFile(addedFinal);
        }

        if (modified != null) {
            final ArrayList<HomeDesktopItemInfo> modifiedFinal = modified;
            mDeferHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callbacks == getLauncherModelCallback()) {
                        callbacks.bindAppsUpdated(modifiedFinal, null);
                    }
                }
            });
            mIconFsCache.saveIconsToCacheFile(modifiedFinal);
            for (HomeDesktopItemInfo item : modifiedFinal) {
                DbManager.updateItemInDatabase(context, item);
            }
        }

        if (availabled != null) {
            final ArrayList<HomeDesktopItemInfo> availabledFinal = availabled;
            mDeferHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callbacks == getLauncherModelCallback()) {
                        callbacks.bindAppsUpdated(availabledFinal, null);
                    }
                }
            });
            for (HomeDesktopItemInfo item : availabled) {
                // item.storage = Constant.APPLICATION_EXTERNAL;
                DbManager.updateItemInDatabase(context, item);
            }
        }
    }

}