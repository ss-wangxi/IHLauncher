
package cc.snser.launcher.features.shortcut;

import cc.snser.launcher.App;
import cc.snser.launcher.Constant;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.support.settings.GestureSettings;
import cc.snser.launcher.support.settings.GestureType;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.btime.launcher.R;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class CustomShortcutActionInfo extends HomeDesktopItemInfo {
    public final ShortcutIconResource iconResource;

    public CustomShortcutActionInfo(Context context, int titleResId, int iconResId, String actionType, boolean lazyLoad) {
        iconResource = ShortcutIconResource.fromContext(context, iconResId);

        if (!lazyLoad) {
            this.defaultTitle = context.getString(titleResId);
            boolean addBoard = false;
            Bitmap bitmap = WorkspaceIconUtils.createIconBitmap(Utilities.getDrawableDefault(context, iconResId, true), context, addBoard, false);
            this.defaultIcon = new FastBitmapDrawable(bitmap);
            this.intent = new Intent(Constant.LAUNCHER_CUSTOM_SHORTCUT_ACTION);
            this.intent.setType(actionType);
        }
    }

    public static List<GestureSettings> allForGestureSetting(Context context, GestureType gestureType) {
        List<GestureSettings> all = new ArrayList<GestureSettings>();
        return all;
    }

    public static List<CustomShortcutActionInfo> all(Context context, boolean lazyLoad) {
        List<CustomShortcutActionInfo> all = new ArrayList<CustomShortcutActionInfo>();
               
        all.add(new CustomShortcutActionInfo(context, R.string.custom_shortcut_action_launcher_settings, R.drawable.icon_launcher_settings, CustomShortcutAction.ACTION_TYPE_LAUNCHER_SETTINGS, lazyLoad));
        all.add(new CustomShortcutActionInfo(context, R.string.custom_shortcut_action_app_store, R.drawable.icon_appstore, CustomShortcutAction.ACTION_TYPE_APP_STORE, lazyLoad));
        return all;
    }
}
