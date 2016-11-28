package cc.snser.launcher.apps.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import cc.snser.launcher.App;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.support.report.StatManager;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.DialogUtils;
import com.shouxinzm.launcher.util.ToastUtils;

import java.io.File;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class AppUtils {

    public static Intent getLaunchMainIntentForPackage(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();

        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
        intentToResolve.setPackage(packageName);
        ResolveInfo resolveInfo = packageManager.resolveActivity(intentToResolve, 0);

        if (resolveInfo == null) {
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(packageName, resolveInfo.activityInfo.name);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static void uninstallApplicationInfo(final Context context, final AppInfo applicationInfo, int requestCode) {
        PackageManager packageManager = context.getPackageManager();
        if (packageManager.resolveActivity(applicationInfo.getIntent(), 0) == null) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        App application = (App) context.getApplicationContext();
                        application.getModel().removeApplicationInfo(context, applicationInfo);
                    } else {
                        dialog.dismiss();
                    }
                }
            };

            DialogUtils.showDialog(context, context.getString(
                    R.string.global_warmth_warning), context.getString(
                    R.string.app_delete_activity_not_found), context.getString(
                    R.string.ok), listener, context.getString(
                    R.string.cancel), listener);
        } else {
            AppUtils.uninstallPackage(context,
                    applicationInfo.getIntent().getComponent().getPackageName(), requestCode);
        }
    }

    public static void uninstallPackage(Context context, String packageName) {
        if (LOGD_ENABLED) {
            XLog.d(Utils.TAG, "Try to uninstall package: " + packageName);
        }
        Intent uninstallIntent = AppUtils.createUninstallIntent(context, packageName);
        if (uninstallIntent != null) {
            context.startActivity(uninstallIntent);
        }
    }

    public static void uninstallPackage(Context context, String packageName,
            int requestCode) {
        if (LOGD_ENABLED) {
            XLog.d(Utils.TAG, "Try to uninstall package: " + packageName);
        }
        Intent uninstallIntent = AppUtils.createUninstallIntent(context, packageName);
        if (uninstallIntent != null) {
            if (context instanceof Activity) {
            	StatManager.mUninstallPkgName = packageName;
                ((Activity) context).startActivityForResult(uninstallIntent, requestCode);
            } else {
                context.startActivity(uninstallIntent);
            }
        }
    }

    public static Intent createUninstallIntent(Context context,
            String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if ((appInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                ToastUtils.showMessage(context, R.string.uninstall_system_app_alert);
                return null;
            }
        } catch (NameNotFoundException e) {
            ToastUtils.showMessage(context, R.string.uninstall_app_not_exist_alert);
            return null;
        }
        Intent uninstallIntent = new Intent();
        uninstallIntent.setAction(Intent.ACTION_DELETE);
        uninstallIntent.setData(Uri.parse("package:" + packageName));
        return uninstallIntent;
    }

    public static boolean isPackageExists(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return appInfo != null;
        } catch (NameNotFoundException e) {
            // ignore
        }
        return false;
    }
    
    public static boolean isUserApp(ResolveInfo ri) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (ri.activityInfo.applicationInfo.flags & mask) == 0;
    }
    
    public static void installPackage(Context context, File target) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(target);
        String type = "application/vnd.android.package-archive";
        intent.setDataAndType(uri, type);
        context.startActivity(intent);
    }

    /**
     * Install an APK file with callback.
     * <p>
     * This method will first install the APK file, then call method
     * {@link android.app.Activity#onActivityResult(int requestCode, int resultCode, Intent data)}
     * on the specified <code>activity</code>, the <code>requestCode</code> will
     * be passed to the method.
     * </p>
     *
     * @param activity
     *            The activity to callback on
     * @param target
     *            APK file to install
     * @param requestCode
     *            Request code to pass to the
     *            {@link android.app.Activity#onActivityResult(int requestCode, int resultCode, Intent data)}
     */
    public static void installPackage(Activity activity, File target,
            int requestCode) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(target);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        activity.startActivityForResult(intent, requestCode);
    }

}