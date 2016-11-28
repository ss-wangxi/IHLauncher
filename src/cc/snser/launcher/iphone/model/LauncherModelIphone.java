package cc.snser.launcher.iphone.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import cc.snser.launcher.App;
import cc.snser.launcher.Constant;
import cc.snser.launcher.DeferredHandler;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.IconFsCache;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.model.ItemInfoLoader;
import cc.snser.launcher.model.LauncherModel;
import cc.snser.launcher.model.PackageUpdatedTaskBase;

import com.btime.launcher.util.XLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGW_ENABLED;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public class LauncherModelIphone extends LauncherModel {
    public static final String TAG = "Launcher.Model";

    // We start off with everything not loaded.  After that, we assume that
    // our monitoring of the package manager provides all updates and we never
    // need to do a requery.  These are only ever touched from the loader thread.

    protected AllAppsList mAllAppsList;
    protected AllShortcutsList mAllShortcutsList;
    protected DataModel mDataModel;

    final Object mLock = new Object();
    private final ItemInfoLoader mItemInfoLoader;
    /**Model更新使用的lock*/
    private final Object mModelLock = new Object();

    public LauncherModelIphone(App app, IconCache iconCache, DeferredHandler handler) {
        super(app, iconCache, handler);

        mAllAppsList = new AllAppsList(app);
        mAllShortcutsList = new AllShortcutsList(app);
        mDataModel = new DataModel();
        mItemInfoLoader = ItemInfoLoader.getInstance(app);
    }

    public void updateAppCalledNum(Intent intent) {
        if (intent == null) {
            return;
        }

        ComponentName componentName = intent.getComponent();

        if (componentName == null) {
            final PackageManager packageManager = mApp.getPackageManager();

            ResolveInfo bestMatch = packageManager.resolveActivity(intent, 0);

            if (bestMatch != null) {
                if (LOGD_ENABLED) {
                    XLog.d(TAG, "Find best match for intent: " + intent + " - " + bestMatch.activityInfo.packageName + "," + bestMatch.activityInfo.name);
                }
                final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                mainIntent.setPackage(bestMatch.activityInfo.packageName);

                ResolveInfo match = packageManager.resolveActivity(mainIntent, 0);

                if (match != null && match.activityInfo.packageName.equals(bestMatch.activityInfo.packageName)) {
                    if (LOGD_ENABLED) {
                        XLog.d(TAG, "Find best main match for intent: " + intent + " - " + match.activityInfo.packageName + "," + match.activityInfo.name);
                    }
                    bestMatch = match;
                }

                componentName = new ComponentName(bestMatch.activityInfo.packageName,
                        bestMatch.activityInfo.name);
            }
        }

        if (componentName == null) {
            return;
        }

        HomeItemInfo hii = this.getApplicationInfo(componentName);

        if (!(hii instanceof HomeDesktopItemInfo)) {
            return;
        }

        final HomeDesktopItemInfo applicationInfo = (HomeDesktopItemInfo) hii;
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Increase usage for component: " + applicationInfo.intent);
        }

        applicationInfo.updateInvoke();

        DbManager.updateItemInDatabase(mApp, applicationInfo);
        
      //统计打开应用次数
//        StatManager.getInstance().sendStatEvent(StatManager.EVENT_ID_OPEN_APP, null);
    }

    /**当前返回应用或快捷方式。UserFolderIcon中会调用此方法来获取到对应的对象，在创建文件夹时从原有容器中删除*/
    public HomeDesktopItemInfo getApplicationInfo(Intent intent) {
        return mDataModel.findApp(intent);
//        return mAllAppsList.findApplicationInfoLocked(intent);
    }

    public HomeDesktopItemInfo getApplicationInfo(ComponentName componentName) {
        return mDataModel.findApp(componentName);
//        return mAllAppsList.findApplicationInfoLocked(componentName);
    }

    public ArrayList<AppInfo> getAllApplicationInfos() {
        ArrayList<AppInfo> list = new ArrayList<AppInfo>(mDataModel.mAllApplications);
        Iterator<AppInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            AppInfo app = iterator.next();
            if (((HomeDesktopItemInfo) app).isShortcut()) {
                iterator.remove();
            }
        }
        return list;
    }

    public int getAllApplicationInfosCount() {
        return mAllAppsList.data.size();
    }

    public ArrayList<AppInfo> getAllVisibleApplicationInfos() {
        ArrayList<AppInfo> list = new ArrayList<AppInfo>(mAllAppsList.data);
        return list;
    }

    public ArrayList<AppInfo> getAllDesktopVisibleApplicationInfos() {
        ArrayList<AppInfo> list = new ArrayList<AppInfo>(mAllAppsList.data);
        Iterator<AppInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            AppInfo app = iterator.next();
            if (((HomeDesktopItemInfo) app).container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                iterator.remove();
            }
        }
        return list;
    }

    public boolean addItem(HomeDesktopItemInfo info, boolean pendingInAddList) {
        if (info.isShortcut()) {
            return mAllShortcutsList.add(info, pendingInAddList);
        } else {
            return mAllAppsList.add(info, pendingInAddList);
        }
    }

    public void removeItem(HomeDesktopItemInfo info, boolean pendingInAddList, boolean forHideApp) {
        if (info.isShortcut()) {
            mAllShortcutsList.remove(info, pendingInAddList, forHideApp, false);
        } else {
            mAllAppsList.remove(info, pendingInAddList, forHideApp);
        }
    }

    class AllShortcutsList {
        public static final int DEFAULT_APPLICATIONS_NUMBER = 2;
        /** The list off all apps. */
        public CopyOnWriteArrayList<HomeDesktopItemInfo> data = new CopyOnWriteArrayList<HomeDesktopItemInfo>();

        /** The list of apps that have been added since the last notify() call. */
        public ArrayList<HomeDesktopItemInfo> added = new ArrayList<HomeDesktopItemInfo>(
                DEFAULT_APPLICATIONS_NUMBER);

        /** The list of apps that have been removed since the last notify() call. */
        public ArrayList<HomeDesktopItemInfo> removed = new ArrayList<HomeDesktopItemInfo>();

        /** The list of apps that have been modified since the last notify() call. */
        public ArrayList<HomeDesktopItemInfo> modified = new ArrayList<HomeDesktopItemInfo>();

        //private final int[] mCoordinates = new int[3];

        private final IconCache mIconCache;

        public AllShortcutsList(App context) {
            mIconCache = IconCache.getInstance(context);
        }
        /**
         * Add the supplied ApplicationInfo objects to the list, and enqueue it into
         * the list to broadcast when notify() is called. If the app is already in
         * the list, doesn't add it.
         */
        public boolean add(HomeDesktopItemInfo info, boolean pendingInAddList) {
            synchronized (mModelLock) {
                // 记录到全部应用
                mDataModel.update(info);

                ComponentName cn = encodeAppIntent(info.getIntent());

                // 忽略掉隐藏应用
                if (cn != null && isApplicationHidden(cn)) {
                    return false;
                }

                if (info.id < 0) {
//                    getNextCellPosition(mApp, mCoordinates);
                    info.spanX = 1;
                    info.spanY = 1;

                    //UI thread will find a suitable position for this icon and write the result back to db.
                    addNotPositionedItemToDb(mApp, info, false);
//                    DbManager.addOrMoveItemInDatabase(mApp, info, LauncherSettings.Favorites.CONTAINER_DESKTOP, mCoordinates[0], mCoordinates[1], mCoordinates[2]);
                }

                data.add(info);


                if (pendingInAddList) {
                    added.add(info);
                }
                return true;
            }
        }

        public void removePackage(String packageName) {
            for (int i = data.size() - 1; i >= 0; i--) {
                HomeDesktopItemInfo info = data.get(i);
                if (info.getIntent() == null || info.getIntent().getComponent() == null) {
                    continue;
                }
                final ComponentName component = info.intent.getComponent();
                if (packageName.equals(component.getPackageName())) {
                    remove(i, info, true, false, false);
                }
            }
            mDataModel.remove(packageName);
        }

        public void remove(HomeDesktopItemInfo info) {
            remove(info, true, false, false);
        }

        public void remove(HomeDesktopItemInfo info, boolean pendingInRemoveList, boolean forHideApp, boolean onlyInModel) {
            synchronized (mModelLock) {
                int index = data.indexOf(info);
                if (index >= 0) {
                    remove(index, info, pendingInRemoveList, forHideApp, onlyInModel);
                }
            }
        }

        private void remove(int index, HomeDesktopItemInfo info, boolean pendingInRemoveList, boolean forHideApp, boolean onlyInModel) {
            if (!forHideApp) {
                mDataModel.remove(info);
            }
            if (pendingInRemoveList) {
                removed.add(info);
            }
            mIconCache.remove(info.intent.getComponent());
            data.remove(index);

            if (!onlyInModel) {
                // delete the record
                DbManager.deleteItemByIdImmediately(mApp, info.id);
            }

            if (LOGD_ENABLED) {
                XLog.d(TAG, "Application Info " + info + " is removed.");
            }
        }

        public HomeDesktopItemInfo findAdComponent(String packageName) {
            return null;
        }

        public void clear() {
            data.clear();
            // TODO: do we clear these too?
            added.clear();
            removed.clear();
            modified.clear();
        }
    }

    class AllAppsList {
        public static final int DEFAULT_APPLICATIONS_NUMBER = 42;

        /** The list off all apps. */
        public CopyOnWriteArrayList<HomeDesktopItemInfo> data = new CopyOnWriteArrayList<HomeDesktopItemInfo>();

        /** The list of apps that have been added since the last notify() call. */
        public ArrayList<HomeDesktopItemInfo> added = new ArrayList<HomeDesktopItemInfo>(
                DEFAULT_APPLICATIONS_NUMBER);

        /** The list of apps that have been removed since the last notify() call. */
        public ArrayList<HomeDesktopItemInfo> removed = new ArrayList<HomeDesktopItemInfo>();

        /** The list of apps that have been modified since the last notify() call. */
        public ArrayList<HomeDesktopItemInfo> modified = new ArrayList<HomeDesktopItemInfo>();

        public ArrayList<HomeDesktopItemInfo> availabled = new ArrayList<HomeDesktopItemInfo>();

        private final App mApp;

        private final IconCache mIconCache;

        private final IconFsCache mIconFsCache;

        //private final int[] mCoordinates = new int[3];

        /**
         * Boring constructor.
         */
        public AllAppsList(App app) {
            mApp = app;
            mIconCache = IconCache.getInstance(app);
            mIconFsCache = IconFsCache.getInstance(app);
        }

        /**
         * Add the supplied ApplicationInfo objects to the list, and enqueue it into
         * the list to broadcast when notify() is called. If the app is already in
         * the list, doesn't add it.
         */
        public boolean add(HomeDesktopItemInfo info, boolean pendingInAddList) {
            return add(info, pendingInAddList, true);
        }

        boolean add(HomeDesktopItemInfo info, boolean pendingInAddList, boolean addToDb) {
            synchronized (mModelLock) {
                if (findActivity(data, info.intent.getComponent())) {
                    return false;
                }

                ComponentName cn = info.intent.getComponent();

                // 新增的忽略应用，不加入
                if (info.id < 0 && Utils.shouldIgnoreApp(cn.getPackageName())) {
                    return false;
                }

                // 记录到全部应用
                mDataModel.update(info);

                // 忽略掉隐藏应用
                if (cn != null && isApplicationHidden(cn)) {
                    return false;
                }

                if (info.id < 0) {
//                    getNextCellPosition(mApp, mCoordinates);

                    info.spanX = 1;
                    info.spanY = 1;
                  //UI thread will find a suitable position for this icon and write the result back to db.
                    if (addToDb) {
                        addNotPositionedItemToDb(mApp, info, false);
                    }
//                    DbManager.addOrMoveItemInDatabase(mApp, info, LauncherSettings.Favorites.CONTAINER_DESKTOP, mCoordinates[0], mCoordinates[1], mCoordinates[2]);
                }

                data.add(info);

                if (pendingInAddList) {
                    added.add(info);
                }

                return true;
            }
        }

        public void remove(HomeDesktopItemInfo info) {
            remove(info, true, false);
        }

        public void remove(HomeDesktopItemInfo info, boolean pendingInRemoveList, boolean forHideApp) {
            synchronized (mModelLock) {
                int index = data.indexOf(info);
                if (index >= 0) {
                    remove(index, info, pendingInRemoveList, forHideApp);
                }
            }
        }

        private void remove(int index, HomeDesktopItemInfo info, boolean pendingInRemoveList, boolean forHideApp) {
            if (!forHideApp) {
                mDataModel.remove(info);
            }
            if (pendingInRemoveList) {
                removed.add(info);
            }
            mIconCache.remove(info.intent.getComponent());
            data.remove(index);

            // delete the record
            DbManager.deleteItemByIdImmediately(mApp, info.id);

            if (LOGD_ENABLED) {
                XLog.d(TAG, "Application Info " + info + " is removed.");
            }
        }

        public void clear() {
            data.clear();
            // TODO: do we clear these too?
            added.clear();
            removed.clear();
            modified.clear();
        }

        /**
         * Add the icons for the supplied apk called packageName.
         */
        public void addPackage(Context context, String packageName, HomeDesktopItemInfo replaceAdItemInfo) {
            final List<ResolveInfo> matches = findActivitiesForPackage(context, packageName);
            if (matches.size() <= 0) {
                mAllShortcutsList.remove(replaceAdItemInfo, true, false, false);
                return;
            }
            final ArrayList<HomeDesktopItemInfo> added = new ArrayList<HomeDesktopItemInfo>(matches.size());
            PackageManager packageManager = context.getPackageManager();
            for (ResolveInfo info : matches) {
                HomeDesktopItemInfo app = mItemInfoLoader.getShortcutInfo(packageManager, info, context, true);
                if (replaceAdItemInfo != null) {
                    //执行替换操作
                    mAllShortcutsList.remove(replaceAdItemInfo, false, false, true);
                    replaceAdItemInfo.mergeAdComponentFrom(app);
                    add(replaceAdItemInfo, false);
                    modified.add(replaceAdItemInfo);
                    DbManager.updateItemInDatabase(context, replaceAdItemInfo);
                    replaceAdItemInfo = null;
                } else {
                    add(app, true);
                }
                added.add(app);
            }
        }

        /**
         * Remove the apps for the given apk identified by packageName.
         */
        public void removePackage(String packageName) {
            for (int i = data.size() - 1; i >= 0; i--) {
                HomeDesktopItemInfo info = data.get(i);
                final ComponentName component = info.intent.getComponent();
                if (packageName.equals(component.getPackageName())) {
                    remove(i, info, true, false);
                }
            }
            mDataModel.remove(packageName);
        }

        /**
         * Add and remove icons for this package which has been updated.
         */
        public void updatePackage(Context context, String packageName) {
            final List<ResolveInfo> matches = findActivitiesForPackage(context, packageName);
            if (LOGD_ENABLED) {
                XLog.d(TAG, "package " + packageName + " matches:" + matches.size());
                for (ResolveInfo resolveInfo : matches) {
                    XLog.d(TAG, "resolve info:" + resolveInfo.activityInfo.name);
                }
            }

            if (matches.size() == 1) {
                //Got only one activity candidate. If data has one too, just replace that object.
                //To solve #182457: 应用更新后会自动移出文件夹
                final ResolveInfo info = matches.get(0);
                HomeDesktopItemInfo candidate = null;
                boolean onlyOneCandidate = false;
                for (int i = data.size() - 1; i >= 0; i--) {
                    final HomeDesktopItemInfo applicationInfo = data.get(i);
                    final ComponentName component = applicationInfo.intent.getComponent();
                    if (packageName.equals(component.getPackageName())) {
                        if (candidate == null) {
                            candidate = applicationInfo;
                            onlyOneCandidate = true;
                        } else {
                            onlyOneCandidate = false;
                        }
                    }
                }

                if (onlyOneCandidate) {
                    mIconCache.remove(candidate.intent.getComponent());
                    mIconFsCache.deleteCacheFile(candidate);
                    candidate.intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                    mIconCache.getTitleAndIcon(candidate, info);
                    candidate.lastUpdateTime = Utils.getLastUpdateTime(info);
                    modified.add(candidate);
                    if (LOGD_ENABLED) {
                        XLog.d(TAG, "handle one candidate finish");
                    }
                    return;
                }

            }
            if (matches.size() > 0) {
                // Find disabled/removed activities and remove them from data and
                // add them
                // to the removed list.
                for (int i = data.size() - 1; i >= 0; i--) {
                    final HomeDesktopItemInfo applicationInfo = data.get(i);
                    final ComponentName component = applicationInfo.intent.getComponent();
                    if (packageName.equals(component.getPackageName())) {
                        if (!findActivity(matches, component)) {
                            if (LOGD_ENABLED) {
                                XLog.d(TAG, "delete iteminfo:" + component);
                            }
                            remove(i, applicationInfo, true, false);
                        }
                    }
                }

                // Find enabled activities and add them to the adapter
                // Also updates existing activities with new labels/icons
                final PackageManager packageManager = context.getPackageManager();
                int count = matches.size();
                for (int i = 0; i < count; i++) {
                    final ResolveInfo info = matches.get(i);
                    HomeDesktopItemInfo applicationInfo = findApplicationInfoLocked(
                            info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                    if (LOGD_ENABLED) {
                        XLog.d(TAG, " found applicationInfo?" + (applicationInfo != null));
                    }
                    if (applicationInfo == null) {
                        HomeDesktopItemInfo app = mItemInfoLoader.getShortcutInfo(packageManager, info, context, true);
                        add(app, true);
                    } else {
                        mIconCache.remove(applicationInfo.intent.getComponent());
                        mIconCache.getTitleAndIcon(applicationInfo, info);
                        applicationInfo.lastUpdateTime = Utils.getLastUpdateTime(info);
                        modified.add(applicationInfo);
                    }
                }
            } else {
                // Remove all data for this package.
                for (int i = data.size() - 1; i >= 0; i--) {
                    final HomeDesktopItemInfo applicationInfo = data.get(i);
                    final ComponentName component = applicationInfo.intent.getComponent();
                    if (packageName.equals(component.getPackageName())) {
                        remove(i, applicationInfo, true, false);
                    }
                }
            }

            mDataModel.update(packageName);
        }

        /**
         * Query the package manager for MAIN/LAUNCHER activities in the supplied
         * package.
         */
        private List<ResolveInfo> findActivitiesForPackage(Context context, String packageName) {
            final PackageManager packageManager = context.getPackageManager();

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setPackage(packageName);

            final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
            return apps != null ? apps : Collections.<ResolveInfo>emptyList();
        }

        /**
         * Returns whether <em>apps</em> contains <em>component</em>.
         */
        private boolean findActivity(List<ResolveInfo> apps, ComponentName component) {
            final String className = component.getClassName();
            for (ResolveInfo info : apps) {
                final ActivityInfo activityInfo = info.activityInfo;
                if (activityInfo.name.equals(className)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns whether <em>apps</em> contains <em>component</em>.
         */
        private boolean findActivity(CopyOnWriteArrayList<HomeDesktopItemInfo> apps, ComponentName component) {
            final int n = apps.size();
            for (int i = 0; i < n; i++) {
                final HomeDesktopItemInfo info = apps.get(i);
                if (info.getIntent().getComponent().equals(component)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Find an ApplicationInfo object for the given packageName and className.
         */
        HomeDesktopItemInfo findApplicationInfoLocked(String packageName, String className) {
            for (int i = data.size() - 1; i >= 0; i--) {
                HomeDesktopItemInfo info = data.get(i);
                final ComponentName component = info.intent.getComponent();
                if (component != null && packageName.equals(component.getPackageName())
                        && className.equals(component.getClassName())) {
                    return info;
                }
            }
            return null;
        }

        public void availablePackages(Context context, String[] packageNames) {
            for (String packageName : packageNames) {
                final List<ResolveInfo> matches = findActivitiesForPackage(context, packageName);
                if (matches.size() > 0) {
                    // Find disabled/removed activities and remove them from data
                    // and add them
                    // to the removed list.
                    for (int i = data.size() - 1; i >= 0; i--) {
                        final HomeDesktopItemInfo applicationInfo = data.get(i);
                        final ComponentName component = applicationInfo.intent.getComponent();
                        if (packageName.equals(component.getPackageName())) {
                            if (!findActivity(matches, component)) {
                                if (LOGD_ENABLED) {
                                    XLog.d(TAG, "Remove 1 in availablePackages");
                                    remove(i, applicationInfo, true, false);
                                }
                            }
                        }
                    }

                    // Find enabled activities and add them to the adapter
                    // Also updates existing activities with new labels/icons
                    final PackageManager packageManager = context.getPackageManager();
                    int count = matches.size();
                    for (int i = 0; i < count; i++) {
                        final ResolveInfo info = matches.get(i);
                        HomeDesktopItemInfo applicationInfo = findApplicationInfoLocked(
                                info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                        if (applicationInfo == null) {
                            HomeDesktopItemInfo app = mItemInfoLoader.getShortcutInfo(packageManager, info, context, true);
                            add(app, true);
                        } else {
                            mIconCache.remove(applicationInfo.getIntent().getComponent());
                            mIconCache.getTitleAndIcon(applicationInfo, info);
                            applicationInfo.system = (info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                            applicationInfo.lastUpdateTime = Utils.getLastUpdateTime(info);
                            applicationInfo.storage = Utils.getApplicationStorage(info);
                            availabled.add(applicationInfo);
                        }
                    }
                } else {
                    // Remove all data for this package.
                    /*
                    for (int i = data.size() - 1; i >= 0; i--) {
                        final ApplicationInfo applicationInfo = data.get(i);
                        final ComponentName component = applicationInfo.intent.getComponent();
                        if (packageName.equals(component.getPackageName())) {
                            if (LOGD_ENABLED) {
                                XLog.d(TAG, "Remove 2 in availablePackages");
                                remove(i, applicationInfo);
                            }
                        }
                    }*/
                }
            }
        }
    }

    public static void addNotPositionedItemToDb(Context context, HomeItemInfo homeItemInfo, boolean notify) {
      //we use -1 to indate ui find a suitable position for this icon and write the result back to db.
    	DbManager.addItemToDatabase(context, homeItemInfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, -1, -1, -1, notify);
    }

    // TODO: 暂时暴露一下，后面再优化为protected/private
    public Callbacks getCallbacks() {
        return mCallbacks != null ? mCallbacks.get() : null;
    }

    AllAppsList getAllAppsList() {
        return mAllAppsList;
    }

    AllShortcutsList getAllShortcutsList() {
        return mAllShortcutsList;
    }

    DataModel getDataModel() {
        return mDataModel;
    }

    // TODO: 暂时暴露一下，后面再优化为protected/private
    public DeferredHandler getHandler() {
        return mHandler;
    }
    //////////////////////////////////////////////////////////////////////
    //数据结构容器
    class DataModel {
        //////////////////////////////////////////////////////////////////////
        // Launcher application 数据结构容器

        /**
         * 所有应用的launcher application数据结构，包括隐藏应用、快捷方式等等（不包含各种Widget）
         */
        private final ArrayList<HomeDesktopItemInfo> mAllApplications = new ArrayList<HomeDesktopItemInfo>();

        /**
         * 更新某个应用的launcher application数据结构（记录到全部应用）
         * 如果存在则删除旧的，增加新的
         * 如果不存在则直接增加
         * 用ComponentName作为关键值
         * @param app
         */
        public final void update(HomeDesktopItemInfo app) {
            ComponentName cn = encodeAppIntent(app.getIntent());
            if (cn == null) {
                if (LOGW_ENABLED) {
                    XLog.w(TAG, "unknown info: " + app.getIntent());
                }
                return;
            }
            synchronized (mAllApplications) {
                for (final HomeDesktopItemInfo hdii : mAllApplications) {
                    if (cn.equals(encodeAppIntent(hdii.getIntent()))) {
                        mAllApplications.remove(hdii);
                        break;
                    }
                }
                mAllApplications.add(app);
            }
        }

        /**
         * 此方法暂时没有用到
         * 删除某个应用的launcher application数据结构
         * 用ComponentName作为关键值
         * @param cn
         */
        public final void remove(final HomeDesktopItemInfo info) {
            ComponentName cn = encodeAppIntent(info.getIntent());
            removeInternal(cn);
        }

        /**
         * 此方法暂时没有用到
         * 删除某个应用的launcher application数据结构
         * 用ComponentName作为关键值
         * @param cn
         */
        private final void removeInternal(final ComponentName cn) {
            if (cn == null) {
                return;
            }
            if (Constant.PACKAGE_NAME.equals(cn.getPackageName())) {
                if (LOGW_ENABLED) {
                    //XLog.w(TAG, "stack", new Exception());
                }
                return;
            }
            synchronized (mAllApplications) {
                for (final HomeDesktopItemInfo hdii : mAllApplications) {
                    if (cn.equals(encodeAppIntent(hdii.getIntent()))) {
                        mAllApplications.remove(hdii);
                        break;
                    }
                }
            }
        }

        /**
         * 删除某个应用的launcher application数据结构
         * 用package作为关键值，可能存在多个元素需要删除
         * @param intent
         */
        public final void remove(final String pkg) {
            if (Constant.PACKAGE_NAME.equals(pkg)) {
                if (LOGW_ENABLED) {
                    //XLog.w(TAG, "stack", new Exception());
                }
                return;
            }
            synchronized (mAllApplications) {
                for (final Iterator<HomeDesktopItemInfo> iter = mAllApplications.iterator(); iter.hasNext();) {
                    final HomeDesktopItemInfo hdii = iter.next();
                    if (hdii.getComponentName() != null && pkg.equals(hdii.getComponentName().getPackageName())) {
                        iter.remove();
                    }
                }
            }
        }

        //TODO 待实现。同步该包名的所有itemInfo
        public final void update(final String pkg) {

        }

        /**
         * 删除所有记录下来的应用的launcher application数据结构
         */
        public final void removeAllApps() {
            synchronized (mAllApplications) {
                mAllApplications.clear();
            }
        }

        public final HomeDesktopItemInfo findApp(ComponentName cn) {
            if (cn == null) {
                return null;
            }
            synchronized (mAllApplications) {
                for (final HomeDesktopItemInfo hdii : mAllApplications) {
                    if (cn.equals(encodeAppIntent(hdii.getIntent()))) {
                        return hdii;
                    }
                }
            }
            return null;
        }

        public final HomeDesktopItemInfo findApp(Intent intent) {
            ComponentName cn = encodeAppIntent(intent);
            return findApp(cn);
        }
    }

    public static final ComponentName encodeAppIntent(Intent intent) {
        if (intent == null) {
            if (LOGW_ENABLED) {
                XLog.w(TAG, "intent is null");
            }
            return null;
        }

        ComponentName cn = intent.getComponent();
        if (cn != null) { // OK, use original
            return cn;
        }

        // iLauncher shortcut, encode it with "action" and "type"
        if (Constant.LAUNCHER_CUSTOM_SHORTCUT_ACTION.equals(intent.getAction())) {
            final String pkg = "custom-package: " + Constant.PACKAGE_NAME;
            final String cls = "action: " + Constant.LAUNCHER_CUSTOM_SHORTCUT_ACTION + "; type: " + intent.getType();
            cn = new ComponentName(pkg, cls);
            return cn;
        }

        // other shortcut, encode it with Intent.toString()
        final String pkg = "custom-package: " + Constant.PACKAGE_NAME;
        final String cls = "intent-string: " + intent;
        cn = new ComponentName(pkg, cls);
        return cn;
    }

    @Override
    protected LoaderTask createLoaderTask(Context context, boolean isLaunching, Handler handler, boolean flushAllApps) {
        LoaderTask loaderTask = new LoaderTask(context, this, isLaunching, handler, flushAllApps);
        return loaderTask;
    }

    @Override
    protected PackageUpdatedTaskBase createPackageUpdatedTask(int op, String[] packages) {
        return new PackageUpdatedTask(mApp, op, packages, this);
    }

    @Override
    public void removeApplicationInfo(Context context, AppInfo appInfo) {
        if (!(appInfo instanceof HomeDesktopItemInfo) || ((HomeDesktopItemInfo) appInfo).isShortcut()) {
            return;
        }

        HomeDesktopItemInfo applicationInfo = (HomeDesktopItemInfo) appInfo;

        mAllAppsList.remove(applicationInfo);

        final List<HomeDesktopItemInfo> removed = mAllAppsList.removed;
        mAllAppsList.removed = new ArrayList<HomeDesktopItemInfo>();

        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
        if (callbacks == null) {
            XLog.w(TAG, "no mCallbacks");
            return;
        }

        mHandler.post(new Runnable() {
            public void run() {
                if (callbacks == mCallbacks.get()) {
                    callbacks.bindAppsRemoved(removed, true);
                }
            }
        });
    }

    public boolean isShortcutIntentExist(Intent intent, String title) {
        ComponentName cn = intent.getComponent();

        if (cn == null) {
            return false;
        }
//        for (HomeDesktopItemInfo allapps : mAllAppsList.data) {
//            if (cn.equals(allapps.getComponentName())) {
//                return true;
//            }
//        }

        for (HomeDesktopItemInfo allapps : mAllShortcutsList.data) {
            if (intent.filterEquals(allapps.getIntent()) && title.equals(allapps.getTitle())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getAppWidgetHostId() {
        return LauncherProvider.APPWIDGET_HOST_ID;
    }

    @Override
    public Uri getContentAppWidgetResetUri() {
        return LauncherProvider.CONTENT_APPWIDGET_RESET_URI;
    }
}
