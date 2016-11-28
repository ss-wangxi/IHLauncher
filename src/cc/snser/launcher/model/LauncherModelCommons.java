package cc.snser.launcher.model;

import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.iphone.model.IphoneUtils;
import android.database.Cursor;

public class LauncherModelCommons {
    private static int getColumnIndex(Cursor c, String columnName, boolean throwExceptionIfNotFound) {
        if (throwExceptionIfNotFound) {
            return c.getColumnIndexOrThrow(columnName);
        } else {
            return c.getColumnIndex(columnName);
        }
    }

    public static class AppHideListColumnIndex {
        public final int idIndex;
        public final int intentIndex;

        public AppHideListColumnIndex(Cursor c) {
            this(c, true);
        }

        public AppHideListColumnIndex(Cursor c, boolean throwExceptionIfNotFound) {
            idIndex = getColumnIndex(c, LauncherSettings.AppHideList._ID, throwExceptionIfNotFound);
            intentIndex = getColumnIndex(c, LauncherSettings.AppHideList.INTENT, throwExceptionIfNotFound);
        }
    }

    public static class FavoritesColumnIndex {
        public final int idIndex;
        public final int intentIndex;
        public final int titleIndex;
        public final int iconTypeIndex;
        public final int iconIndex;
        public final int iconPackageIndex;
        public final int iconResourceIndex;
        public final int titlePackageIndex;
        public final int titleResourceIndex;
        public final int containerIndex;
        public final int itemTypeIndex;
        public final int appWidgetIdIndex;
        public final int screenIndex;
        public final int cellXIndex;
        public final int cellYIndex;
        public final int spanXIndex;
        public final int spanYIndex;
        public final int lastUpdateTimeIndex;
        public final int lastCalledTimeIndex;
        public final int calledNumIndex;
        public final int storageIndex;
        public final int systemIndex;
        public final int categoryIndex;

        public FavoritesColumnIndex(Cursor c) {
            this(c, true);
        }

        public FavoritesColumnIndex(Cursor c, boolean throwExceptionIfNotFound) {
            idIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension._ID, throwExceptionIfNotFound);
            intentIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.INTENT, throwExceptionIfNotFound);
            titleIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.TITLE, throwExceptionIfNotFound);
            iconTypeIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.ICON_TYPE, throwExceptionIfNotFound);
            iconIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.ICON, throwExceptionIfNotFound);
            iconPackageIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.ICON_PACKAGE, throwExceptionIfNotFound);
            iconResourceIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.ICON_RESOURCE, throwExceptionIfNotFound);
            titlePackageIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.TITLE_PACKAGE, throwExceptionIfNotFound);
            titleResourceIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.TITLE_RESOURCE, throwExceptionIfNotFound);
            containerIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.CONTAINER, throwExceptionIfNotFound);
            itemTypeIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.ITEM_TYPE, throwExceptionIfNotFound);
            appWidgetIdIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.APPWIDGET_ID, throwExceptionIfNotFound);
            screenIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.SCREEN, throwExceptionIfNotFound);
            cellXIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.CELLX, throwExceptionIfNotFound);
            cellYIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.CELLY, throwExceptionIfNotFound);
            spanXIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.SPANX, throwExceptionIfNotFound);
            spanYIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.SPANY, throwExceptionIfNotFound);
            lastUpdateTimeIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.LAST_UPDATE_TIME, throwExceptionIfNotFound);
            lastCalledTimeIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.LAST_CALLED_TIME, throwExceptionIfNotFound);
            calledNumIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.CALLED_NUM, throwExceptionIfNotFound);
            storageIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.STORAGE, throwExceptionIfNotFound);
            systemIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.SYSTEM, throwExceptionIfNotFound);
            categoryIndex = getColumnIndex(c, IphoneUtils.FavoriteExtension.CATEGORY, throwExceptionIfNotFound);
        }
    }
}
