package cc.snser.launcher.iphone.model;

import android.content.Context;
import android.net.Uri;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.apps.model.workspace.HomeItemInfo;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.style.SettingPreferences;

public class IphoneUtils {

	public static class FavoriteExtension extends LauncherSettings.Favorites {

		/**
		 * The content:// style URL for this table
		 */

		public static Uri getContentUri(Context context, long id, boolean notify) {
			return Uri.parse("content://" + LauncherProvider.getAuthority()
					+ "/" + LauncherProvider.TABLE_FAVORITES_I + "/" + id + "?"
					+ LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
		}

		public static Uri getContentUri(Context context, boolean notify) {
			return Uri.parse("content://" + LauncherProvider.getAuthority()
					+ "/" + LauncherProvider.TABLE_FAVORITES_I + "?"
					+ LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
		}

		public static final String LAST_UPDATE_TIME = "last_update_time";

		/**
		 * The last called time the application
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String LAST_CALLED_TIME = "last_called_time";

		/**
		 * The call number the application
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String CALLED_NUM = "called_num";

		/**
		 * storage of the application.
		 * <P>
		 * if it is installed in sd card , but the sd card has been unmounted,
		 * this value will be 1. otherwise, will be 0.
		 * </P>
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String STORAGE = "storage";

		/**
		 * whether the application is a system app.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String SYSTEM = "system";

		/**
		 * the folder category
		 * <p>
		 * Folders are created by user or 'desktop assort' function, -1 means it
		 * is a user generated folder, 0 or positive value indicate its category
		 * set by 'desktop assort', and any change of folder title or folder
		 * content will set it -1.
		 * </p>
		 * <p>
		 * Type: INTEGER
		 * </p>
		 */
		public static final String CATEGORY = "category";
	}

	public static boolean fillPosition(HomeItemInfo info, Workspace workspace,
			boolean forceReposition, int targetScreen) {
		if (forceReposition
				|| IphoneItemInfoUtils.isItemInfoNotPositioned(info)) {
			int container = LauncherSettings.Favorites.CONTAINER_DESKTOP; // item的container有可能不为desktop（比如取消隐藏应用），因此这里重新设一下
			boolean forceNewScreen = workspace.getDefaultScreen() == workspace
					.getScreenCount() - 1
					&& workspace.getDefaultScreen() == Workspace.NEW_DEFAULT_DEFAULT_SCREEN;
			if (info.spanX == 1 && info.spanY == 1) {
				// LauncherModel make ui to find a suitable location for the
				// icon.
				int[] cellPosition = workspace.getFirstEmptyCellLocation(
						targetScreen, forceNewScreen);

				int screen = cellPosition[0];
				int cellX = cellPosition[1];
				int cellY = cellPosition[2];

				IphoneItemInfoUtils.setPositionInfo(info, container, screen,
						cellX, cellY);
				return true;
			} else {
				int[] layout = SettingPreferences.getHomeLayout(workspace.getContext());
				if (info.spanX > layout[0] || info.spanY > layout[1]) {
					// TODO 空间不够放widget的，先不显示了
					return false;
				}
				// 查找当前屏幕有没有空间
				int screen = targetScreen;
				int[] location = workspace.getSpaceScreenAvailableFor(screen,
						info.spanX, info.spanY);
				if (location == null) {
					screen = workspace.getScreenCount() - 1;
					location = workspace.getSpaceScreenAvailableFor(screen,
							info.spanX, info.spanY);
				}
				if (location == null) {
					screen = workspace.getScreenCount();
					location = new int[] { 0, 0 };
				}
				if (location != null) {
					IphoneItemInfoUtils.setPositionInfo(info, container,
							screen, location[0], location[1]);
					return true;
				}
			}
		}
		return false;
	}

}
