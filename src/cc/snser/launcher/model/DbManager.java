package cc.snser.launcher.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcelable;
import cc.snser.launcher.Constant;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.apps.model.HiddenApplication;
import cc.snser.launcher.apps.model.ItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.iphone.model.IphoneItemInfoUtils;
import cc.snser.launcher.iphone.model.IphoneUtils;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.style.SettingPreferences;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;
import com.shouxinzm.launcher.util.StringUtils;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class DbManager {
    private static final String TAG = "Launcher.DbManager";

    /*区分db访问半成品 by snsermail@gmail.com*/
    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    public static void addOrMoveFolderItemInDatabase(Context context, HomeItemInfo item, long container) {
        if (item.id == HomeItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(context, item, container, 0, item.cellX, 0, false);
        } else {
            // From somewhere else
            moveItemInDatabase(context, item, container, 0, item.cellX, 0);
        }
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    public static void addOrMoveItemInDatabase(Context context, HomeItemInfo item, long container,
            int screen, int cellX, int cellY) {
        if (item.id == HomeItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
        } else {
            // From somewhere else
            moveItemInDatabase(context, item, container, screen, cellX, cellY);
        }
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    public static void moveItemInDatabase(Context context, HomeItemInfo item, long container, int screen,
            int cellX, int cellY) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;

        moveItemInDatabase(context, item);
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    public static void moveItemInDatabase(Context context, HomeItemInfo item) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Move item with type: " + item.itemType
                    + " to new screen: " + item.screen + ", new cellX: " + item.cellX + ", new cellY: " + item.cellY);
        }
        final Uri uri = LauncherSettings.Favorites.getContentUri(context, item.id, false);
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);

        convertScreenNumber(context, item, values);

         DaemonThread.postThreadTask(new Runnable() {
                @Override
                public void run() {
                    cr.update(uri, values, null, null);
                }
            });
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    private static void convertScreenNumber(Context context, ItemInfo item, ContentValues values) {
        if (item != null && item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP && item.screen >= 0) {
        	if (item.isInSecondLayer) {
        		values.put(LauncherSettings.Favorites.SCREEN, item.screen - 0);
        	} else {
        		values.put(LauncherSettings.Favorites.SCREEN, item.screen - Workspace.getWorkspacePrefixScreenSize());
			}
        }
    }

    /*不用区分db访问 by snsermail@gmail.com*/
    public static boolean isAdShortcutExists(Context context, Intent shortcutIntent) {
        boolean isRecommendApp = false;
        String intentType = null;
        if (shortcutIntent != null && shortcutIntent.getComponent() != null) {
            String shortcutPackageName = shortcutIntent.getComponent().getPackageName();
        }

        if (isRecommendApp) {
            final ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(LauncherSettings.Favorites.getContentUri(context, true), new String[] {
                    LauncherSettings.Favorites.ICON_PACKAGE,
                    LauncherSettings.Favorites.INTENT
                }, LauncherSettings.Favorites.ITEM_TYPE + " = "
                        + LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT, null, null);

            String packageName;
            String intentDescription;
            Intent intent;

            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        packageName = c.getString(0);
                        if (!Constant.PACKAGE_NAME.equals(packageName)) {
                            continue;
                        }
                        intentDescription = c.getString(1);
                        try {
                            intent = Intent.parseUri(intentDescription, 0);
                        } catch (URISyntaxException e) {
                            continue;
                        }
                        if (intent == null) {
                            continue;
                        }
                        if (!Constant.LAUNCHER_CUSTOM_SHORTCUT_ACTION.equals(intent.getAction())) {
                            continue;
                        }
                        if (!intentType.equals(intent.getType())) {
                            continue;
                        }

                        return true;
                    }
                } finally {
                    c.close();
                }
            }
        }

        return false;
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    public static void addItemToDatabase(Context context, HomeItemInfo item, long container,
            int screen, int cellX, int cellY, boolean notify) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Add item with type: " + item.itemType + ", screen: " + screen + ", cellX: " + cellX + ", cellY: " + cellY);
        }
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;

        final ContentValues values = new ContentValues();
        item.onAddToDatabase(values);

        IphoneItemInfoUtils.onAddToDatabase(item, values);

        addItemToDatabase(context, item, values, notify);
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    public static void addItemToDatabase(Context context, HomeItemInfo item, ContentValues values, boolean notify) {
        final ContentResolver cr = context.getContentResolver();

        convertScreenNumber(context, item, values);

        Uri result = cr.insert(LauncherSettings.Favorites.getContentUri(context, notify), values);

        if (result != null) {
            item.id = Integer.parseInt(result.getPathSegments().get(2));
        }
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    /**
     * Update an item to the database in a specified container.
     */
    public static void updateItemInDatabase(Context context, HomeItemInfo item) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Update item with id: " + item.id + ", type: " + item.itemType + ", screen: " + item.screen + ", cellX: " + item.cellX + ", cellY: " + item.cellY);
        }
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        item.onAddToDatabase(values);

        IphoneItemInfoUtils.onAddToDatabase(item, values);

        convertScreenNumber(context, item, values);
       
        cr.update(LauncherSettings.Favorites.getContentUri(context, item.id, false), values, null, null);
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    public static boolean containsItemInDatabase(Context context, int screen, int cellX, int cellY) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings.Favorites.getContentUri(context, false);

        final Cursor c = cr.query(uri, new String[] {
                LauncherSettings.Favorites._ID
            }, LauncherSettings.Favorites.SCREEN + " = " + screen
            + " and " + LauncherSettings.Favorites.CELLX + " = "
            + cellX
            + " and " + LauncherSettings.Favorites.CELLY + " = "
            + cellY, null, null);

        if (c != null) {
            try {
                return c.moveToNext();
            } finally {
                c.close();
            }
        }

        return false;
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    public static void addItemToDatabase(Context context, HomeItemInfo item) {
        final ContentValues values = new ContentValues();
        item.onAddToDatabase(values);

        final ContentResolver cr = context.getContentResolver();

        Uri result = cr.insert(LauncherSettings.Favorites.getContentUri(context, false), values);
        if (result != null) {
            item.id = Integer.parseInt(result.getPathSegments().get(2));
        }
    }

    /*区分db访问半成品 by snsermail@gmail.com*/
    /**
     * Removes the specified item from the database
     * @param context
     * @param item
     */
    public static void deleteItemFromDatabase(Context context, HomeItemInfo item) {
        if (item == null) {
            return;
        }
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Delete item with id: " + item.id + ", type: " + item.itemType + ", screen: " + item.screen + ", cellX: " + item.cellX + ", cellY: " + item.cellY);
        }
        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = LauncherSettings.Favorites.getContentUri(context, item.id, false);
         DaemonThread.postThreadTask(new Runnable() {
                @Override
                public void run() {
                    cr.delete(uriToDelete, null, null);
                }
            });
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    /**
     * 根据id立即从库中删除item
     * @param context
     * @param item
     */
    public static void deleteItemByIdImmediately(Context context, long id) {
        final ContentResolver cr = context.getContentResolver();
        Uri uri = LauncherSettings.Favorites.getContentUri(context, id, false);
        cr.delete(uri, null, null);
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    public static void deleteItemsInScreenFromDatabase(Context context, int screen) {
        final int finalScreen = screen - Workspace.getWorkspacePrefixScreenSize();
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Delete items with screen: " + screen);
        }
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings.Favorites.getContentUri(context, false);

        DaemonThread.postThreadTask(new Runnable() {
            @Override
            public void run() {
                final Cursor c = cr.query(uri, new String[] {
                    LauncherSettings.Favorites._ID
                }, LauncherSettings.Favorites.SCREEN + " = " + finalScreen + " and "
                        + LauncherSettings.Favorites.CONTAINER + " = "
                        + LauncherSettings.Favorites.CONTAINER_DESKTOP + " and "
                        + LauncherSettings.Favorites.ITEM_TYPE + " = "
                        + LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER, null, null);

                if (c != null) {
                    Set<Long> folderIds = new HashSet<Long>();
                    try {
                        while (c.moveToNext()) {
                            folderIds.add(c.getLong(0));
                        }
                    } finally {
                        c.close();
                    }

                    if (!folderIds.isEmpty()) {
                        cr.delete(uri, LauncherSettings.Favorites.CONTAINER + " in ("
                                + StringUtils.join(folderIds, ",") + ")", null);
                    }
                }

                // delete the reset items
                cr.delete(uri, LauncherSettings.Favorites.SCREEN + " = " + finalScreen
                        + " and " + LauncherSettings.Favorites.CONTAINER + " = "
                        + LauncherSettings.Favorites.CONTAINER_DESKTOP, null);
            }
        });
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    public static void increaseItemsScreenCountFromDatabase(Context context, int fromScreen, final boolean increase) {
        final int finalFromScreen = fromScreen - Workspace.getWorkspacePrefixScreenSize();
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Increase items from screen " + fromScreen + " with value " + (increase ? 1 : -1));
        }
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings.Favorites.getContentUri(context, false);
        final ContentValues values = new ContentValues();
        DaemonThread.postThreadTask(new Runnable() {
            @Override
            public void run() {
                values.put(increase ? LauncherSettings.Favorites.INCREASE_COLUMN : LauncherSettings.Favorites.DECREASE_COLUMN, LauncherSettings.Favorites.SCREEN);
                cr.update(uri, values, LauncherSettings.Favorites.SCREEN + " >= " + finalFromScreen
                        + " and " + LauncherSettings.Favorites.CONTAINER + " = "
                        + LauncherSettings.Favorites.CONTAINER_DESKTOP, null);
            }
        });
    }

    /*不用区分db访问 by snsermail@gmail.com*/
    public static void moveItemsInScreenFromDatabase(Context context, int fromScreen, int toScreen) {
        final int finalFromScreen = fromScreen - Workspace.getWorkspacePrefixScreenSize();
        final int finalToScreen = toScreen - Workspace.getWorkspacePrefixScreenSize();
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Move items from screen " + fromScreen + " to screen " + toScreen);
        }
        if (fromScreen == toScreen) {
            return;
        }
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings.Favorites.getContentUri(context, false);
        final ContentValues values = new ContentValues();
         DaemonThread.postThreadTask(new Runnable() {
            @Override
            public void run() {
                values.put(LauncherSettings.Favorites.SCREEN, Integer.MIN_VALUE);
                cr.update(uri, values, LauncherSettings.Favorites.SCREEN + " = " + finalFromScreen
                        + " and " + LauncherSettings.Favorites.CONTAINER + " = "
                        + LauncherSettings.Favorites.CONTAINER_DESKTOP, null);

                if (finalFromScreen < finalToScreen) {
                    for (int i = finalFromScreen + 1; i <= finalToScreen; i++) {
                        values.put(LauncherSettings.Favorites.SCREEN, i - 1);
                        cr.update(uri, values, LauncherSettings.Favorites.SCREEN + " = " + i
                                + " and " + LauncherSettings.Favorites.CONTAINER + " = "
                                + LauncherSettings.Favorites.CONTAINER_DESKTOP, null);
                    }
                } else if (finalFromScreen > finalToScreen) {
                    for (int i = finalFromScreen - 1; i >= finalToScreen; i--) {
                        values.put(LauncherSettings.Favorites.SCREEN, i + 1);
                        cr.update(uri, values, LauncherSettings.Favorites.SCREEN + " = " + i
                                + " and " + LauncherSettings.Favorites.CONTAINER + " = "
                                + LauncherSettings.Favorites.CONTAINER_DESKTOP, null);
                    }
                }

                values.put(LauncherSettings.Favorites.SCREEN, finalToScreen);
                cr.update(uri, values, LauncherSettings.Favorites.SCREEN + " = "
                        + Integer.MIN_VALUE + " and " + LauncherSettings.Favorites.CONTAINER
                        + " = " + LauncherSettings.Favorites.CONTAINER_DESKTOP, null);
            }
        });
    }

    
    /*区分db访问半成品 by snsermail@gmail.com*/
    public static void syncHomeDesktopItemInfoInDatabase(final Context context, final HomeDesktopItemInfo item) {
        //XLog.d(TAG, "Sync home item " + item.toString());
        
        final Uri uri = LauncherSettings.Favorites.getContentUri(context, item.id, false);
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        values.put(LauncherSettings.Favorites.TITLE, item.getTitle());
        values.put(IphoneUtils.FavoriteExtension.LAST_UPDATE_TIME, item.lastUpdateTime);
        values.put(IphoneUtils.FavoriteExtension.LAST_CALLED_TIME, item.lastCalledTime);
        values.put(IphoneUtils.FavoriteExtension.CALLED_NUM, item.calledNum);
        values.put(IphoneUtils.FavoriteExtension.STORAGE, item.storage);
        values.put(IphoneUtils.FavoriteExtension.SYSTEM, item.system ? 1 : 0);

        convertScreenNumber(context, item, values);

        cr.update(uri, values, null, null);
    }

    /*不用区分db访问 by snsermail@gmail.com*/
    public static HomeDesktopItemInfo addShortcutToDesktop(final Context context, Intent data,
            final int screen, final int cellX, final int cellY, final boolean notify, boolean addBorder, boolean fromReceiver) {

        final HomeDesktopItemInfo info = infoFromShortcutIntent(context, data, addBorder);

        addItemToDatabase(context, info, LauncherSettings.Favorites.CONTAINER_DESKTOP,
                screen, cellX, cellY, notify);

        return info;
    }

    /*不用区分db访问 by snsermail@gmail.com*/
    public static HomeDesktopItemInfo infoFromShortcutIntent(Context context, Intent data, boolean addBoard) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = StringUtils.trimString(data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        if (intent != null && "android.intent.action.CALL_PRIVILEGED".equals(intent.getAction())) {
            intent.setAction("android.intent.action.CALL");
        }

        Bitmap icon = null;
        Bitmap shortcutIconBitmap = null;
        ShortcutIconResource iconResource = null;

        if (bitmap != null && bitmap instanceof Bitmap) {
            shortcutIconBitmap = (Bitmap) bitmap;
            icon = WorkspaceIconUtils.createIconBitmap(new FastBitmapDrawable(shortcutIconBitmap), context, addBoard, true);
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    iconResource = (ShortcutIconResource) extra;
                    if (Constant.PACKAGE_NAME.equals(iconResource.packageName)) {
                        Resources resources = context.getResources();
                        final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                        icon = WorkspaceIconUtils.createIconBitmap(Utilities.getDrawableDefault(context, id, true), context, addBoard, false);
                    } else {
                        final PackageManager packageManager = context.getPackageManager();
                        Resources resources = packageManager.getResourcesForApplication(
                                iconResource.packageName);
                        final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                        icon = WorkspaceIconUtils.createIconBitmap(resources.getDrawable(id), context, addBoard, true);
                    }
                } catch (Exception e) {
                    XLog.w(TAG, "Could not load shortcut icon: " + extra);
                }
            }
        }

        final HomeDesktopItemInfo info = new HomeDesktopItemInfo();

        if (icon == null) {
            icon = IconCache.getInstance(context).getDefaultIcon();
            info.usingFallbackIcon = true;
        }

        info.setTitle(name);
        info.setIcon(new FastBitmapDrawable(icon));
        info.intent = intent;
        info.shortcutIconBitmap = shortcutIconBitmap;
        info.iconResource = iconResource;
        info.iconType = shortcutIconBitmap != null ? LauncherSettings.BaseLauncherColumns.ICON_TYPE_BITMAP
                : LauncherSettings.BaseLauncherColumns.ICON_TYPE_RESOURCE;

        return info;
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    public static void addHiddenApplicationToDatabase(final Context context, final HiddenApplication item, final boolean notify, final boolean sync) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Add hidden application with intent: " + item.intent);
        }
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        item.onAddToDatabase(values);

        if (sync) {
            Uri result = cr.insert(LauncherSettings.AppHideList.getContentUri(notify), values);

            if (result != null) {
                item.id = Integer.parseInt(result.getPathSegments().get(2));
            }
        } else {
             DaemonThread.postThreadTask(new Runnable() {
                @Override
                public void run() {
                    Uri result = cr.insert(LauncherSettings.AppHideList.getContentUri(notify),
                            values);

                    if (result != null) {
                        item.id = Integer.parseInt(result.getPathSegments().get(2));
                    }
                }
            });
        }
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    public static void deleteHiddenApplicationFromDatabase(final Context context, final long id, final boolean notify, final boolean sync) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "Delete hidden application with id: " + id);
        }
        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = LauncherSettings.AppHideList.getContentUri(id, notify);

        if (sync) {
            cr.delete(uriToDelete, null, null);
        } else {
             DaemonThread.postThreadTask(new Runnable() {
                @Override
                public void run() {
                    cr.delete(uriToDelete, null, null);
                }
            });
        }
    }

    /*区分db访问完成 by snsermail@gmail.com*/
    /**
     * 获取桌面元素所处最大的屏幕数
     * @param context
     * @return
     */
    public static int getMaxScreenCount(Context context) {
        final ContentResolver contentResolver = context.getContentResolver();

        Cursor c = null;
        int maxScreenCount = -1;

        final int xCount = SettingPreferences.getHomeLayout(context)[1];
        final int yCount = SettingPreferences.getHomeLayout(context)[0];
        try {
            c = contentResolver.query(LauncherSettings.Favorites.getContentUri(context, true),
                    new String[] {
                        "max(" + LauncherSettings.Favorites.SCREEN + ")"
                    }, LauncherSettings.Favorites.CONTAINER + "="
                            + LauncherSettings.Favorites.CONTAINER_DESKTOP + " and "
                            + LauncherSettings.Favorites.CELLX + "<" + xCount + " and "
                            + LauncherSettings.Favorites.CELLY + "<" + yCount, null, null);

            if (c != null && c.moveToFirst()) {
                if (!c.isNull(0)) {
                    maxScreenCount = c.getInt(0);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return maxScreenCount + 1;
    }
    
    /*不用区分db访问 by snsermail@gmail.com*/
    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectArgs, String sortOrder) {
        final ContentResolver cr = context.getContentResolver();
        return cr.query(uri, projection, selection, selectArgs, sortOrder);
    }
}
