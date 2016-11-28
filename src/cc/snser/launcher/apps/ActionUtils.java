package cc.snser.launcher.apps;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import cc.snser.launcher.Constant;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.RuntimeConfig;
import cc.snser.launcher.apps.utils.AppUtils;
import cc.snser.launcher.util.ReflectionUtils;
import cc.snser.launcher.util.ResourceUtils;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.DeviceUtils;
import com.shouxinzm.launcher.util.ToastUtils;
import com.shouxinzm.launcher.util.WindowManagerUtils;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;

/**
 * Utils to jump to some apps.
 * @author shixiaolei
 *
 */
public class ActionUtils {
    private static final String TAG = "Launcher.actionUtils";

    public static final String INTENT_TYPE_SMS = "vnd.android-dir/mms-sms";
    public static final String INTENT_CONTENT_CONTACT = "content://com.android.contacts/contacts";
    private ActionUtils() {
    }

    /**
     * {@link Context#startActivity}}的包装： 启动一个intent, 会拦截Acitivity不存在、无权限等异常.
     *
     * @return 如果成功启动，返回<code>true</code>，否则返回<code>false</code>.
     */
    public static boolean startActivitySafely(Context context, Intent intent) {
        try {
            XLog.d(TAG, "Intent " + intent + " is started.");
            
            context.startActivity(intent);
            return true;

        } catch (ActivityNotFoundException e) {
            ToastUtils.showMessage(context, R.string.activity_not_found, Toast.LENGTH_SHORT);
            XLog.e(TAG, "Unable to launch. intent=" + intent, e);
            return false;

        } catch (SecurityException e) {
            ToastUtils.showMessage(context, R.string.activity_not_found, Toast.LENGTH_SHORT);
            XLog.e(TAG, "Launcher does not have the permission to launch " + intent, e);
            return false;

        } catch (Throwable e) {
            ToastUtils.showMessage(context, R.string.activity_not_found, Toast.LENGTH_SHORT);
            XLog.e(TAG, "Unexpected error to launch. intent=" + intent, e);
            
            return false;
        }
    }

