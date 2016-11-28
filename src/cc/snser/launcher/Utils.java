package cc.snser.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.ChannelUtils;
import com.shouxinzm.launcher.util.DeviceUtils;
import com.shouxinzm.launcher.util.DialogUtils;
import com.shouxinzm.launcher.util.FileUtils;
import com.shouxinzm.launcher.util.IOUtils;
import com.shouxinzm.launcher.util.SdCardUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;
import static cc.snser.launcher.Constant.LOGW_ENABLED;

public class Utils {

    public static final String TAG = "Launcher.Global";

    public static final DecimalFormat NUMBER_FORMATER = new DecimalFormat("#0.#");

    public static final DecimalFormat INTEGER_NUMBER_FORMATER = new DecimalFormat("#");

    public static final Collator COLLATOR = Collator.getInstance();

    private static Method sSetLayerTypeMethod = null;
    
    private static final String MANUFACTURER = Build.MANUFACTURER.toLowerCase();
    private static final String MODEL = Build.MODEL.toLowerCase();

    public static boolean shouldUseMarketDownload() {
        return false;
    }

    public static void invokeSetLayerTypeMethod(View view) {
        if (DeviceUtils.isIceCreamSandwich()) {
            try {
                if (sSetLayerTypeMethod == null) {
                    sSetLayerTypeMethod = View.class.getMethod("setLayerType", new Class[] {Integer.TYPE, Paint.class});
                }
                sSetLayerTypeMethod.invoke(view, new Object[] {Integer.valueOf(1), null});
            } catch (Throwable e) {
                if (LOGE_ENABLED) {
                    XLog.e(TAG, "Failed to invoke the set layer type method.", e);
                }
            }
        }
    }

    public static Message createMessage(Handler handler, int what, Bundle data,
            Object token) {
        Message message = Message.obtain(handler, what);
        if (token != null) {
            message.obj = token;
        }
        if (data != null) {
            message.setData(data);
        }
        return message;
    }

    public static boolean isContextFinished(Context context) {
        return context instanceof Activity
                && ((Activity) context).isFinishing();
    }

    public static void handleException(Throwable e) {
        XLog.e("Launcher.Error", "Unexpected error!", e);
    }

    public static String getLocale(Context context) {
        return context.getResources().getConfiguration().locale.toString();
    }

    public static boolean isLocaleChinese(Context context) {
        return getLocale(context).startsWith("zh");
    }

    public static void restartLauncher(Context context) {
        restartLauncher(context.getApplicationContext(), false);
    }

    public static void restartLauncher(Context context, boolean allProcess) {
        startLauncher(context.getApplicationContext(), allProcess ? Constant.FLAG_RESTART_ALL_PROCESS : Constant.FLAG_RESTART_LAUNCHER);
    }

