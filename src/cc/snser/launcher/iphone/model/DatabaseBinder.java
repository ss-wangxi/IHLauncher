package cc.snser.launcher.iphone.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.SystemClock;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.apps.model.workspace.LauncherAppWidgetInfo;
import cc.snser.launcher.apps.model.workspace.LauncherWidgetViewInfo;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.model.ItemInfoLoader;
import cc.snser.launcher.model.LauncherModel.Callbacks;
import cc.snser.launcher.model.LauncherModelCommons.FavoritesColumnIndex;
import cc.snser.launcher.style.SettingPreferences;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.IOUtils;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DatabaseBinder {
    private final Context mContext;

    private final LoaderTask loaderTask;

    private final PackageManager mPM;

    private final boolean mIsFirst;

    private static final boolean DEBUG_LOADERS = LoaderTask.DEBUG_LOADERS;
    private static final String TAG = LauncherModelIphone.TAG;

    private final ArrayList<HomeItemInfo> desktopItems = new ArrayList<HomeItemInfo>();
    private final ArrayList<HomeItemInfo> missedItemInfo = new ArrayList<HomeItemInfo>();
    private final ArrayList<HomeItemInfo> nonePositionItemInfo = new ArrayList<HomeItemInfo>();


    ArrayList<HomeItemInfo> addToBottomItems = new ArrayList<HomeItemInfo>();

    ArrayList<HomeDesktopItemInfo> shortcutInfos = new ArrayList<HomeDesktopItemInfo>();
    private final boolean mFirstLoad;

    public DatabaseBinder(Context context, LoaderTask loaderTask, boolean firstLoad) {
        this.mContext = context;
        this.mPM = mContext.getPackageManager();
        this.loaderTask = loaderTask;
        final Callbacks oldCallbacks = loaderTask.getLauncherModel().getCallbacks();
        mIsFirst = oldCallbacks != null ? oldCallbacks.isFirst() : true;
        mFirstLoad = firstLoad;
    }

    public ArrayList<HomeDesktopItemInfo> load() {
        loadAndBindDesktop();

        loaderTask.getAllAppsList().clear();
        loaderTask.getAllShortcutsList().clear();

        Collections.sort(shortcutInfos, new Comparator<HomeDesktopItemInfo>() {
            @Override
            public int compare(HomeDesktopItemInfo lhs, HomeDesktopItemInfo rhs) {
                if (lhs.getId() < rhs.getId()) {
                    return -1;
                } else if (lhs.getId() > rhs.getId()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        Iterator<HomeDesktopItemInfo> iterator = shortcutInfos.iterator();
        while (iterator.hasNext()) {
            HomeDesktopItemInfo info = iterator.next();
            boolean added = false;
            if (!info.isShortcut()) {
                added = loaderTask.getAllAppsList().add(info, true);
            } else {
                added = loaderTask.getAllShortcutsList().add(info, true);
            }
            //库中有记录，添加到list却失败。可能是恢复布局导入的库。进行删除避免不一致的情况，避免取消隐藏时产生重复图标
            //TODO 待优化
            //双层不消重
            if (!added) {
                if (DEBUG_LOADERS) {
                    XLog.d(TAG, "add iteminfo to model failed, try to remove itemInfo");
                }
                DbManager.deleteItemByIdImmediately(mContext, info.id);
                iterator.remove();
            }
        }

        loaderTask.getAllAppsList().added = new ArrayList<HomeDesktopItemInfo>();
        loaderTask.getAllShortcutsList().added = new ArrayList<HomeDesktopItemInfo>();

        return shortcutInfos;
    }

    private void loadAndBindDesktop() {
        final long t = SystemClock.uptimeMillis();

        final Callbacks oldCallbacks = loaderTask.getLauncherModel().getCallbacks();

        // Tell the workspace that we're about to start firing items at it
        loaderTask.postRunnable(oldCallbacks, new Runnable() {
            @Override
            public void run() {
                Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    callbacks.startBindingInHome();
                }
            }
        });

        if (loaderTask.stopped()) {
            return;
        }

        // load the current screen
        int screenCountDb = DbManager.getMaxScreenCount(mContext);
        int screenCountPref = SettingPreferences.getScreenNumber(0);
        
        int screenCount  = screenCountPref > 0 ? screenCountPref : screenCountDb;
        if(mFirstLoad){
        	screenCount = Math.max(screenCountDb, screenCountPref);
        }
        final int currentScreen = (oldCallbacks != null ? oldCallbacks.getCurrentWorkspaceScreen() : 0);
        //final int currentScreen = SettingPreferences.getHomeScreen();

        if (loaderTask.stopped()) {
            return;
        }

        loadAndBindScreen(oldCallbacks, currentScreen);

        loadAllScreen(oldCallbacks, screenCount);
        for (int i = 1; !loaderTask.stopped(); i++) {
   
            int left = currentScreen - i;
            int right = currentScreen + i;
            if (left < 0 && right >= screenCount) {
                break;
            }
            if (left >= 0) {
                loadAndBindScreen(oldCallbacks, left);
            }
            if (right < screenCount) {
                loadAndBindScreen(oldCallbacks, right);
            }
        }

        loadAndBindScreen(oldCallbacks, -1);

        if (loaderTask.stopped()) {
            return;
        }

        if (!nonePositionItemInfo.isEmpty()) {
            for (HomeItemInfo homeItemInfo : nonePositionItemInfo) {
                if (homeItemInfo instanceof HomeDesktopItemInfo) {
                    if (!shortcutInfos.contains(homeItemInfo)) {
                        shortcutInfos.add((HomeDesktopItemInfo) homeItemInfo);
                    }
                }
                if (DEBUG_LOADERS) {
                    XLog.d(TAG, "add none positioned info at databaseBinder:" + homeItemInfo);
                }
            }
            loaderTask.postRunnable(oldCallbacks, new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindMissedItem(nonePositionItemInfo);
                    }
                }
            });
        }

        if (missedItemInfo != null && missedItemInfo.size() > 0) {
          //将所有missedItemInfo也添加到shortcutInfos中
            for (HomeItemInfo missedInfo : missedItemInfo) {
                IphoneItemInfoUtils.cleanPosition(missedInfo);
                if (missedInfo instanceof HomeDesktopItemInfo) {
                    if (!shortcutInfos.contains(missedInfo)) {
                        shortcutInfos.add((HomeDesktopItemInfo) missedInfo);
                    }
                }
            }

            loaderTask.postRunnable(oldCallbacks, new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindMissedItem(missedItemInfo);
                    }
                }
            });
        }

        if (loaderTask.stopped()) {
            return;
        }

        // If we're profiling, this is the last thing in the queue.
        if (DEBUG_LOADERS) {
            loaderTask.postRunnable(oldCallbacks, new Runnable() {
                @Override
                public void run() {
                    XLog.d(TAG, "bound workspace in " + (SystemClock.uptimeMillis() - t) + "ms");
                }
            });
        }
    }
    
    private void loadAllScreen(final Callbacks oldCallbacks, final int count){
    	for(int i = 0; i < count; i ++){
    		 loaderTask.postRunnable(oldCallbacks, new Runnable() {
    	            @Override
    	            public void run() {
    	           
    	                final Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
    	                if (callbacks != null) {
    	                	if(callbacks.getCurrentScreens() < count)
    	                		callbacks.addScreen();
    	                }
    	            }
    	        });
    	}
    }

    @SuppressWarnings("unchecked")
    private void loadAndBindScreen(final Callbacks oldCallbacks, int targetScreen) {

        desktopItems.clear();

        loadItems(LauncherSettings.Favorites.CONTAINER_DESKTOP, targetScreen);

        if (loaderTask.stopped()) {
            return;
        }

        int n;
        if (!desktopItems.isEmpty()) {
            // Add the items to the workspace.
            final ArrayList<HomeItemInfo> finalDesktopItems = (ArrayList<HomeItemInfo>) desktopItems.clone();
            n = finalDesktopItems.size();
            for (int i = 0; i < n; i += LoaderTask.ITEMS_CHUNK) {
            	
                final int start = i;
                final int chunkSize = (i + LoaderTask.ITEMS_CHUNK <= n) ? LoaderTask.ITEMS_CHUNK : (n - i);
                loaderTask.postRunnable(oldCallbacks, new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = loaderTask.tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                        	long start0 = System.currentTimeMillis();
                            callbacks.bindItems(finalDesktopItems, start, start + chunkSize);
                            XLog.e("bindItems", "bindItems use time = " + (System.currentTimeMillis() - start0));
                        }
                    }
                });
            }
        }
    }


    private void loadItems(int targetContainer, int targetScreen) {
        boolean loadFolderContentItems = false;

        // 构建查询条件
        String selection = null;
        if (targetContainer == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            selection = IphoneUtils.FavoriteExtension.CONTAINER + " = "
                    + LauncherSettings.Favorites.CONTAINER_DESKTOP + " and "
                    + IphoneUtils.FavoriteExtension.SCREEN + " = " + targetScreen;
        } else {
            selection = IphoneUtils.FavoriteExtension.CONTAINER + " = "
                    + targetContainer;
            loadFolderContentItems = true;
        }

        loadItems(selection, loadFolderContentItems, targetContainer, targetScreen);
    }

    private void loadItems(String selection, boolean loadFolderContentItems, int debugContainerId, int debugScreenId) {
        final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

        // 查询
        Cursor c = DbManager.query(mContext, LauncherSettings.Favorites.getContentUri(mContext, true), null, selection, null, null);

        final int xCount = SettingPreferences.getHomeLayout(mContext)[1];
        final int yCount = SettingPreferences.getHomeLayout(mContext)[0];

        final HomeItemInfo occupied[][] = new HomeItemInfo[xCount][yCount];

        try {
            if (c != null) {
                FavoritesColumnIndex index = new FavoritesColumnIndex(c);
                while (!loaderTask.stopped() && c.moveToNext()) {
                    try {
                        int itemType = c.getInt(index.itemTypeIndex);
                        switch (itemType) {
                            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                loadItemApp(c, index, occupied, itemType, loadFolderContentItems);
                                break;
                            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                loadItemAppWidget(c, index, occupied);
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_VIEW:
                                loadItemWidgetView(c, index, occupied, loadFolderContentItems);
                                break;
                        }
                        // TODO: more
                        // ...
                    } catch (Exception e) {
                        XLog.printStackTrace(e);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(c);
        }

        if (DEBUG_LOADERS) {
            if (debugContainerId == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                XLog.d(TAG, "loaded workspace screen[" + debugScreenId + "] in " + (SystemClock.uptimeMillis() - t) + "ms");
                XLog.d(TAG, "workspace screen[" + debugScreenId + "] layout: ");
                for (int y = 0; y < yCount; y++) {
                    String line = "";
                    for (int x = 0; x < xCount; x++) {
                        line += ((occupied[x][y] != null) ? "#" : ".");
                    }
                    XLog.d(TAG, "[ " + line + " ]");
                }
            } else {
                XLog.d(TAG, "loaded folder items in " + (SystemClock.uptimeMillis() - t) + "ms");
            }
        }
    }

    // TODO: 简化参数
    private void loadItemApp(final Cursor c, FavoritesColumnIndex index, final HomeItemInfo occupied[][], int itemType, boolean loadFolderContentItems) {
        String intentDescription = c.getString(index.intentIndex);

        Intent intent = null;
        try {
            intent = Intent.parseUri(intentDescription, 0);
        } catch (URISyntaxException e) {
            return;
        } catch (NullPointerException e) {
            return;
        }

        HomeDesktopItemInfo info = null;

        ResolveInfo resolveInfo = null;
        if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            resolveInfo = mPM.resolveActivity(intent, 0);
            info = ItemInfoLoader.getInstance(mContext).getShortcutInfo(intent, resolveInfo, mContext, c, index.titleIndex, index.iconTypeIndex, index.iconPackageIndex, index.iconResourceIndex, !loadFolderContentItems);//文件夹内的图标延迟初始化
        } else {
            info = ItemInfoLoader.getInstance(mContext).getShortcutInfo(c, intent, mContext, index.iconTypeIndex, index.iconPackageIndex, index.iconResourceIndex, index.iconIndex, index.titlePackageIndex, index.titleResourceIndex, index.titleIndex);
        }

        IphoneItemInfoUtils.loadFromDatabase(info, c, index);

        if (info != null) {
            info.intent = intent;
            long container = info.container;

            //尝试将app替换为shortcut
//            info = shortcutReplacer.replacedItemInfo(info, dbScreen);

            //尝试替换推荐位为实际的应用
            //info = repalceAdComponent(info);

            // 记录到全部应用
            loaderTask.getLauncherModel().getDataModel().update(info);
            
            //IconCache.getInstance(mContext).getIcon(info.intent);

            // 处理隐藏应用
            ComponentName cn = LauncherModelIphone.encodeAppIntent(intent);
            if (cn != null && loaderTask.getLauncherModel().isApplicationHidden(cn)) {
                //隐藏应用，库中却有记录。可能是恢复布局导入的库。进行删除避免不一致的情况，避免取消隐藏时产生重复图标
                //TODO 待优化
                if (DEBUG_LOADERS) {
                    XLog.e(TAG, "delete hide apps from database");
                }
                DbManager.deleteItemByIdImmediately(mContext, info.id);
                return;
            }

            // check & update map of what's occupied
            if (!checkItemPlacement(occupied, info, missedItemInfo, nonePositionItemInfo, addToBottomItems)) {
                XLog.d(TAG, "missing info:" + info);
                return;
            }

            switch ((int) container) {
                case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                    desktopItems.add(info);
                    break;
                default: {
                    // Item is in a user folder
                    // 兼容历史数据，变更cell x字段，使之成为记录顺序号的字段
                    int sequence = info.cellX;
                    if (sequence != info.cellX) {
                        DbManager.addOrMoveFolderItemInDatabase(mContext, info, container);
                    }
                }
                    break;
            }

            shortcutInfos.add(info);
        } else {
            // Failed to load the shortcut, probably because the
            // activity manager couldn't resolve it (maybe the app
            // was uninstalled), or the db row was somehow screwed up.
            // Delete it.
            long id = c.getLong(index.idIndex);
            XLog.e(TAG, "Error loading shortcut " + id + ", removing it");
            DbManager.deleteItemByIdImmediately(mContext, id);
        }
    }

    private void loadItemAppWidget(final Cursor c, FavoritesColumnIndex index, final HomeItemInfo occupied[][]) {
        // Read all Launcher-specific widget details
        int appWidgetId = c.getInt(index.appWidgetIdIndex);

        LauncherAppWidgetInfo appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId);

        IphoneItemInfoUtils.loadFromDatabase(appWidgetInfo, c, index);

        // check & update map of what's occupied
        if (!checkItemPlacement(occupied, appWidgetInfo, missedItemInfo, nonePositionItemInfo, addToBottomItems)) {
            XLog.d(TAG, "missing info:" + appWidgetInfo);
            return;
        }

        if (appWidgetInfo.container != LauncherSettings.Favorites.CONTAINER_DESKTOP || appWidgetInfo.getScreen() < 0) {
            XLog.e(TAG, "Widget found where container " + "!= CONTAINER_DESKTOP or screen < 0  -- ignoring!");
            return;
        }

        desktopItems.add(appWidgetInfo);
    }

    private void loadItemWidgetView(final Cursor c, FavoritesColumnIndex index, final HomeItemInfo occupied[][], boolean loadFolderContentItems) {
        // Read all Launcher-specific widget view details
        LauncherWidgetViewInfo info = getLauncherWidgetViewInfo(c, mContext, index);

        if (info != null) {
            // check & update map of what's occupied
            //第一次的时候,忽略掉未定位的widget，交由下面的代码处理，添加到addToBottomWidgets中
            if (!checkItemPlacement(occupied, info, missedItemInfo, nonePositionItemInfo, addToBottomItems)) {
                XLog.d(TAG, "missing info:" + info);
                return;
            }

            int container = (int) info.container;
            switch (container) {
                case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                    desktopItems.add(info);
                    break;
                default:
                    // Item is in a user folder
                    // 兼容历史数据，变更cellx字段，使之成为记录顺序号的字段
                    int sequence = info.cellX;
                    if (sequence != info.cellX) {
                        DbManager.addOrMoveFolderItemInDatabase(mContext, info, container);
                    }
                    if (!loadFolderContentItems) {
                        desktopItems.add(info);
                    }
                    break;
            }
        } else {
            // Failed to load the shortcut, probably because the
            // activity manager couldn't resolve it (maybe the app
            // was uninstalled), or the db row was somehow screwed up.
            // Delete it.
            long id = c.getLong(index.idIndex);
            XLog.e(TAG, "Error loading widget view " + id + ", removing it");
            DbManager.deleteItemByIdImmediately(mContext, id);
        }
    }

    /**
     * Make an ShortcutInfo object for a shortcut that isn't an application.
     */
    private static LauncherWidgetViewInfo getLauncherWidgetViewInfo(Cursor c, Context context, FavoritesColumnIndex index) {
        int widgetViewType = c.getInt(index.appWidgetIdIndex);
        if (widgetViewType < 0) {
            return null;
        }

        // Plugin widget view
        Intent intent = null;
        String intentDescription = c.getString(index.intentIndex);
        if (intentDescription != null) {
            try {
                intent = Intent.parseUri(intentDescription, 0);
            } catch (URISyntaxException e) {
                XLog.e(TAG, "Parse WidgetView intent URI failed. [" + intentDescription + "]");
                return null;
            }
        }

        int spanX = c.getInt(index.spanXIndex);
        int spanY = c.getInt(index.spanYIndex);

        Serializable identity = widgetViewType;

        LauncherWidgetViewInfo widgetViewInfo = new LauncherWidgetViewInfo(widgetViewType, identity);
        widgetViewInfo.intent = intent;
        IphoneItemInfoUtils.loadFromDatabase(widgetViewInfo, c, index);

        widgetViewInfo.spanX = spanX;
        widgetViewInfo.spanY = spanY;

        return widgetViewInfo;
    }
    private boolean isPlacementOccupied(HomeItemInfo occupied[][], int x, int y){
    	HomeItemInfo item = occupied[x][y];
    	if(item == null){
    		return false;
    	}
    	
    	return true;
    	
    	
    }
    // check & update map of what's occupied; used to discard overlapping/invalid items
    private boolean checkItemPlacement(HomeItemInfo occupied[][], HomeItemInfo item, List<HomeItemInfo> missedHomeItemInfo, List<HomeItemInfo> nonePositionItemInfo, List<HomeItemInfo> addToBottomItems) {
        if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            return true;
        }

        if (mIsFirst && IphoneItemInfoUtils.isItemInfoNotPositioned(item)) {
            addToBottomItems.add(item);
            return false;
        }

        if (IphoneItemInfoUtils.isItemInfoNotPositioned(item)) {
            nonePositionItemInfo.add(item);
            return false;
        }

        if (item.cellX >= occupied.length || item.cellY >= occupied[item.cellX].length
                || item.cellX + item.spanX - 1 >= occupied.length || item.cellY + item.spanY - 1 >= occupied[item.cellX].length) {
            missedHomeItemInfo.add(item);
            return false;
        }

        for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
            for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                if (x < occupied.length && y < occupied[x].length && isPlacementOccupied(occupied, x, y)) {
                    XLog.e(TAG, "Error loading shortcut " + item
                        + " into cell (" + item.screen + ":"
                        + x + "," + y
                        + ") occupied by "
                        + occupied[x][y]);
                    missedHomeItemInfo.add(item);
                    return false;
                }
            }
        }
        for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
            for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                if (x < occupied.length && y < occupied[x].length) {
                    occupied[x][y] = item;
                }
            }
        }
        return true;
    }
}
