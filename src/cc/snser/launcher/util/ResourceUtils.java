
package cc.snser.launcher.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.ViewGroup;
import cc.snser.launcher.App;
import cc.snser.launcher.Utils;
import cc.snser.launcher.style.SettingPreferences;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.DeviceUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

import java.lang.reflect.Field;

public class ResourceUtils {
    private static final String TAG = "Launcher.ResourceUtils";

    private static int sStatusBarHeight = -1;
    private static int sReflectStatusBarHeight = -1;

    private static int sMinNotificationTextWidth = -1;
    private static int sMinNotificationTextHeight = -1;

    private static int sStatusBarCurrentHeight;
    
    private ResourceUtils() {
    }

    public static int getMinNotificationTextWidth(Context context) {
        if (sMinNotificationTextWidth < 0) {
            sMinNotificationTextWidth = Utilities.dip2px(context, 19f);
        }
        return sMinNotificationTextWidth;
    }

    public static int getMinNotificationTextHeight(Context context) {
        if (sMinNotificationTextHeight < 0) {
            sMinNotificationTextHeight = Utilities.dip2px(context, 19f);
        }
        return sMinNotificationTextHeight;
    }

    public static void resetStatusBarHeight(Context context) {
        sStatusBarHeight = -1;
        getStatusBarHeight(context);
    }

    public static int getStatusBarHeight(Context context) {
        if (sStatusBarHeight >= 0) {
            return sStatusBarHeight;
        }
        int statusBarHeight = 0;
        if (context instanceof Activity) {
            if (ScreenDimensUtils.isFullScreen((Activity) context)) {
                statusBarHeight = 0;
            } else {
                try {
                    statusBarHeight = ((ViewGroup) ((Activity) context).getWindow().getDecorView())
                            .getChildAt(0).getPaddingTop();
                } catch (Throwable e) {
                    // ignore
                }
                if (statusBarHeight <= 0) {
                    statusBarHeight = context.getResources().getDimensionPixelSize(
                            R.dimen.status_bar_height);
                } else {
                    sStatusBarHeight = statusBarHeight;
                }
            }
        }
        return statusBarHeight;
    }
    
    /**
     * 取Statusbar高度，当全屏时，用反射取高度，
     * 当反射或正常方式都取不到时，使用配置中的高度
     * @param context Activity
     * @return
     */
    public static int getStatusBarHeightEx(Context context) {
    	if (sStatusBarHeight >= 0) {
            return sStatusBarHeight;
        }
        int statusBarHeight = 0;
        if (context instanceof Activity) {
        	if (ScreenDimensUtils.isFullScreen((Activity) context)) {
        		statusBarHeight = getStatusBarByReflect(context);
        	}else{
        		try {
                statusBarHeight = ((ViewGroup) ((Activity) context).getWindow().getDecorView())
                        .getChildAt(0).getPaddingTop();
        		} catch (Throwable e) {
        			// ignore
        		}
        	}
            if (statusBarHeight <= 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(
                        R.dimen.status_bar_height);
            } else {
                sStatusBarHeight = statusBarHeight;
            }
        }
        return statusBarHeight;
    }
    
