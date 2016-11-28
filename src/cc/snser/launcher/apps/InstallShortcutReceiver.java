package cc.snser.launcher.apps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cc.snser.launcher.App;
import cc.snser.launcher.DeferredHandler;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.iphone.model.LauncherModelIphone;
import cc.snser.launcher.model.DbManager;
import cc.snser.launcher.model.LauncherModel;
import cc.snser.launcher.model.LauncherModel.Callbacks;
import cc.snser.launcher.screens.Workspace;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";

    private static final String TAG = "Launcher.InstallShortcutReceiver";

    public void onReceive(Context context, Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "receive shortcut install broadcast " + data);
        }

        App application = (App) context.getApplicationContext();

        final WeakReference<Callbacks> oldCallbacks = application.getModelCallbacks();
        final Callbacks callbacks = oldCallbacks != null ? oldCallbacks.get() : null;

        try {
            handle(context, data, application.getDeferredHandler(), oldCallbacks, callbacks);
        } catch (Exception e) {
            XLog.e(TAG, "Faile to handle the add shortcut request.", e);
        }
    }

    private static void handle(Context context, Intent data,
            final DeferredHandler handler, final WeakReference<Callbacks> oldCallbacks,
            final LauncherModel.Callbacks callbacks) {

        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (intent == null) {
            return;
        }

        installShortcutInIphonemode(context, data, handler, oldCallbacks, callbacks);
    }

    private static boolean installShortcutInIphonemode(Context context, Intent data,
            final DeferredHandler handler, final WeakReference<Callbacks> oldCallbacks,
            final LauncherModel.Callbacks callbacks) {

        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);

        if (intent.getAction() == null) {
            intent.setAction(Intent.ACTION_VIEW);
        }

        String name = StringUtils.trimString(data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
        if (name == null) {
            name = "";
        }

        boolean duplicate = false;//!MultiModeController.isContextualModelIphone(context) && data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);
        LauncherModelIphone launcherModel = (LauncherModelIphone) App.getApp().getModel();
        if (!DbManager.isAdShortcutExists(context, intent)
        		&& !launcherModel.isShortcutIntentExist(intent, name) 
        		&& !shouldIgnoreIntent(intent)) {

            final HomeDesktopItemInfo info = DbManager.infoFromShortcutIntent(context, data, true);
            if (info.usingFallbackIcon == true) {
				return true;
			}
            final List<HomeDesktopItemInfo> addData = new ArrayList<HomeDesktopItemInfo>();
            addData.add(info);

            ((LauncherModelIphone)App.getApp().getModel()).addItem(info, false);
            if (oldCallbacks != null) {
                handler.post(new Runnable() {
                    public void run() {
                        if (callbacks == oldCallbacks.get() && callbacks != null) {
                            callbacks.bindAppsAdded(addData, Workspace.sInEditMode, true);
                        }
                    }
                });
            }
            //Bug 5554 - 0429【手机桌面】安装完成app后提示桌面已经创建快捷方式
            //ToastUtils.showMessage(context, context.getString(R.string.shortcut_installed, name),
            //        Toast.LENGTH_SHORT);
            if (LOGD_ENABLED) {
                XLog.d(TAG, "Created shortcut[" + intent + "] is added duplicate: " + duplicate);
            }
            return true;
        } else {
        	//Bug 5554 - 0429【手机桌面】安装完成app后提示桌面已经创建快捷方式
            //ToastUtils.showMessage(context, context.getString(R.string.shortcut_duplicate, name),
            //        Toast.LENGTH_SHORT);
            if (LOGD_ENABLED) {
                XLog.d(TAG, "Created shortcut[" + intent + "] is duplicated and ignored");
            }
            return false;
        }
    }

    public static boolean shouldIgnoreIntent(Intent intent) {
        return false;
    }
}