    public static void startLauncher(Context context, Integer flag) {
        Intent intent = new Intent(context, Launcher.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (flag != null) {
            intent.putExtra("flag", flag);
        }
        context.startActivity(intent);
    }

    public static long getLastUpdateTime(ResolveInfo info) {
        String path = info.activityInfo.applicationInfo.sourceDir;
        File file = new File(path);
        return file.lastModified();
    }

    public static int getApplicationStorage(ResolveInfo info) {
        return getApplicationStorage(info.activityInfo);
    }

    public static int getApplicationStorage(ActivityInfo info) {
        try {
            if ((info.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
                return Constant.APPLICATION_EXTERNAL;
            } else if (info.applicationInfo.sourceDir.startsWith("/mnt/asec/")) {
                return Constant.APPLICATION_EXTERNAL;
            } else {
                return Constant.APPLICATION_INTERNAL;
            }
        } catch (Exception e) {
            return Constant.APPLICATION_INTERNAL;
        }
    }

    public static void showSdCardNotAvailableDialog(Context context) {
        DialogUtils.showDialog(context, context.getString(R.string.global_sdcardmissing_dialog_title), context.getString(R.string.global_sdcardmissing_dialog_message),
                context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
    }

    // TODO: 重构：考虑搬到 SDCardUtils.java, PathUtils.java

    private static final Set<String> READABLE_STATES = new HashSet<String>();

    private static final Set<String> WRITABLE_STATES = new HashSet<String>();

    public static boolean isExternalStorageReadable() {
        String state = SdCardUtils.getExternalStorageState();
        return READABLE_STATES.contains(state);
    }

    static {
        Utils.READABLE_STATES.add(Environment.MEDIA_MOUNTED);
        Utils.READABLE_STATES.add(Environment.MEDIA_MOUNTED_READ_ONLY);
        Utils.WRITABLE_STATES.add(Environment.MEDIA_MOUNTED);
    }

    public static boolean isExternalStorageWritable() {
        String state = SdCardUtils.getExternalStorageState();
        return Utils.WRITABLE_STATES.contains(state);
    }

    public static ResolveInfo queryDefaultLauncher(Context context) {
        PackageManager packageManager = context.getPackageManager();

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> infos = packageManager.queryIntentActivities(homeIntent, 0);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        List<ComponentName> activities = new ArrayList<ComponentName>();
        for (ResolveInfo info : infos) {
            filters.clear();
            activities.clear();

            String packageName = info.activityInfo.packageName;
            packageManager.getPreferredActivities(filters, activities, packageName);

            for (IntentFilter filter : filters) {
                if (filter.hasAction(Intent.ACTION_MAIN) && filter.hasCategory(Intent.CATEGORY_HOME)) {
                    return info;
                }
            }
        }

        return null;
    }

    public static ResolveInfo queryDefaultBrowser(Context context) {
        PackageManager packageManager = context.getPackageManager();
        
        Intent browserIntent = null;
        
        browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.addCategory(Intent.CATEGORY_DEFAULT);
        browserIntent.addCategory(Intent.CATEGORY_APP_BROWSER);
        browserIntent.setData(Uri.parse("http://www.google.com/m"));
        List<ResolveInfo> infos = Utils.queryBestMatchesByIntent(context, browserIntent);
        if(infos == null)
        {
        	browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.addCategory(Intent.CATEGORY_DEFAULT);
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            browserIntent.setData(Uri.parse("http://www.google.com/m"));
        }
        
        infos = Utils.queryBestMatchesByIntent(context, browserIntent);

        if (infos != null) {
            List<IntentFilter> filters = new ArrayList<IntentFilter>();
            List<ComponentName> activities = new ArrayList<ComponentName>();
            for (ResolveInfo info : infos) {
                filters.clear();
                activities.clear();

                String packageName = info.activityInfo.packageName;
                packageManager.getPreferredActivities(filters, activities, packageName);

                for (IntentFilter filter : filters) {
                    if (filter.hasAction(Intent.ACTION_VIEW)
                            && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                        return info;
                    }
                }
            }
        }

        return null;
    }

    public static List<ResolveInfo> queryBestMatchesByIntent(Context context, Intent intent) {
        List<ResolveInfo> queryInfos = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        List<ResolveInfo> ret = null;
        if (queryInfos != null) {
            Map<String, ResolveInfo> map = new HashMap<String, ResolveInfo>(queryInfos.size());
            for (ResolveInfo info : queryInfos) {
                if (Intent.ACTION_DIAL.equals(intent.getAction()) && DeviceUtils.isCoolpad7298A()) {
                    if ("com.android.contacts".equals(info.activityInfo.packageName) && "com.android.contacts.activities.NonPhoneActivity".equals(info.activityInfo.name)) {
                        continue;
                    }
                }

                String packageName = info.activityInfo.packageName;
                ResolveInfo exist = map.get(packageName);
                if (exist == null) {
                    map.put(packageName, info);
                } else {
                    if (exist.priority < info.priority) {
                        map.put(packageName, info);
                    }
                }
            }
            ret = new ArrayList<ResolveInfo>(map.size());
            for (ResolveInfo info : map.values()) {
                ret.add(info);
            }
        }
        return ret;
    }

    public static List<String> getAllInstalledLaunchers(Context context) {
        PackageManager packageManager = context.getPackageManager();

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> infos = packageManager.queryIntentActivities(
                homeIntent, 0);

        List<String> packageNames = new ArrayList<String>();
        for (ResolveInfo info : infos) {
            packageNames.add(info.activityInfo.packageName);
            if (LOGD_ENABLED) {
                XLog.d(TAG, "InstalledLauncher " + info.activityInfo.packageName);
            }
        }
        return packageNames;
    }

    public static int[] getLayout(String layout) {
        int pos = layout.indexOf("x");
        if (pos < 0) {
            return null;
        }
        return new int[] {Integer.parseInt(layout.substring(0, pos)),
                Integer.parseInt(layout.substring(pos + 1)) };
    }

    /**
     * Read asset to string.
     *
     * @param context
     *            Application context
     * @param packageName
     *            Package name to looking for
     * @param filename
     *            Filename in asset directory
     * @return String content of file, or <code>null</code> if not exists
     */
    public static String readAssetToString(Context context, String packageName,
            String filename) {
        InputStream inputStream = null;
        try {
            inputStream = getAssetManager(context, packageName).open(filename);
            return IOUtils.toString(inputStream);
        } catch (NameNotFoundException e) {
            if (LOGW_ENABLED) {
                XLog.printStackTrace(e);
                //XLog.w(TAG, "Name not found [" + filename + "] on open asset.", e);
            }
            return null;
        } catch (IOException e) {
            if (LOGW_ENABLED) {
                XLog.printStackTrace(e);
                //XLog.w(TAG, "Read asset [" + filename + "] to string failed.", e);
            }
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Get asset manager.
     *
     * @param context
     *            Application context
     * @param packageName
     *            Package name to looking for
     * @return Asset manager of current package
     * @throws NameNotFoundException
     *             If not package found.
     */
    public static AssetManager getAssetManager(Context context,
            String packageName) throws NameNotFoundException {
        if (packageName == null) {
            return context.getAssets();
        }
        Context packageContext = context.createPackageContext(packageName, 0);
        return packageContext.getAssets();
    }

    /**
     * 格式化一个长整型数字到以G、M、K、B结尾的字符串，数字保留位数由DecimalFormat format参数决定.
     */
    public static String toHumanReadableSize(long size, DecimalFormat format) {
        if (format == null) {
            return size + "B";
        }
        if (size > FileUtils.ONE_GB) {
            return format.format((float) size / FileUtils.ONE_GB) + "G";
        } else if (size > FileUtils.ONE_MB) {
            return format.format((float) size / FileUtils.ONE_MB) + "M";
        } else if (size > FileUtils.ONE_KB) {
            return format.format((float) size / FileUtils.ONE_KB) + "K";
        } else {
            return size + "B";
        }
    }

    public static boolean isOnTopOfActivityStack(Context context, Class<?> clazz) {
        ComponentName componentName = getTopActivityClassName(context);
        return componentName.getClassName().equals(clazz.getName());
    }

    public static ComponentName getTopActivityClassName(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.getRunningTasks(1).get(0).topActivity;
    }

    public static String getLastPartOfPackage(String packageName) {
        String[] nameParts = packageName.split("\\.");
        return nameParts[nameParts.length - 1];
    }

    public static boolean shouldIgnoreApp(String packageName) {
        if (Constant.PACKAGE_NAME.equals(packageName)) {
            return true;
        }
        return false;
    }

    public static boolean isLDPI() {
        return false;
    }

    public static boolean isMDPI() {
        return true;
    }

    public static boolean isLargeMDPI(Context context) {
        return false;
    }

    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj2 == null;
        }
        return obj1.equals(obj2);
    }

    // TODO: 重构：考虑搬到 ui/effect

    @SuppressWarnings("rawtypes")
    public static Object getField(Object object, Class clz, String fieldName) {
        Field field = null;
        Boolean accessible = null;

        try {
            field = clz.getDeclaredField(fieldName);
            accessible = field.isAccessible();
            field.setAccessible(true);

            return field.get(object);
        } catch (Throwable e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Failed to get the field: " + fieldName, e);
            }
        } finally {
            if (field != null && accessible != null) {
                field.setAccessible(accessible);
            }
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    public static boolean setField(Object object, Class clz, String fieldName, Object value) {
        Field field = null;
        Boolean accessible = null;

        try {
            field = clz.getDeclaredField(fieldName);
            accessible = field.isAccessible();
            field.setAccessible(true);

            field.set(object, value);
            return true;
        } catch (Throwable e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Failed to set the field: " + fieldName, e);
            }
            return false;
        } finally {
            if (field != null && accessible != null) {
                field.setAccessible(accessible);
            }
        }
    }

    public static Drawable getDrawableFromResources(Context context, int resId) {
        return getDrawableFromResources(context, context.getResources(), resId, true);
    }

    public static Drawable getDrawableFromResources(Context context, Resources resources, int resId, boolean autofit) {
        return getDrawableFromResourcesSafely(context, resources, resId, autofit, 0);
    }

    private static Drawable getDrawableFromResourcesSafely(Context context, Resources resources, int resId, boolean autofit, int count) {
        float oldDensity = resources.getDisplayMetrics().density;

        try {
            Drawable drawable = resources.getDrawable(resId);

            if (!autofit) {
                return drawable;
            }

            if (drawable instanceof NinePatchDrawable || !Utils.isLargeMDPI(context)) {
                return drawable;
            }

            BitmapFactory.Options options;
            if (count > 0) {
                options = BitmapUtils.getLowQualityOptions(null);
            } else {
                options = new BitmapFactory.Options();
            }
            options.inTargetDensity = DisplayMetrics.DENSITY_HIGH;

            resources.getDisplayMetrics().density = 1.5f;
            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());

            Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, options);
            if (bitmap != null) {
                BitmapDrawable ret = new BitmapDrawable(resources, bitmap);
                ret.setTargetDensity(DisplayMetrics.DENSITY_HIGH);
                return ret;
            }
        } catch (NotFoundException e) {
            return null;
        } catch (Throwable e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "Failed to decode the bitmap.", e);
            }
            if (count < 2) {
               return getDrawableFromResourcesSafely(context, resources, resId, autofit, ++count);
            }
        } finally {
            resources.getDisplayMetrics().density = oldDensity;
            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
        }
        return null;
    }

    public static void clearAnimation(View view) {
        try {
            if (view.getAnimation() != null) {
                view.getAnimation().reset();
            }
        } catch (Throwable e) {
            // ignore
        }
        view.clearAnimation();
    }

    public static void cancelAnimation(Animation animation) {
        if (animation != null) {
            try {
                animation.cancel();
            } catch (NoSuchMethodError e) {
                try {
                    AnimationListener listener = (AnimationListener) getField(animation, Animation.class, "mListener");
                    if (listener != null) {
                        listener.onAnimationEnd(animation);
                    }
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
    }

    public static void killLauncherAllProcess(final Context context) {
        new Thread() {

            @Override
            public void run() {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

                if (LOGD_ENABLED) {
                    XLog.d(TAG, "KillScreenLockProcess Before: these process are runninng: " + getRunningProcess(am));
                }
                List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                for (RunningAppProcessInfo process : processes) {
                    for (String pkg : process.pkgList) {
                        if (isTargetPackage(pkg) && process.pkgList.length == 1) {
                            if (process.pid != android.os.Process.myPid()) {
                                android.os.Process.killProcess(process.pid);
                            }
                        }
                    }
                }
                android.os.Process.killProcess(android.os.Process.myPid());
            }

            private String getRunningProcess(ActivityManager am) {
                List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                StringBuilder sb = new StringBuilder();
                for (RunningAppProcessInfo process : processes) {
                    for (String pkg : process.pkgList) {
                        if (isTargetPackage(pkg)) {
                            if (sb.length() > 0) {
                                sb.append(" ,");
                            }
                            sb.append(pkg);
                            break;
                        }
                    }
                }
                return sb.toString();
            }

            private boolean isTargetPackage(String pkg) {
                return Constant.PACKAGE_NAME.equals(pkg);
            }

        }.start();

    }

    public static float scaleFrom(float value, float center, float scale) {
        return 0.5F + (value - center) / scale + center;
    }

    public static float scaleTo(float value, float center, float scale) {
        return 0.5F + (value - center) * scale + center;
    }


    public static String getLcFromAssets(Context packageContext) {
        return ChannelUtils.getLcFromAssets(packageContext);

    }

    public static void setAdapter(AbsListView view, ListAdapter adapter) {
        try {
            ((AdapterView<ListAdapter>) view).setAdapter(adapter);
        } catch (Throwable e) {
            view.setAdapter(adapter);
        }
    }

    public static String int2hex(int num) {
        String res = "";
        for (int i = 0; i < 8; i++) {
            res = number2Hex((byte) (num & 0x0f)) + res;
            num >>>= 4;
        }
        return res;
    }

    static final char[] HEX_ARR = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Wandelt eine Zahl in eine Hexadezimalziffer um.
     *
     * @param b Zahl zwischen 0 und 15
     * @return entsprechende Hexadezimal-Ziffer
     */
    public static char number2Hex(byte b) {
        char res = '?';
        if (b >= 0 && b <= 15) {
            res = HEX_ARR[b];
        }
        return res;
    }

    /**
     * 改进的32位FNV算法1
     *
     * @param data
     *            字符串
     * @return int值
     */
    public static int FNVHash1(String data) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < data.length(); i++) {
            hash = (hash ^ data.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return hash;
    }

    public static int bytesToInt(byte[] b) {
        //0xff 表示的是4个字节的整形，所以ok
        int cc = (b[0] & 0xff) | ((b[1] & 0xff) << 8) | (b[2] & 0xff << 16) | ((b[3] & 0xff) << 24);
        return cc;
    }

    public static String encode(String value) {
        try {
            return value == null ? null : URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    public static void showInstalledAppDetails(Context context, String packageName) {
        Intent intent = new Intent();
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public static final String EXTENSION_JPG = ".jpg";
    public static final String EXTENSION_JPEG = ".jpeg";
    public static final String EXTENSION_PNG = ".png";

    /**
     * Read asset to bitmap.
     * <p>
     * The parameter resPrefix should be only include path and base name of asset file, this method
     * will find matched image file automatically. The order of matching is {@link EXTENSION_JPG},
     * {@link EXTENSION_PNG} and {@link EXTENSION_JPEG}</p>
     *
     * @param context Application context
     * @param packageName Package name to looking for
     * @param resPrefix Resource name in asset directory, should only include path and base name of file
     * @param width Width of output image, should always less than a half of original image width
     * @param height Height of output image, should always less than a half of original image height
     * @return Built bitmap, or <code>null</code> if not exists
     */
    public static Bitmap readAssetResToBitmap(Context context, String packageName, String resPrefix, int width, int height) {
        Bitmap bitmap = readAssetToBitmap(context, packageName, resPrefix + EXTENSION_JPG, width, height);
        if (bitmap != null) {
            return bitmap;
        }
        bitmap = readAssetToBitmap(context, packageName, resPrefix + EXTENSION_PNG, width, height);
        if (bitmap != null) {
            return bitmap;
        }
        bitmap = readAssetToBitmap(context, packageName, resPrefix + EXTENSION_JPEG, width, height);
        return bitmap;
    }

    /**
     * Read asset to bitmap.
     * <p>
     * Will return the original image when either width or height is negative.</p>
     *
     * @param context Application context
     * @param packageName Package name to looking for
     * @param filename Filename in asset directory
     * @param width Width of output image, should always less than a half of original image width
     * @param height Height of output image, should always less than a half of original image height
     * @return Built bitmap, or <code>null</code> if not exists
     */
    public static Bitmap readAssetToBitmap(Context context, String packageName, String filename, int width, int height) {
        InputStream inputStream = null;
        try {
            inputStream = Utils.getAssetManager(context, packageName).open(filename);
            if (width < 0 || height < 0) {
                return BitmapUtils.decodeStream(inputStream, true);
            } else {
                return BitmapUtils.decodeInputStreamToBitmap(inputStream, width, height, true);
            }
        } catch (NameNotFoundException e) {
            if (LOGW_ENABLED) {
                XLog.printStackTrace(e);
                //XLog.w(TAG, "Name not found [" + filename + "] on open asset.", e);
            }
            return null;
        } catch (IOException e) {
            if (LOGW_ENABLED) {
                XLog.printStackTrace(e);
                //XLog.w(TAG, "Read asset [" + filename + "] to bitmap failed.", e);
            }
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * view should call {@link View.destroyDrawingCache()} while get drawing
     * cache
     *
     * @param view
     * @return
     */
    public static boolean shouldRefreshCache(View view) {
        return false;
    }

    private static final String FILTER_FONT_NAME_PREFIX = "手心桌面字体-";

    public static String filterFontName(String name) {
        if (name == null) {
            return null;
        }
        while (name.startsWith(FILTER_FONT_NAME_PREFIX)) {
            name = name.substring(FILTER_FONT_NAME_PREFIX.length());
        }
        return name;
    }

    private static final String FILTER_THEME_NAME_PREFIX = "手机桌面主题-";

    public static String filterThemeName(String name) {
        if (name == null) {
            return null;
        }
        while (name.startsWith(FILTER_THEME_NAME_PREFIX)) {
            name = name.substring(FILTER_THEME_NAME_PREFIX.length());
        }
        return name;
    }
    
    
    /***
     * 该手机屏幕分辨率、dpi低，但无良厂商把屏幕物理尺寸做的很大
     * 有些通用的逻辑面对这样的手机也只能是无可奈何了
     * @return
     */
    public static boolean isSumsungGtN8000(){
    	return (MANUFACTURER.equals("samsung")) && (MODEL.equals("gt-n8000"));
    }
}