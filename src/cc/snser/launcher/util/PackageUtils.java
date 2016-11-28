package cc.snser.launcher.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Browser;
import android.text.TextUtils;
import cc.snser.launcher.App;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.ActionUtils;
import cc.snser.launcher.apps.utils.AppUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.ChannelUtils;

import java.io.InputStream;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * utils for package launch, install, uninstall and so on
 * @author yangkai
 *
 */
public class PackageUtils {

    private static final String TAG = "Launcher.PackageUtils";
    
    private static int sSelfVersionCode = -1;
    private static String sSelfVersionName;

    private PackageUtils() {
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * start a application from the intent
     * @param context
     * @param intent
     * @return true if start succeed
     */
    public static void launchIntent(Context context, Intent intent) {
        if (context instanceof Launcher) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "context instanceof Launcher startActivitySafely intent " + intent + " action " + intent.getAction());
            }
            ((Launcher) context).startActivitySafely(intent, null);
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        boolean succeed = ActionUtils.startActivitySafely(context, intent);
        if (LOGD_ENABLED) {
            XLog.d(TAG, "startActivitySafely return : " + succeed);
        }
        if (succeed) {
            App app = (App) context.getApplicationContext();
            if ( app != null ){
            	if ( null != app.getModel() )
            		app.getModel().updateAppCalledNum(intent);
            }
        }
    }

    public static void launchAppIntent(Context context, String packageName) {
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent it = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(it);
        } catch (Exception e) {
            // ingore
        }
    }

    public static boolean launchMarketIntentIfNecessary(Context context, String packageName) {
        if (!Utils.shouldUseMarketDownload()) {
            return false;
        }
        launchMarketIntent(context, packageName);
        return true;
    }

    public static void launchPlayIntent(Context context) {
        Intent intent = AppUtils.getLaunchMainIntentForPackage(context, ChannelUtils.GOOGLE_PLAY_PACKAGE_NAME);
        if (intent != null) {
            ActionUtils.startActivitySafely(context, intent);
        } else {
            launchBrowserIntent(context, "https://play.google.com/store", false, false);
        }
    }

    public static void launchMarketIntent(Context context, String packageName) {
        if (AppUtils.isPackageExists(context, ChannelUtils.GOOGLE_PLAY_PACKAGE_NAME)) {
            Uri uri = Uri.parse(ChannelUtils.MARKET_PACKAGE_INTENT_URL + packageName);
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            it.setPackage(ChannelUtils.GOOGLE_PLAY_PACKAGE_NAME);
            try {
                context.startActivity(it);
                return;
            } catch (Exception e) {
                // ingore
            }
        }

        launchBrowserIntent(context, ChannelUtils.PLAY_URL + packageName, false, false);
    }

    public static void launchBrowserIntent(Context context, String link, boolean addNewTaskFlag, boolean addResetTask) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.addCategory(Intent.CATEGORY_DEFAULT);
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        browserIntent.setData(Uri.parse(link));
        browserIntent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());

        if (addNewTaskFlag) {
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        if (addResetTask) {
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }

        launchIntent(context, browserIntent);
    }

    public static int getPackageVersionCode(Context context, String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionCode;
        } catch (NameNotFoundException e) {
            return 0;
        }
    }

    public static String getPackageVersionName(Context context, String packageName) {
        String defaultVersionName = "1.0.0";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            return versionName != null ? versionName : versionCode != 0 ? String.valueOf(versionCode) : defaultVersionName;
        } catch (NameNotFoundException e) {
            return defaultVersionName;
        }
    }
    
    public int getSelfVersionCode(Context context) {
        if (sSelfVersionCode <= 0) {
            sSelfVersionCode = getPackageVersionCode(context, context.getPackageName());
        }
        return sSelfVersionCode;
    }
    
    public static String getSelfVersionName(Context context) {
        if (TextUtils.isEmpty(sSelfVersionName)) {
            sSelfVersionName = getPackageVersionName(context, context.getPackageName());
        }
        return sSelfVersionName;
    }
    
    public static String getPackageSignTime(Context context){
    	if(context == null) return null;
    	
    	String result = null;
    	try {
    		InputStream input = context.getAssets().open("info");
    		
    		byte[] buffer = new byte[100];
    		int length = input.read(buffer);
            if(length > 0){
            	String date = EncodingUtils.getAsciiString(buffer);
            	result = date;
            }
		} catch (Exception e) {
			//ignore this exception
		}
    	return result;
    }
    
    public static String getPackageSign(Context context,String packageName) {
    	try {
    		/*String test = "";
    		PackageManager pm = context.getPackageManager();
            PackageInfo apps = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            test = apps.signatures[0].toString();*/
		} catch (Exception e) {
		}
        return null;
    }
 

    public static final String SINA_ADDRESS = "http://weibo.com/launcher";
    public static final String SINA_ADDRESS_ID = "2513171644";

    public static void jumpToWeibo(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("sinaweibo://userinfo?uid=" + SINA_ADDRESS_ID);
            intent.setData(uri);
            List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(intent, 0);
            if (infos != null && infos.size() > 0) {
                Intent chooseIntent = Intent.createChooser(intent, "Weibo");
                context.startActivity(chooseIntent);
                return;
            }
        } catch (Exception e) {
        }
        PackageUtils.launchBrowserIntent(context, SINA_ADDRESS, false, false);
    }

    public static boolean startActivitySafely(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isApkValid(Context context, String file) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkInfo = pm.getPackageArchiveInfo(file, PackageManager.GET_CONFIGURATIONS);
            return pkInfo != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
