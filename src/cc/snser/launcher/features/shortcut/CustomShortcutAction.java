
package cc.snser.launcher.features.shortcut;

import cc.snser.launcher.Constant;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;

public class CustomShortcutAction {
    public static final String ACTION_TYPE_APP_STORE = "custom_shortcut_action_app_store";

    public static final String ACTION_TYPE_LAUNCHER_SETTINGS = "custom_shortcut_action_launcher_settings";

    // 桌面菜单
    public static final String ACTION_TYPE_WORKSPACE_MENU = "custom_shortcut_action_type_workspace_menu";

    // 打开通知栏
    public static final String ACTION_TYPE_OPEN_NOTIFICATION_BAR = "custom_shortcut_action_type_open_notification_bar";

    // T9搜索
    public static final String ACTION_TYPE_SERACH_BY_T9 = "custom_shortcut_action_type_search_by_t9";

    // 索引搜索
    public static final String ACTION_TYPE_SERACH_BY_INDEX = "custom_shortcut_action_type_search_by_index";

    // 一键锁屏
    public static final String ACTION_TYPE_SCREEN_LOCK = "custom_shortcut_action_type_screen_lock";
    
    //风格切换
    public static final String ACTION_TYPE_STYLE_SWITCH = "custom_shortcut_action_type_style_switch";

    public static final boolean supports(String actionType) {
        if (ACTION_TYPE_APP_STORE.equals(actionType)) {
            return true;
        } else if (ACTION_TYPE_LAUNCHER_SETTINGS.equals(actionType)) {
            return true;
        } else if (ACTION_TYPE_OPEN_NOTIFICATION_BAR.equals(actionType)) {
            return true;
        } else if (ACTION_TYPE_WORKSPACE_MENU.equals(actionType)) {
            return true;
        } else if (ACTION_TYPE_SERACH_BY_T9.equals(actionType)) {
            return true;
        } else if (ACTION_TYPE_SERACH_BY_INDEX.equals(actionType)) {
            return true;
        } else if (ACTION_TYPE_STYLE_SWITCH.equals(actionType)) {
        	return true;
        }
        
        
        
        /*else if (ACTION_TYPE_SCREEN_LOCK.equals(actionType)) {
            return true;
        }*/
        
        return false;
    }

    public static boolean isPresetShortcut(HomeDesktopItemInfo homeDesktopItemInfo) {
        if (homeDesktopItemInfo == null || homeDesktopItemInfo.getIntent() == null) {
            return false;
        }
        return homeDesktopItemInfo.isShortcut() && Constant.LAUNCHER_CUSTOM_SHORTCUT_ACTION.equals(homeDesktopItemInfo.getIntent().getAction());
    }
}
