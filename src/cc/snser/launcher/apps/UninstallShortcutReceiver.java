package cc.snser.launcher.apps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.LauncherSettings;

import com.shouxinzm.launcher.util.StringUtils;

public class UninstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_UNINSTALL_SHORTCUT =
            "com.android.launcher.action.UNINSTALL_SHORTCUT";

    public void onReceive(Context context, Intent data) {
        if (!ACTION_UNINSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }

        handle(context, data);
    }

    private static void handle(Context context, Intent data) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = StringUtils.trimString(data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
        boolean duplicate = data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);

        if (intent != null && name != null) {
            final ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(LauncherSettings.Favorites.getContentUri(context, true), new String[] {
                LauncherSettings.Favorites._ID, LauncherSettings.Favorites.INTENT, LauncherSettings.Favorites.ITEM_TYPE
            }, LauncherSettings.Favorites.TITLE + "=?", new String[] {
                name
            }, null);

            final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);

            boolean changed = false;

            try {
                while (c.moveToNext()) {
                    try {
                        if (intent.filterEquals(Intent.parseUri(c.getString(intentIndex), 0))) {
                            int itemType = c.getInt(itemTypeIndex);
                            if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                continue;
                            }
                            final long id = c.getLong(idIndex);
                            final Uri uri = LauncherSettings.Favorites.getContentUri(context, id, false);
                            cr.delete(uri, null, null);
                            changed = true;
                            if (!duplicate) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            } finally {
                c.close();
            }

            if (changed) {
            	
                cr.notifyChange(LauncherSettings.Favorites.getContentUri(context, true), null);
                //Bug 5554 - 0429【手机桌面】安装完成app后提示桌面已经创建快捷方式
                //ToastUtils.showMessage(context, context.getString(R.string.shortcut_uninstalled, name),
                //        Toast.LENGTH_SHORT);
            }
        }
    }
}
