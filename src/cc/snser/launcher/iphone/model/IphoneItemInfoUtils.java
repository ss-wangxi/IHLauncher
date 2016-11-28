package cc.snser.launcher.iphone.model;

import android.content.ContentValues;
import android.database.Cursor;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.apps.model.workspace.LauncherAppWidgetInfo;
import cc.snser.launcher.apps.model.workspace.LauncherWidgetViewInfo;
import cc.snser.launcher.model.LauncherModelCommons.FavoritesColumnIndex;
import cc.snser.launcher.screens.Workspace;

public class IphoneItemInfoUtils {

    public static void loadFromDatabase(LauncherWidgetViewInfo info, Cursor c, FavoritesColumnIndex columnIndex) {
        commonLoad(info, c, columnIndex);
    }

    public static void loadFromDatabase(LauncherAppWidgetInfo info, Cursor c, FavoritesColumnIndex columnIndex) {
        commonLoad(info, c, columnIndex);
    }

    public static void loadFromDatabase(HomeDesktopItemInfo info, Cursor c, FavoritesColumnIndex columnIndex) {
        commonLoad(info, c, columnIndex);

        info.system = c.getInt(columnIndex.systemIndex) == 1;
        info.storage = c.getInt(columnIndex.storageIndex);

        info.lastUpdateTime = c.getLong(columnIndex.lastUpdateTimeIndex);

        info.lastCalledTime = c.getLong(columnIndex.lastCalledTimeIndex);
        info.calledNum = c.getInt(columnIndex.calledNumIndex);
        info.category = c.getInt(columnIndex.categoryIndex);
    }

    private static void commonLoad(HomeItemInfo info, Cursor c, FavoritesColumnIndex columnIndex) {

        int dbScreen = c.getInt(columnIndex.screenIndex);
        int screen = dbScreen >= 0 ? dbScreen + Workspace.getWorkspacePrefixScreenSize() : dbScreen;
        //
        info.id = c.getLong(columnIndex.idIndex);
        //
        info.container = c.getInt(columnIndex.containerIndex);
        info.screen = screen;
        info.cellX = c.getInt(columnIndex.cellXIndex);
        info.cellY = c.getInt(columnIndex.cellYIndex);
        //
        info.spanX = c.getInt(columnIndex.spanXIndex);
        info.spanY = c.getInt(columnIndex.spanYIndex);
        //
        info.category = c.getInt(columnIndex.categoryIndex);
    }

    public static void onAddToDatabase(HomeItemInfo itemInfo, ContentValues values) {
        values.put(IphoneUtils.FavoriteExtension.CATEGORY, itemInfo.category);
        if (itemInfo instanceof HomeDesktopItemInfo) {
            onAddToDatabase((HomeDesktopItemInfo) itemInfo, values);
        }
    }

    private static void onAddToDatabase(HomeDesktopItemInfo itemInfo, ContentValues values) {
        values.put(IphoneUtils.FavoriteExtension.SYSTEM, itemInfo.system ? 1 : 0);
        if (!itemInfo.isShortcut()) {
            values.put(IphoneUtils.FavoriteExtension.STORAGE, itemInfo.storage);

            if (itemInfo.lastUpdateTime <= 0) {
                values.putNull(IphoneUtils.FavoriteExtension.LAST_UPDATE_TIME);
            } else {
                values.put(IphoneUtils.FavoriteExtension.LAST_UPDATE_TIME, itemInfo.lastUpdateTime);
            }
        }

        storeInvokeInfo(itemInfo, values);
    }

    /**
     * 工具函数，协助完成存储/加载过程
     * @param values
     */
    private static final void storeInvokeInfo(HomeDesktopItemInfo itemInfo, ContentValues values) {
        if (itemInfo.lastUpdateTime <= 0) {
            values.putNull(IphoneUtils.FavoriteExtension.LAST_CALLED_TIME);
        } else {
            values.put(IphoneUtils.FavoriteExtension.LAST_CALLED_TIME, itemInfo.lastCalledTime);
        }
        if (itemInfo.calledNum <= 0) {
            values.putNull(IphoneUtils.FavoriteExtension.CALLED_NUM);
        } else {
            values.put(IphoneUtils.FavoriteExtension.CALLED_NUM, itemInfo.calledNum);
        }
    }


    public static boolean isItemInfoNotPositioned(HomeItemInfo itemInfo) {
        return itemInfo.screen == -1 && itemInfo.cellX == -1 && itemInfo.cellY == -1;
    }

    /**
     * 清除位置信息
     */
    public static final void cleanPosition(HomeItemInfo info) {
        info.container = -1;
        info.screen = -1;
        info.cellX = -1;
        info.cellY = -1;
    }

    public static final void setPositionInfo(HomeItemInfo info, int container, int screen, int cellX, int cellY) {
        info.container = container;
        info.screen = screen;
        info.cellX = cellX;
        info.cellY = cellY;
    }

}
