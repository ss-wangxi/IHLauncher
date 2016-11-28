package cc.snser.launcher.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import cc.snser.launcher.Constant;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.IconFsCache;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class ItemInfoLoader {

    private static final boolean DEBUG_LOADERS = LOGD_ENABLED;

    protected static final String TAG = "Launcher.ShortcutInfoFactory";

    private static ItemInfoLoader mInstance = null;

    private static Object mSync = new Object();

    private final IconCache mIconCache;

    public static ItemInfoLoader getInstance(Context context) {
        synchronized (mSync) {
            if (mInstance == null) {
                mInstance = new ItemInfoLoader(context);
            }
        }
        return mInstance;
    }

    private ItemInfoLoader(Context context) {
        mIconCache = IconCache.getInstance(context);
    }

    /**
     * This is called from the code that adds shortcuts from the intent receiver.  This
     * doesn't have a Cursor, but
     */
    public HomeDesktopItemInfo getShortcutInfo(PackageManager pm, ResolveInfo ri, Context context, boolean loadInfo) {
        ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
        HomeDesktopItemInfo info = getShortcutInfo(pm, cn, ri, context, loadInfo);
        if (info != null) {
            info.setActivity(cn, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            info.lastUpdateTime = Utils.getLastUpdateTime(ri);
            info.system = (ri.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
            info.storage = Utils.getApplicationStorage(ri);
        }
        return info;
    }

    /**
     * Make an ShortcutInfo object for a shortcut that is an application.
     *
     * If c is not null, then it will be used to fill in missing data like the title and icon.
     */
    private HomeDesktopItemInfo getShortcutInfo(PackageManager manager, ComponentName componentName, ResolveInfo resolveInfo, Context context, boolean loadInfo) {
        final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
        final HomeDesktopItemInfo info = new HomeDesktopItemInfo();

        if (componentName == null) {
            return null;
        }

        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;

        // TODO: See if the PackageManager knows about this case.  If it doesn't
        // then return null & delete this.

        // the resource -- This may implicitly give us back the fallback icon,
        // but don't worry about that.  All we're doing with usingFallbackIcon is
        // to avoid saving lots of copies of that in the database, and most apps
        // have icons anyway.

        // from the resource

        initIconResourceWithoutCache(info, componentName, resolveInfo, loadInfo);

        if (DEBUG_LOADERS) {
            XLog.d(TAG, "getShortcutInfo for app in " + (SystemClock.uptimeMillis() - t) + "ms");
        }

        return info;
    }

    public void initIconResourceWithoutCache(final HomeDesktopItemInfo info, ComponentName componentName, ResolveInfo resolveInfo,
            boolean loadInfo) {
        FastBitmapDrawable iconDrawable = null;
        if (loadInfo) {
            if (resolveInfo != null) {
                info.defaultTitle = mIconCache.getTitle(componentName, resolveInfo);
            }

            // fall back to the class name of the activity
            if (info.defaultTitle == null) {
                info.defaultTitle = componentName.getClassName();
            }


            // from the resource
            if (resolveInfo != null) {
                iconDrawable = mIconCache.getIcon(componentName, resolveInfo);
            }
        } else {
            info.defaultTitle = "";
        }

        if (iconDrawable != null) {
            info.defaultIcon = iconDrawable;
            info.usingFallbackIcon = false;
        } else {
            Bitmap icon = mIconCache.getDefaultIcon();
            info.usingFallbackIcon = true;

            info.defaultIcon = new FastBitmapDrawable(icon);
        }
    }

    /**
     * Make an ShortcutInfo object for a shortcut that is an application.
     *
     * If c is not null, then it will be used to fill in missing data like the title and icon.
     */
    public HomeDesktopItemInfo getShortcutInfo(Intent intent, ResolveInfo resolveInfo, Context context,
            Cursor c, int titleIndex, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, boolean loadInfo) {
        final long t = LOGD_ENABLED ? SystemClock.uptimeMillis() : 0;
        final HomeDesktopItemInfo info = new HomeDesktopItemInfo();

        if (intent == null || intent.getComponent() == null) {
            return null;
        }

        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        info.iconType = c.getInt(iconTypeIndex);
        // TODO: See if the PackageManager knows about this case.  If it doesn't
        // then return null & delete this.

        // the resource -- This may implicitly give us back the fallback icon,
        // but don't worry about that.  All we're doing with usingFallbackIcon is
        // to avoid saving lots of copies of that in the database, and most apps
        // have icons anyway.

        if (!initTitleResourcesForItemTypeApplication(info, intent, resolveInfo, context, c, titleIndex, loadInfo)) {
            return null;
        }
        
        if (!initIconResourcesForItemTypeApplicationWithCache(info, intent, resolveInfo, context, c, iconPackageIndex, iconResourceIndex, loadInfo)) {
            return null;
        }


        if (LOGD_ENABLED) {
            XLog.d(TAG, "getShortcutInfo for app in " + (SystemClock.uptimeMillis() - t) + "ms");
        }

        return info;
    }

    /**
     * Make an ShortcutInfo object for a shortcut that isn't an application.
     */
    public HomeDesktopItemInfo getShortcutInfo(Cursor c, Intent intent, Context context,
            int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex,
            int titlePackageIndex, int titleResourceIndex, int titleIndex) {
        final long t = LOGD_ENABLED ? SystemClock.uptimeMillis() : 0;

        final HomeDesktopItemInfo info = new HomeDesktopItemInfo();

        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        info.iconType = c.getInt(iconTypeIndex);

        if (!initTitleResourcesForItemTypeShortcut(info, c, intent, context, titlePackageIndex, titleResourceIndex, titleIndex)) {
            return null;
        }
        if (!initIconResourcesForItemTypeShortcut(info, c, intent, context, iconPackageIndex, iconResourceIndex, iconIndex)) {
            return null;
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "getShortcutInfo for shortcut in " + (SystemClock.uptimeMillis() - t) + "ms");
        }

        return info;
    }

    private static boolean initTitleResourcesForItemTypeApplication(HomeDesktopItemInfo info, Intent intent, ResolveInfo resolveInfo, Context context,
            Cursor c, int titleIndex, boolean loadInfo) {
        // from the db
        info.defaultTitle = c.getString(titleIndex);

        if (!loadInfo) {
            return true;
        }
        // from the resource
        if (info.defaultTitle == null) {
            if (resolveInfo != null) {
                info.defaultTitle = IconCache.getInstance(context).getTitle(intent, resolveInfo);
            }
        }

        // fall back to the class name of the activity
        if (info.defaultTitle == null) {
            info.defaultTitle = intent.getComponent().getClassName();
        }

        return true;
    }

    
    public static boolean initIconResourcesForItemTypeApplicationWithCache(HomeDesktopItemInfo info, Intent intent, ResolveInfo resolveInfo, Context context,
    		boolean loadInfo) {
        if (!loadInfo) {
            Bitmap icon = IconCache.getInstance(context).getDefaultIcon();
            info.usingFallbackIcon = true;

            info.defaultIcon = new FastBitmapDrawable(icon);
            return true;
        }

        FastBitmapDrawable iconDrawable = null;
       
       
        // from the cache
        if (resolveInfo != null) {
        	iconDrawable = IconCache.getInstance(context).getCachedIcon(intent.getComponent());
        	if (iconDrawable == null) {
        		Bitmap icon = IconFsCache.getInstance(context).getIconFromCacheFile(context, intent.getComponent());
        		if (icon != null) {
        			iconDrawable = new FastBitmapDrawable(icon);
        			IconCache.getInstance(context).putCachedIcon(intent.getComponent(), iconDrawable);
        		}
        	}
        }

        // from the resource
        if (iconDrawable == null) {
        	if (resolveInfo != null) {
        		iconDrawable = IconCache.getInstance(context).getIcon(intent, resolveInfo);
        	}
        }

        if (iconDrawable != null) {
        	info.defaultIcon = iconDrawable;
        	info.usingFallbackIcon = false;
        } else {
        	Bitmap icon = IconCache.getInstance(context).getDefaultIcon();
        	info.usingFallbackIcon = true;

        	info.defaultIcon = new FastBitmapDrawable(icon);
        }


        return true;
    }
    
    public static boolean initIconResourcesForItemTypeApplicationWithCache(HomeDesktopItemInfo info, Intent intent, ResolveInfo resolveInfo, Context context,
    		Cursor c, int iconPackageIndex, int iconResourceIndex, boolean loadInfo) {
    	
        if (!loadInfo) {
            Bitmap icon = IconCache.getInstance(context).getDefaultIcon();
            info.usingFallbackIcon = true;

            info.defaultIcon = new FastBitmapDrawable(icon);;
            return true;
        }

        Bitmap icon = null;
        String resourceName = c.getString(iconResourceIndex);
        // the resource
        if (resourceName != null) {
            try {
                Resources resources = context.getResources();
                
                if (resources != null) {
                	String packageName = c.getString(iconPackageIndex);
                    final int id = resources.getIdentifier(resourceName, null, null);
                    Drawable iconDrawable = resources.getDrawable(id);
                    icon = WorkspaceIconUtils.createIconBitmap(iconDrawable, context, true, false);
                    info.usingFallbackIcon = false;
                    info.defaultIcon = new FastBitmapDrawable(icon);
                    ShortcutIconResource iconResource = new ShortcutIconResource();
                    iconResource.packageName = packageName;
                    iconResource.resourceName = resourceName;
                    info.iconResource = iconResource;
                    info.iconType = LauncherSettings.Favorites.ICON_TYPE_RESOURCE;
                }
            } catch (Exception e) {
            	initIconResourcesForItemTypeApplicationWithCache(info, intent,resolveInfo,context,
                		loadInfo);
            }
        }
        else
        {
        	initIconResourcesForItemTypeApplicationWithCache(info, intent,resolveInfo,context,
            		loadInfo);
        }

        return true;
    }

    private static boolean initTitleResourcesForItemTypeShortcut(HomeDesktopItemInfo info, Cursor c, Intent intent, Context context,
            int titlePackageIndex, int titleResourceIndex, int titleIndex) {
        PackageManager packageManager = context.getPackageManager();

        info.defaultTitle = null;

        // TODO: If there's an explicit component and we can't install that, delete it.
        String titleResourceName = c.getString(titleResourceIndex);
        if (!TextUtils.isEmpty(titleResourceName)) {
            try {
                String titlePackageName = c.getString(titlePackageIndex);

                Resources resources = null;

                if (TextUtils.isEmpty(titlePackageName)) {
                    resources = context.getResources();
                } else {
                    resources = packageManager.getResourcesForApplication(titlePackageName);
                }

                if (resources != null) {
                    final int id = resources.getIdentifier(titleResourceName, null, null);
                    info.defaultTitle = resources.getString(id);
                }
            } catch (Exception e) {
                // drop this. we have other places to look for icons
            }
        }

        if (info.defaultTitle == null) {
            info.defaultTitle = c.getString(titleIndex);
        }

        return true;
    }

    private static boolean initIconResourcesForItemTypeShortcut(HomeDesktopItemInfo info, Cursor c, Intent intent, Context context,
            int iconPackageIndex, int iconResourceIndex, int iconIndex) {
        PackageManager packageManager = context.getPackageManager();

        Bitmap icon = null;
        String packageName = c.getString(iconPackageIndex);
        switch (info.iconType) {
        case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
            String resourceName = c.getString(iconResourceIndex);

            ShortcutIconResource iconResource = new ShortcutIconResource();
            iconResource.packageName = packageName;
            iconResource.resourceName = resourceName;
            info.iconResource = iconResource;

            boolean addBoard = false;

            if (Constant.PACKAGE_NAME.equals(packageName)) {
                try {
                    int pos = resourceName.lastIndexOf("/");
                    if (pos >= 0) {
                        resourceName = resourceName.substring(pos + 1);
                    }
                    Drawable drawable = null;
                    if (drawable == null) {
                        drawable = Utilities.getDrawableDefault(context, resourceName, true);
                    }

                    if (drawable != null) {
                    	icon = WorkspaceIconUtils.createIconBitmap(drawable, context, addBoard, false);
                    }
                } catch (Exception e) {
                    // drop this.  we have other places to look for icons
                }
            }

            // the resource
            if (icon == null) {
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    if (resources != null) {
                        final int id = resources.getIdentifier(resourceName, null, null);
                        icon = WorkspaceIconUtils.createIconBitmap(resources.getDrawable(id), context, !Constant.PACKAGE_NAME.equals(packageName)
                                , !Constant.PACKAGE_NAME.equals(packageName));
                    }
                } catch (Exception e) {
                    // drop this.  we have other places to look for icons
                }
            }
            // the db
            if (icon == null) {
                icon = getIconFromCursor(context, c, iconIndex);
            }
            // the fallback icon
            if (icon == null) {
                icon = IconCache.getInstance(context).getDefaultIcon();
                info.usingFallbackIcon = true;
            }
            break;
        case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
            icon = getIconFromCursor(context, c, iconIndex);
            if (icon == null) {
                icon = IconCache.getInstance(context).getDefaultIcon();
                info.usingFallbackIcon = true;
            } else {
                icon = WorkspaceIconUtils.createIconBitmap(new FastBitmapDrawable(icon), context, true, true);
            }
            break;
        default:
            icon = IconCache.getInstance(context).getDefaultIcon();
            info.usingFallbackIcon = true;
            break;
        }
        info.defaultIcon = new FastBitmapDrawable(icon);

        return true;
    }

    private static Bitmap getIconFromCursor(Context context, Cursor c, int iconIndex) {
        if (c.isNull(iconIndex)) {
            return null;
        }
        byte[] data = c.getBlob(iconIndex);
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Throwable e) {
            return null;
        }
    }

}
