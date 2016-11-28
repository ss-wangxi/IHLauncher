package cc.snser.launcher.features.shortcut;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.text.TextUtils;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;

import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.DeviceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemShortcutInfo extends HomeDesktopItemInfo {

    public SystemShortcutInfo(Context context, PackageManager packageManager, ResolveInfo resolveInfo) {
        this.defaultTitle = resolveInfo.loadLabel(packageManager).toString();
        Bitmap bitmap = BitmapUtils.drawableToBitmap(resolveInfo.loadIcon(packageManager));
        if (BitmapUtils.isValidBitmap(bitmap)) {
            this.defaultIcon = new FastBitmapDrawable(bitmap);
        }

        this.intent = new Intent();
        this.intent.setComponent(new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name));
    }

    public static List<SystemShortcutInfo> getAll(Context context) {
        Intent shortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<SystemShortcutInfo> ret = new ArrayList<SystemShortcutInfo>();

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(shortcutIntent, PackageManager.GET_ACTIVITIES);

        if (!resolveInfos.isEmpty()) {
            Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(packageManager));
            for (ResolveInfo resolveInfo : resolveInfos) {
                if(filterSpecialLogical(resolveInfo)){
                    ret.add(new SystemShortcutInfo(context, packageManager, resolveInfo));
                }
            }
        }

        return ret;
    }

    private static ArrayList<ShortcutIdentify> mMiFilter;
    private static void initFilter(){
        if(mMiFilter == null){
            mMiFilter = new ArrayList<ShortcutIdentify>();
            mMiFilter.add(new ShortcutIdentify("com.android.settings", "com.android.settings.CreateShortcut"));
            mMiFilter.add(new ShortcutIdentify("com.android.email", "com.kingsoft.email2.ui.CreateShortcutActivityEmail"));
        }
    }

    /**
     * 针对特定的Rom,显示或隐藏某些快捷方式扩口
     * @param info
     * @return true表示可以加入
     */
    private static boolean filterSpecialLogical(ResolveInfo info){
        if(info == null) return false;
        if(DeviceUtils.isXiaomi() || DeviceUtils.isHongMi()){
            initFilter();
            if(info.activityInfo != null && info.activityInfo.applicationInfo != null){
                if(!TextUtils.isEmpty(info.activityInfo.name) && !TextUtils.isEmpty(info.activityInfo.applicationInfo.packageName)){
                    String activityName = info.activityInfo.name;
                    String packageName = info.activityInfo.applicationInfo.packageName;
                    if(mMiFilter != null){
                        for (ShortcutIdentify identify : mMiFilter){
                            if(identify.isEqual(packageName,activityName)){
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
        return true;
    }

    public static class ShortcutIdentify{
        public String mClassName;
        public String mComponentName;

        public ShortcutIdentify(String componentName,String className){
            mClassName = className;
            mComponentName = componentName;
        }

        public boolean isEqual(String componentName,String className){
            if(componentName == null || className == null ) return false;
            if(componentName.compareToIgnoreCase(mComponentName) == 0 || className.compareToIgnoreCase(mClassName) == 0){
                return true;
            }
            return false;
        }
    }
}