    private static int getStatusBarByReflect(Context context){
    	if(sReflectStatusBarHeight >= 0){
    		return sReflectStatusBarHeight;
    	}
    	
    	Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            //e1.printStackTrace();
        } 
        sReflectStatusBarHeight = statusBarHeight;
        return statusBarHeight;
    }

    public static void updateStatesBarCurrentHeight(Context context, boolean isFullScreen) {
        sStatusBarCurrentHeight = isFullScreen ? 0 : getStatusBarHeight(context);
    }

    public static int getStatusBarCurrentHeight(Context context) {
        return sStatusBarCurrentHeight;
    }

    /**
     * 按照dp取状态栏高度
     * @param context
     * @return
     */
    public static int getStatusBarHeightInResource(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.status_bar_height);
    }

    public static int getNavigationBarHeight(Context context) {
        if (!DeviceUtils.isStandardRom()) {
            return 0;
        }
        
        //横屏状态下不考虑NavigationBar add by snsermail@gmail.com
        if (App.getApp().isScreenLandscape()) {
            return 0;
        }
        
        try {
            int resourceId = context.getResources().getIdentifier("config_showNavigationBar", "bool", "android");

            if (!context.getResources().getBoolean(resourceId)) {
                XLog.e(TAG, "There is no navigation bar.");
                return 0;
            }

            // huawei 隐藏导航栏的属性
            if (DeviceUtils.isHuaWei()) {
                int navMin = Settings.System.getInt(context.getContentResolver(), "navigationbar_is_min", 0);
                if (navMin == 1) {
                	XLog.e(TAG, "The navigation bar is min.");
                    return 0;
                }
            }
            
        } catch (Throwable e) {
            return 0;
        }

        try {
            int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return context.getResources().getDimensionPixelSize(resourceId);
        } catch (Throwable e) {
            // ignore
        }

        return 0;
    }

    public static int getDockbarHeight(Context context) {
    	if(Utils.isSumsungGtN8000()){
    		return 110;
    	}
    	
    	int ret = context.getResources().getDimensionPixelOffset(R.dimen.dockbar_height);

        int c = SettingPreferences.getHomeLayout(context)[0];
        if (c > 4) {
            ret -= (ret - WorkspaceIconUtils.getIconHeightWidthPadding(context)) * (c - 4) / (c - 3);
        }

        return ret;
    }
    
    public static int getVirtualStatusBarHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.virtual_statusbar_height);
    }

    public static String getString(Context context, String resName) {
        return context.getResources().getString(getResourceId(context, "string", resName));
    }

    public static int getResourceId(Context context, String defType, String resName) {
        return context.getResources().getIdentifier(resName, defType, context.getPackageName());
    }

    public static Drawable loadDrawable(Context context, PackageManager packageManager, ActivityInfo activityInfo, boolean system) {
        Drawable drawable = null;
        try {
            Resources resources = packageManager.getResourcesForApplication(activityInfo.applicationInfo);

            if (system) {
                try {
                    drawable = activityInfo.loadIcon(packageManager);
                } catch (Exception e) {
                    // ignore
                }
            }

            if (drawable == null && activityInfo.icon != 0) {
                if (drawable == null) {
                    try {
                        drawable = Utils.getDrawableFromResources(context, resources, activityInfo.icon, true);
                    } catch (NotFoundException e) {
                        // ignore
                    }
                }

                if (drawable == null) {
                    int oldOrientation = resources.getConfiguration().orientation;
                    try {
                        if (resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            resources.getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
                            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
                            drawable = Utils.getDrawableFromResources(context, resources, activityInfo.icon, true);
                        }
                    } finally {
                        if (resources.getConfiguration().orientation != oldOrientation) {
                            resources.getConfiguration().orientation = oldOrientation;
                            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
                        }
                    }
                }
            }

            if (drawable == null && activityInfo.applicationInfo.icon != 0) {
                try {
                    drawable = Utils.getDrawableFromResources(context, resources, activityInfo.applicationInfo.icon, true);
                } catch (NotFoundException e) {
                    // ignore
                }

                if (drawable == null) {
                    int oldOrientation = resources.getConfiguration().orientation;
                    try {
                        if (resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            resources.getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
                            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
                            drawable = Utils.getDrawableFromResources(context, resources, activityInfo.applicationInfo.icon, true);
                        }
                    } finally {
                        if (resources.getConfiguration().orientation != oldOrientation) {
                            resources.getConfiguration().orientation = oldOrientation;
                            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
                        }
                    }
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to load the drawable icon", e);
        }

        if (drawable == null) {
            try {
                drawable = activityInfo.loadIcon(packageManager);
            } catch (Exception e) {
                // ignore
            }
        }

        return drawable;
    }

    public static Pair<Float, Float> calculateWorkspaceFixedShortAxisPaddingAndCellWidth(Context context) {
        int shortAxisCells = SettingPreferences.getHomeLayout(context)[1];
        int width = ScreenDimensUtils.getScreenShortAxisWidth(context);

        float radio = 1.69f;

        float cellWidth = (radio * width + (2 - radio) * WorkspaceIconUtils.getIconSizeWithPadding(context)) / (2 + radio * shortAxisCells - radio);
        float padding = (width - shortAxisCells * cellWidth) / 2;

        return new Pair<Float, Float>(padding, cellWidth);
    }
    
    public static int getScreenHeight(Context context){
    	//获取有效高度（舍弃虚拟键高度）
    	DisplayMetrics metrics = Utilities.getDisplayMetrics();	
    	return metrics.heightPixels;
    }
    
    //动态计算workspaceCellLayout垂直padding
    public static int[] calculateVerticalPadding(Context context) {
    	int[] padding = new int[2];
        padding[0] = 0;//ResourceUtils.getVirtualStatusBarHeight(context);// + context.getResources().getDimensionPixelSize(R.dimen.workspace_extra_padding_top);
        padding[1] = 0;//context.getResources().getDimensionPixelSize(R.dimen.workspace_extra_padding_bottom);
        return padding;
    }
    
    //动态计算workspaceCellLayout水平padding
    public static int[] calculateHorizontalPadding(Context context) {
        int[] padding = new int[2];
        padding[0] = 0;//context.getResources().getDimensionPixelSize(R.dimen.workspace_extra_padding_left);
        padding[1] = 0;//context.getResources().getDimensionPixelSize(R.dimen.workspace_extra_padding_right);
        return padding;
    }
    
}