    public static boolean startActivitySilently(Context context, Intent intent) {
        try {
            XLog.d(TAG, "Intent " + intent + " is started.");
            
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Unable to launch. intent=" + intent, e);
            }
            return false;

        } catch (SecurityException e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Launcher does not have the permission to launch " + intent, e);
            }
            return false;

        } catch (Throwable e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Unexpected error to launch. intent=" + intent, e);
            }
            return false;
        }
    }

    /**
     * {@link Activity#startActivityForResult}}的包装： 启动一个intent, 会拦截Acitivity不存在、无权限等异常.
     *
     * @return 如果成功启动， 返回<code>true</code>，否则返回<code>false</code>.
     */
    public static boolean startActivityForResultSafely(Activity context, Intent intent, int requestCode) {
        try {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "Intent " + intent + " with requestCode " + requestCode + " is started.");
            }

            context.startActivityForResult(intent, requestCode);
            return true;

        } catch (ActivityNotFoundException e) {
            ToastUtils.showMessage(context, R.string.activity_not_found, Toast.LENGTH_SHORT);
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Unable to launch. intent=" + intent, e);
            }
            return false;

        } catch (SecurityException e) {
            ToastUtils.showMessage(context, R.string.activity_not_found, Toast.LENGTH_SHORT);
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Launcher does not have the permission to launch " + intent, e);
            }
            return false;

        } catch (Throwable e) {
            ToastUtils.showMessage(context, R.string.activity_not_found, Toast.LENGTH_SHORT);
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Unexpected error to launch. intent=" + intent, e);
            }
            return false;
        }
    }

    private static final String[] MUSIC_PACKAGE_CANDIDATES = new String[] {
        "com.sonyericsson.playnowstore.android",
        "com.htc.music",
        "com.miui.player",
        "com.sec.android.app.music",
        "com.google.android.music",
        "com.motorola.cmp",
        "com.android.music",
        "com.meizu.media.music",
        "com.oppo.music",
        "cn.nubia.music.preset",
        "com.baidu.musicplayer"
    };

    private static ComponentName findMainComponent(PackageManager packageManager, String packageName) {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);

        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        if (apps != null && !apps.isEmpty()) {
            ResolveInfo info = apps.get(0);

            return new ComponentName(info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);
        }

        return null;
    }

    /**
     * try to validate the component having launcher main action
     * 判断一个package/activity是否满足如下条件（能在Launcher中显示并能够加载），如果不是，则试图找到一个合适的返回
     * action android:name="android.intent.action.MAIN"
     * category android:name="android.intent.category.LAUNCHER"
     * @param packageManager
     * @param action
     * @param packageName
     * @param className
     * @param mainIntentForQuery
     * @return
     */
    private static String convertLauncherActivity(PackageManager packageManager, String action, String type, String packageName, String className, Intent mainIntentForQuery) {
        mainIntentForQuery.setPackage(packageName);

        // Android 4.1照相机，特殊处理
        if (DeviceUtils.isApiLevel16()) {
            //MIUI的4.1版本不需要进行相机的特殊修改
            if ("android.media.action.IMAGE_CAPTURE".equals(action)) {
                if ("com.huawei.camera.ThirdCamera".equals(className)) { // 适配联想p770
                    String ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.huawei.camera");
                    if (ret != null && (ret.equals(className) || ret.equals("com.huawei.camera"))) { // 严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                        return ret;
                    }
                } else if ("com.android.hwcamera.ThirdCamera".equals(className)) { // 适配华为荣耀3C
                    String ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.android.hwcamera");
                    if (ret != null && (ret.equals(className) || ret.equals("com.android.hwcamera"))) { // 严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                        return ret;
                    }
                } else if ("com.android.camera.Camera".equals(className)) {
                    String ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.android.camera.CameraLauncher");
                    if (ret != null && (ret.equals(className) || ret.equals("com.android.camera.CameraLauncher"))) { //严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                        return ret;
                    }
                    if ("com.android.camera".equals(packageName) && "com.android.camera.Camera".equals(className)) { // 适配联想p770
                        ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.android.camera.CameraPre");
                        if (ret != null && (ret.equals(className) || ret.equals("com.android.camera.CameraPre"))) { // 严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                            return ret;
                        }
                    }
                } else if ("com.android.hwcamera.Camera".equals(className)) { //适配华为P1,判断目标class与备选地址是否存在
                    String ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.android.hwcamera");
                    if (ret != null && (ret.equals(className) || ret.equals("com.android.hwcamera"))) { // 严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                        return ret;
                    }
                } else if ("com.google.android.gallery3d".equals(packageName)) { // 适配android 4.2.1
                    String ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.android.camera.CameraLauncher");
                    if (ret != null && (ret.equals(className) || ret.equals("com.android.camera.CameraLauncher"))) { // 严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                        return ret;
                    }
                } else if ((DeviceUtils.isHTC609D() || DeviceUtils.isHTCOne() || DeviceUtils.isHTCOneX()) && "com.android.camera".equals(packageName) && "com.android.camera.CameraServiceEntry".equals(className)) { // 适配htc 609D
                    String ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.android.camera.CameraEntry");
                    if (ret != null && (ret.equals(className) || ret.equals("com.android.camera.CameraEntry"))) { // 严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                        return ret;
                    }
                } else if ("com.android.gallery3d".equals(packageName) && "com.android.camera.CameraActivity".equals(className)) { // 适配mx3
                    String ret = queryIntentActivities(packageManager, mainIntentForQuery, className, "com.android.camera.Camera");
                    if (ret != null && (ret.equals(className) || ret.equals("com.android.camera.Camera"))) { // 严格判断，避免queryIntentActivity可能的返回其他图标，导致图标重复
                        return ret;
                    }
                }
            }
        }

        if (Intent.ACTION_DIAL.equals(action)) {
            if ("com.android.contacts".equals(packageName) && "com.android.contacts.activities.PeopleActivity".equals(className)) {
                String candidate2 = "com.android.contacts.activities.DialtactsActivity";
                List<String> allMatchResult = new ArrayList<String>();
                String ret = queryIntentActivities(packageManager, mainIntentForQuery, allMatchResult, candidate2, className);
                if (allMatchResult.contains(candidate2)) {
                    return candidate2;
                }
                if (ret != null) {
                    return ret;
                }
            }
        }

        if (INTENT_TYPE_SMS.equals(type)) {
            if ("com.android.contacts".equals(packageName) && "com.android.contacts.activities.PeopleActivity".equals(className)) {
                String candidate2 = "com.android.mms.ui.ConversationList";
                List<String> allMatchResult = new ArrayList<String>();
                String ret = queryIntentActivities(packageManager, mainIntentForQuery, allMatchResult, candidate2, className);
                if (allMatchResult.contains(candidate2)) {
                    return candidate2;
                }
                if (ret != null) {
                    return ret;
                }
            }
        }

        return queryIntentActivities(packageManager, mainIntentForQuery, className);
    }

    public static String queryIntentActivities(PackageManager packageManager, Intent mainIntentForQuery, String... candidateBestMatchs) {
        return queryIntentActivities(packageManager, mainIntentForQuery, null, candidateBestMatchs);
    }

    private static String queryIntentActivities(PackageManager packageManager, Intent mainIntentForQuery, List<String> allMatchResult, String... candidateBestMatchs) {
        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntentForQuery, 0);
        String ret = null;
        boolean found = false;
        if (apps != null && !apps.isEmpty()) {
            String direct = null;
            String indirect = null;
            for (ResolveInfo app : apps) {
                for (String candiateBestMatch : candidateBestMatchs) {
                    if (app.activityInfo.name.equals(candiateBestMatch)) {
                        if (direct == null) {
                            direct = app.activityInfo.name;
                        }
                        if (allMatchResult != null && !allMatchResult.contains(app.activityInfo.name)) {
                            allMatchResult.add(app.activityInfo.name);
                        }
                        if (allMatchResult == null) {
                            break;
                        }
                    } else if ((app.activityInfo.targetActivity != null && app.activityInfo.targetActivity.equals(candiateBestMatch))) {
                        if (indirect == null) {
                            indirect = app.activityInfo.name;
                        }
                        if (allMatchResult != null && !allMatchResult.contains(app.activityInfo.name)) {
                            allMatchResult.add(app.activityInfo.name);
                        }
                        if (allMatchResult == null) {
                            break;
                        }
                    }
                }
            }
            if (direct != null) {
                found = true;
                ret = direct;
            } else if (indirect != null) {
                found = true;
                ret = indirect;
            } else {
                found = true;
                ret = apps.get(0).activityInfo.name;
            }
        }
        //如果找到了launchable的activity，才返回
        if (found) {
            return ret;
        }
        //否则，返回空
        return null;
    }

    public static ComponentName findBestMatchedComponent(Context context, String packageName, String className,
            String action, String uri, String type, String category) {
        PackageManager packageManager = context.getPackageManager();

        Intent mainIntentForQuery = new Intent(Intent.ACTION_MAIN, null);
        mainIntentForQuery.addCategory(Intent.CATEGORY_LAUNCHER);

        if (packageName == null || packageName.length() == 0) {
            Intent intent = null;
            try {
                if (action != null && uri != null) {
                    intent = new Intent(action, Uri.parse(uri));
                } else if (action != null) {
                    intent = new Intent(action);
                } else if (uri != null) {
                    intent = Intent.parseUri(uri, 0);
                } else {
                    XLog.w(TAG, "Shortcut has no action or uri.");
                    return null;
                }

                if (type != null) {
                    intent.setType(type);
                }
                if (category != null) {
                    intent.addCategory(category);
                }
            } catch (Exception e) {
                XLog.printStackTrace(e);
                //XLog.w(TAG, "Shortcut has malformed uri: " + uri + " for action: " + action + ".", e);
                return null; // Oh well
            }

            if ("android.intent.action.MUSIC_PLAYER".equals(action)) {
                ComponentName candidate = null;

                for (String cadidatePackage : MUSIC_PACKAGE_CANDIDATES) {
                    candidate = findMainComponent(packageManager, cadidatePackage);
                    if (candidate != null) {
                        break;
                    }
                }

                if (candidate != null) {
                    packageName = candidate.getPackageName();
                    className = candidate.getClassName();
                }
            } else {
                ComponentName candidate = null;
                if (DeviceUtils.isNexus() && "image/*".equals(type) && Intent.ACTION_GET_CONTENT.equals(action)) {
                    candidate = findMainComponent(packageManager, "com.google.android.apps.photos");
                }
                if (candidate == null) {
                    candidate = DeviceUtils.resolveActivity(action, uri);
                }
                if (candidate != null) {
                    packageName = candidate.getPackageName();
                    className = candidate.getClassName();
                }
            }

            ResolveInfo bestMatch = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            List<ResolveInfo> allMatches = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if(allMatches == null || allMatches.isEmpty())
            	allMatches = packageManager.queryIntentActivities(intent, PackageManager.GET_INTENT_FILTERS);

            if (packageName == null || packageName.length() == 0) {
                if (allMatches.isEmpty()) {
                    if (Intent.ACTION_DIAL.equals(intent.getAction())) {
                        // re-query
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("tel:"));
                        bestMatch = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                        allMatches = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    } else if (Intent.ACTION_MAIN.equals(intent.getAction())
                            && "vnd.android-dir/mms-sms".equalsIgnoreCase(intent.getType())) {
                        if (DeviceUtils.isYulong()) {
                            packageName = "com.android.mms";
                            className = "com.android.mms.ui.ControlCascadesActivity";
                        } else if (DeviceUtils.isAfterApiLevel19()) {
                            if (AppUtils.isPackageExists(context, "com.google.android.talk")) {
                                packageName = "com.google.android.talk";
                                className = "com.google.android.talk.SigningInActivity";
                            }
                        }
                    }
                }

                if (packageName == null || packageName.length() == 0) {
                    if (allMatches.isEmpty()) {
                        XLog.w(TAG, "Shortcut has no matching application for intent: " + intent + ".");

                        return null;
                    }

                    boolean found = false;
                    if (bestMatch != null) {
                        mainIntentForQuery.setPackage(bestMatch.activityInfo.applicationInfo.packageName);
                        if (queryIntentActivities(packageManager, mainIntentForQuery, bestMatch.activityInfo.name) == null) {
                            bestMatch = null;
                        }
                       
                    }

                    if (bestMatch != null) {
                        if (LOGD_ENABLED) {
                            XLog.d(TAG, "Best matched activity found for intent: " + intent + ", packageName: "
                                    + bestMatch.activityInfo.applicationInfo.packageName + ", className: "
                                    + bestMatch.activityInfo.name + ", there are multi matches: ");
                            for (ResolveInfo ri : allMatches) {
                                XLog.d(TAG, "Matched candidate: " + ri.activityInfo.applicationInfo.packageName + ", "
                                        + ri.activityInfo.name + ", " + ri.loadLabel(packageManager)
                                        + ", is system: " + ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0));

                            }
                        }
                        if ((bestMatch.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            // try to find the best match
                            for (ResolveInfo ri : allMatches) {
                                if (bestMatch.activityInfo.name.equals(ri.activityInfo.name)
                                    && bestMatch.activityInfo.applicationInfo.packageName
                                        .equals(ri.activityInfo.applicationInfo.packageName)) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        if (LOGD_ENABLED) {
                            XLog.d(TAG, "No best matched activity found for intent: " + intent + ", there are multi matches: ");
                            for (ResolveInfo ri : allMatches) {
                                XLog.d(TAG, "Matched candidate: " + ri.activityInfo.applicationInfo.packageName + ", "
                                        + ri.activityInfo.name + ", " + ri.loadLabel(packageManager)
                                        + ", is system: " + ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
                            }
                        }
                    }

                    if (!found) {
                        String targetPackageName = null;

                        if ("android.intent.action.GET_CONTENT".equals(action)) {
                            targetPackageName = "com.cooliris.media";
                        } else {
                            targetPackageName = "com.android";
                        }

                        // try to find the first matched pacakgeName and system app
                        int bestCount = 0;
                        for (ResolveInfo ri : allMatches) {
                            if ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                                continue;
                            }

                            String pkg = ri.activityInfo.applicationInfo.packageName;

                            int count = 0;
                            for (int i = 0; i < targetPackageName.length() && i < pkg.length(); i++) {
                                if (targetPackageName.charAt(i) == pkg.charAt(i)) {
                                    count++;
                                } else {
                                    break;
                                }
                            }

                            if ("android.intent.action.GET_CONTENT".equals(action)) {
                                if (pkg.contains("gallery")) {
                                    count = Math.max(count, 16);
                                } else if (pkg.startsWith("com.android")) {
                                    count = Math.max(count, "com.android".length());
                                } else if (pkg.startsWith("com.sec.android")) {
                                    count = Math.max(count, "com.sec.android".length());
                                }
                            }

                            mainIntentForQuery.setPackage(ri.activityInfo.applicationInfo.packageName);
                            if (queryIntentActivities(packageManager, mainIntentForQuery, ri.activityInfo.name) != null) {
                                count *= 1000;
                            }

                            if (count > bestCount) {
                                bestCount = count;
                                bestMatch = ri;
                                found = true;
                            }
                        }
                    }

                    if (!found) {
                        // try to find the first system app
                        for (ResolveInfo ri : allMatches) {
                            if ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                                found = true;
                                bestMatch = ri;
                                break;
                            }
                        }
                    }

                    if (!found && bestMatch != null && (bestMatch.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        // try to find the best matched non-system app
                        for (ResolveInfo ri : allMatches) {
                            if (bestMatch.activityInfo.name.equals(ri.activityInfo.name)
                                && bestMatch.activityInfo.applicationInfo.packageName
                                    .equals(ri.activityInfo.applicationInfo.packageName)) {
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found) {
                        // pick up the first matched app
                        bestMatch = allMatches.get(0);
                    }

                    packageName = bestMatch.activityInfo.applicationInfo.packageName;
                    className = bestMatch.activityInfo.name;
                }
            }
            
            if("com.taobao.taobao".equals(packageName) && "android.intent.category.BROWSABLE".equals(category))
            {
    			return null;
            }
            
            if ("com.android.contacts".equals(packageName) && "com.sec.android.app.contacts.PhoneBookTopMenuActivity".equals(className)) {
                className = "com.sec.android.app.contacts.DialerEntryActivity";
            }

            //适配部分将"文件管理器"内置为系统程序的机型 的相册
            if ("com.android.filemanager".equals(packageName) && "com.android.filemanager.browser.FilePickOrSaveActivity".equals(className)
                    || "com.speedsoftware.rootexplorer".equals(packageName) && "com.speedsoftware.rootexplorer.RootExplorer".equals(className)) {
                boolean foundGallery = false;
                String tmpPackageName, tmpClassName;
                try {
                    tmpPackageName = "com.cooliris.media";
                    tmpClassName = "com.cooliris.media.Gallery";
                    ComponentName cn = new ComponentName(tmpPackageName, tmpClassName);
                    packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
                    foundGallery = true;
                    packageName = tmpPackageName;
                    className = tmpClassName;
                } catch (NameNotFoundException e) {
                    // ignore
                }
                if (!foundGallery) {
                    if (allMatches.size() > 1) {
                        for (ResolveInfo tempMatchedInfo : allMatches) {
                            tmpPackageName = tempMatchedInfo.activityInfo.applicationInfo.packageName;
                            tmpClassName = tempMatchedInfo.activityInfo.name;
                            if (!tmpPackageName.equals(packageName)) {
                                foundGallery = true;
                                packageName = tmpPackageName;
                                className = tmpClassName;
                                break;
                            }
                        }
                    }
                }
                if (!foundGallery) {
                    return null;
                }
            }

            //适配HTC部分机型里的相册
            if ("com.htc.album".equals(packageName) && "com.htc.album.picker.PickerFolderActivity".equals(className)) {
                boolean foundHtc = false;
                String tmpClassName;
                try {
                    tmpClassName = "com.htc.album.AlbumMain.ActivityMainCarousel";
                    ComponentName cn = new ComponentName(packageName, tmpClassName);
                    packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
                    foundHtc = true;
                    className = tmpClassName;
                } catch (NameNotFoundException e) {
                    // ignore
                }
                if (!foundHtc) {
                    try {
                        tmpClassName = "com.htc.album.AlbumMain.ActivityMainDropList";
                        ComponentName cn = new ComponentName(packageName, tmpClassName);
                        packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
                        foundHtc = true;
                        className = tmpClassName;
                    } catch (NameNotFoundException e) {
                        // ignore
                    }
                }
            }
            
            
            //适配4.4机型里的相册
            if ("com.cyanogenmod.filemanager".equals(packageName) && "com.cyanogenmod.filemanager.activities.PickerActivity".equals(className)) {
                String tmpPackageName, tmpClassName;
                try {
                    tmpPackageName = "com.android.gallery3d";
                    tmpClassName = "com.android.gallery3d.app.GalleryActivity";
                    ComponentName cn = new ComponentName(tmpPackageName, tmpClassName);
                    packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
                    packageName = tmpPackageName;
                    className = tmpClassName;
                } catch (NameNotFoundException e) {
                    // ignore
                }
            }

            // 适配索爱
            if ("com.sonyericsson.playnowstore.android".equals(packageName)) {
                String tmpPackageName;
                String tmpClassName;
                try {
                    tmpPackageName = "com.sonyericsson.music";
                    tmpClassName = "com.sonyericsson.music.PlayerActivity";
                    ComponentName cn = new ComponentName(tmpPackageName, tmpClassName);
                    packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
                    packageName = tmpPackageName;
                    className = tmpClassName;
                } catch (NameNotFoundException e) {
                    // ignore
                }
            }

            if (LOGD_ENABLED) {
                XLog.d(TAG, "Find matched activity found for intent: " + intent + " and target package: " + packageName + ", target className: " + className);
            }
        } else {
            if (!AppUtils.isPackageExists(context, packageName)) {
                Map<String, String> packageNameMappings = IconCache.getInstance(context).getPackageNameMappings();
                String targetPackageName = null;
                String targetClassName = null;
                for (Entry<String, String> entry : packageNameMappings.entrySet()) {
                    if (entry.getValue().equals(packageName)) {
                        int pos = entry.getKey().indexOf("/");
                        if (pos >= 0) {
                            targetPackageName = entry.getKey().substring(0, pos);
                            targetClassName = entry.getKey().substring(pos + 1);
                        } else {
                            targetPackageName = entry.getKey();
                            targetClassName = null;
                        }
                        if (AppUtils.isPackageExists(context, targetPackageName)) {
                            packageName = targetPackageName;
                            className = targetClassName;
                            break;
                        }
                    }
                }
            }

            if (LOGD_ENABLED) {
                XLog.d(TAG, "Find matched activity found for packageName: " + packageName + " and target package: " + packageName + ", target className: " + className);
            }
        }

        boolean convertLauncherActivity = true;

        if (DeviceUtils.isMeizuMXs() && INTENT_CONTENT_CONTACT.equals(uri) && "com.android.contacts".equals(packageName) && "com.android.contacts.activities.PeopleActivity".equals(className)) {
            String className2 = convertLauncherActivity(packageManager, action, type, "com.meizu.mzsnssyncservice", "com.meizu.mzsnssyncservice.ui.SnsTabActivity", mainIntentForQuery);
            if (!TextUtils.isEmpty(className2)) {
                packageName = "com.meizu.mzsnssyncservice";
                className = className2;
                convertLauncherActivity = false;
            }
        }

        // 校验并转换为Launcher可显示的Activity
        if (!Constant.PACKAGE_NAME.equals(packageName) && convertLauncherActivity) {
            className = convertLauncherActivity(packageManager, action, type, packageName, className, mainIntentForQuery);
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "Find matched activity real target package: " + packageName + ", target className: " + className);
        }

        return (packageName == null || className == null) ? null : new ComponentName(packageName, className);
    }

    public static Intent parseIntent(String intentDescription) throws URISyntaxException {
        Intent intent;

        if (intentDescription.indexOf("#") >= 0) {
            intent = Intent.parseUri(intentDescription, 0);
        } else {
            ComponentName componentName = ComponentName.unflattenFromString(intentDescription);
            if (componentName == null) {
                intent = Intent.parseUri(intentDescription, 0);
            } else {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(componentName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            }
        }
        return intent;
    }

    public static boolean isIntentExist(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
        PackageManager localPackageManager = context.getPackageManager();
        if (localPackageManager.resolveActivity(intent, 0) == null) {
            return false;
        }
        return true;
    }

    public static void initSystemUiForTransparentBlurActivity(Activity activity) {
        Window window = activity.getWindow();
        if (LauncherSettings.isEnableStatusBarAutoTransparent()) {
            boolean successful = false;
            if (DeviceUtils.isLollipop() && DeviceUtils.isStandardRom()) {
                try {
                    Method method = ReflectionUtils.getMethod(
                            window.getClass(), "setStatusBarColor", new Class[] {int.class});
                    if (method != null) {
                        // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //ViewUtils.setSystemUiVisibility(window.getDecorView(), 0x00000400);
                        method.invoke(window, 0x00000000);
                        successful = true;
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }

            if (!successful) {
                window.addFlags(WindowManagerUtils.FLAG_TRANSLUCENT_STATUS);
            }
        }

        if (LauncherSettings.isEnableNavigationBarAutoTransparent()) {
            RuntimeConfig.sGlobalBottomPadding = ResourceUtils.getNavigationBarHeight(activity);
            if (RuntimeConfig.sGlobalBottomPadding > 0) {
                boolean successful = false;
                if (DeviceUtils.isLollipop() && DeviceUtils.isStandardRom()) {
                    try {
                        Method method = ReflectionUtils.getMethod(
                                window.getClass(), "setNavigationBarColor",
                                new Class[] {int.class});
                        if (method != null) {
                            window.addFlags(
                                    WindowManagerUtils.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            // SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            ViewUtils.setSystemUiVisibility(window.getDecorView(), 0x00000200);
                            method.invoke(window, 0xFF000000);
                            successful = true;
                        }
                    } catch (Throwable e) {
                        // ignore
                    }
                }

                if (!successful) {
                    window.addFlags(WindowManagerUtils.FLAG_TRANSLUCENT_NAVIGATION);
                }
            }
        }

        if (LauncherSettings.isEnableStatusBarAutoTransparentV2()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.dimAmount = 0.5f;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

    }
}
