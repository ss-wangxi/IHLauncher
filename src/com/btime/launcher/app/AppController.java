package com.btime.launcher.app;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import cc.snser.launcher.App;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.Launcher.LauncherState;
import cc.snser.launcher.screens.Workspace;

import com.btime.launcher.util.XLog;
import com.btime.netmonsrv.service.NetmonServiceHelper;
import com.btime.settings.externalcall.SettingsServiceHelper;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.ToastUtils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.Toast;

public class AppController {
    
    private SparseArray<Intent> mAppStartIntents;
    private SparseArray<AppType> mDefaultApps;
    
    private ActivityManager mActivityManager;
    private PackageManager mPackageManager;
    private Field mFieldProcessState;
    
    private static final String PKGNAME_TRAFFIC_CAROS = "com.caros.netmon";
    public static final String PKGNAME_SETTINGS_CAROS = "com.caros.settings";
    
    public static final String PKGNAME_C601_CAMERA = "com.android.gallery3d";
    public static final String PKGNAME_C601_GALLERY = "com.android.gallery3d";
    public static final String PKGNAME_C601_BTIME = "com.btime.bjtime";
    public static final String PKGNAME_C601_SETTINGS = "com.android.settings";
    
    public static final String CLSNAME_C601_CAMERA = "com.android.camera.CameraActivity";
    public static final String CLSNAME_C601_GALLERY = "com.android.gallery3d.app.GalleryActivity";
    
    private static final int SCROLL_PAGE_DIRECTION_LEFT = -1;
    private static final int SCROLL_PAGE_DIRECTION_RIGHT = 1;
    private static final int SCROLL_PAGE_DIRECTION_HOME = 0;
    
    /**
     * 成功
     */
    public static final int CHECK_SUPPORT_ERRCODE_OK = 0;
    /**
     * 失败(当前不在桌面)
     */
    public static final int CHECK_SUPPORT_ERRCODE_HOME = -1;
    /**
     * 失败(不支持该方向滑屏)
     */
    public static final int CHECK_SUPPORT_ERRCODE_DIRECTION = -2;
    
    private AppController() {
        mActivityManager = (ActivityManager)App.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        mPackageManager = App.getAppContext().getPackageManager();
        try {
            mFieldProcessState = RunningAppProcessInfo.class.getDeclaredField("processState");
        } catch (Exception e) {
        }
        initInternalAppIntents();
        initDefaultApps();
    }
    
    private static class SingletonHolder {
        private static AppController sInstance = new AppController();
    }
    
    public static AppController getInstance() {
        return SingletonHolder.sInstance;
    }
    
    private void initInternalAppIntents() {
        mAppStartIntents = new SparseArray<Intent>();
        {
            final Intent intent = mPackageManager.getLaunchIntentForPackage(PKGNAME_TRAFFIC_CAROS);
            mAppStartIntents.put(AppPage.PAGE_TRAFFIC_CAROS_MAIN.value(), (intent != null ? intent : new Intent().setPackage(PKGNAME_TRAFFIC_CAROS)));
        }
        {
            final Intent intent = mPackageManager.getLaunchIntentForPackage(PKGNAME_SETTINGS_CAROS);
            mAppStartIntents.put(AppPage.PAGE_SETTINGS_CAROS_MAIN.value(), (intent != null ? intent : new Intent().setPackage(PKGNAME_SETTINGS_CAROS)));
        }
    }
    
    private void initDefaultApps() {
        mDefaultApps = new SparseArray<>();
        
        mDefaultApps.put(AppType.TYPE_TRAFFIC.value(), AppPage.PAGE_TRAFFIC_CAROS_MAIN);
        mDefaultApps.put(AppType.TYPE_SETTINGS.value(), AppPage.PAGE_SETTINGS_CAROS_MAIN);
        
        mDefaultApps.put(AppType.TYPE_SETTINGS_WLAN.value(), SettingsPage.PAGE_SETTINGS_CAROS_WLAN);
        mDefaultApps.put(AppType.TYPE_SETTINGS_WLAN_ON.value(), SettingsPage.PAGE_SETTINGS_CAROS_WLAN_ON);
        mDefaultApps.put(AppType.TYPE_SETTINGS_WLAN_OFF.value(), SettingsPage.PAGE_SETTINGS_CAROS_WLAN_OFF);
        mDefaultApps.put(AppType.TYPE_SETTINGS_BLUETOOTH.value(), SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH);
        mDefaultApps.put(AppType.TYPE_SETTINGS_BLUETOOTH_ON.value(), SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH_ON);
        mDefaultApps.put(AppType.TYPE_SETTINGS_BLUETOOTH_OFF.value(), SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH_OFF);
        mDefaultApps.put(AppType.TYPE_SETTINGS_TRAFFIC.value(), SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC);
        mDefaultApps.put(AppType.TYPE_SETTINGS_TRAFFIC_ON.value(), SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC_ON);
        mDefaultApps.put(AppType.TYPE_SETTINGS_TRAFFIC_OFF.value(), SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC_OFF);
        mDefaultApps.put(AppType.TYPE_SETTINGS_FM.value(), SettingsPage.PAGE_SETTINGS_CAROS_FM);
        mDefaultApps.put(AppType.TYPE_SETTINGS_FM_ON.value(), SettingsPage.PAGE_SETTINGS_CAROS_FM_ON);
        mDefaultApps.put(AppType.TYPE_SETTINGS_FM_OFF.value(), SettingsPage.PAGE_SETTINGS_CAROS_FM_OFF);
        mDefaultApps.put(AppType.TYPE_SETTINGS_AP.value(), SettingsPage.PAGE_SETTINGS_CAROS_AP);
        mDefaultApps.put(AppType.TYPE_SETTINGS_AP_ON.value(), SettingsPage.PAGE_SETTINGS_CAROS_AP_ON);
        mDefaultApps.put(AppType.TYPE_SETTINGS_AP_OFF.value(), SettingsPage.PAGE_SETTINGS_CAROS_AP_OFF);
        mDefaultApps.put(AppType.TYPE_SETTINGS_VOLUME_UP.value(), SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_UP);
        mDefaultApps.put(AppType.TYPE_SETTINGS_VOLUME_DOWN.value(), SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_DOWN);
        mDefaultApps.put(AppType.TYPE_SETTINGS_VOLUME_ON.value(), SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_ON);
        mDefaultApps.put(AppType.TYPE_SETTINGS_VOLUME_OFF.value(), SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_OFF);
        mDefaultApps.put(AppType.TYPE_SETTINGS_VOLUME_MAX.value(), SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_MAX);
        mDefaultApps.put(AppType.TYPE_SETTINGS_VOLUME_MIN.value(), SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_MIN);
        mDefaultApps.put(AppType.TYPE_SETTINGS_BRIGHTNESS_UP.value(), SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_UP);
        mDefaultApps.put(AppType.TYPE_SETTINGS_BRIGHTNESS_DOWN.value(), SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_DOWN);
        mDefaultApps.put(AppType.TYPE_SETTINGS_BRIGHTNESS_MAX.value(), SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_MAX);
        mDefaultApps.put(AppType.TYPE_SETTINGS_BRIGHTNESS_MIN.value(), SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_MIN);
    }
    
    /**
     * 获取实际启动的AppPage
     * @param type AppType.TYPE_XXX
     * @return AppPage.PAGE_XXX
     */
    public AppType getAppPage(AppType type) {
        return type == null ? null : mDefaultApps.get(type.value());
    }
    
    /**
     * 获取AppPage对应的packagename
     * @param page
     * @return
     */
    public String getAppPagePackage(AppType page) {
        return getIntentPackage(getAppPageIntent(page));
    }
    
    /**
     * 获取AppPage对应的Intent
     * @param page
     * @return
     */
    public Intent getAppPageIntent(AppType page) {
        if (page != null) {
            final Intent intent =  mAppStartIntents.get(page.value());
            if (intent != null) {
                if (intent.getComponent() == null && intent.getData() == null && intent.getPackage() != null) {
                    final Intent intentNew = mPackageManager.getLaunchIntentForPackage(intent.getPackage());
                    if (intentNew != null) {
                        intentNew.setPackage(intent.getPackage());
                        mAppStartIntents.put(page.value(), intentNew);
                        return intentNew;
                    }
                }
                return intent;
            }
        }
        return null;
    }
    
    public void setDefaultApp(AppType type, AppPage page) {
        if (type != null && page != null) {
            mDefaultApps.put(type.value(), page);
        }
    }
    
    private String getIntentPackage(Intent intent) {
        if (intent != null) {
            if (intent.getPackage() != null) {
                return intent.getPackage();
            } else if (intent.getComponent() != null) {
                return intent.getComponent().getPackageName();
            }
        }
        return null;
    }
    
    /**
     * 判断Package的主进程是否存在
     * @param pkg 包名
     * @return
     */
    public boolean checkPackageMainProgressAlive(String pkg) {
        boolean isAlive = false;
        if (!TextUtils.isEmpty(pkg)) {
            final List<RunningAppProcessInfo> processes = mActivityManager.getRunningAppProcesses();
            for (RunningAppProcessInfo process : processes) {
                if (pkg.equals(process.processName) && process.pkgList != null) {
                    for (String pkgname : process.pkgList) {
                        if (pkg.equals(pkgname)) {
                            isAlive = true;
                            break;
                        }
                    }
                }
                if (isAlive) {
                    break;
                }
            }
        }
        return isAlive;
    }
    
    public boolean checkPackageMainProgressTop(String pkg) {
        boolean isTop = false;
        if (!TextUtils.isEmpty(pkg) && mFieldProcessState != null) {
            final List<RunningAppProcessInfo> processes = mActivityManager.getRunningAppProcesses();
            final int PROCESS_STATE_TOP = 2;
            for (RunningAppProcessInfo process : processes) {
                boolean isAlive = false;
                if (pkg.equals(process.processName) && process.pkgList != null) {
                    for (String pkgname : process.pkgList) {
                        if (pkg.equals(pkgname)) {
                            isAlive = true;
                            break;
                        }
                    }
                }
                if (isAlive) {
                    try {
                        if (mFieldProcessState.getInt(process) == PROCESS_STATE_TOP) {
                            isTop = true;
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return isTop;
    }
    
    
    /**
     * 通过类型启动app
     * @param type AppType.TYPE_XXX
     * @return true:启动成功 false:启动失败
     */
    public boolean startApp(AppType type) {
        final AppType appPage = type != null ? mDefaultApps.get(type.value()) : null;
        if (appPage != null) {
            if (SettingsServiceHelper.getIntance().filterStartIntent(appPage)) {
                return SettingsServiceHelper.getIntance().startApp(appPage);
            } else if (BroadcastHelper.getIntance().filterStartIntent(appPage)) {
                return BroadcastHelper.getIntance().sendBroadcast(getAppPageIntent(appPage));
            } else {
                return startActivity(getAppPageIntent(appPage), false);
            }
        }
        return false;
    }
    
    public boolean startActivity(Intent intent) {
        return startActivity(intent, false);
    }
    
    public boolean startActivity(Intent intent, boolean toastError) {
        boolean isStartSucc = false;
        if (filterStartIntent(intent)) {
            final Launcher launcher = Launcher.getInstance();
            if (launcher != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK/* | Intent.FLAG_ACTIVITY_CLEAR_TASK*/);
                XLog.d("Snser", "startActivity intent=" + intent.hashCode() + " extra=" + intent.getBooleanExtra("start_from_launcher", false));
                try {
                    launcher.getApplicationContext().startActivity(intent);
                    isStartSucc = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final String pkgname = getIntentPackage(intent);
                if (isStartSucc) {
                    handleTrafficCtrl(pkgname);
                    handleCtrlLauncher(pkgname);
                } else if (toastError) {
                    ToastUtils.showMessage(launcher.getApplicationContext(), R.string.activity_not_found, Toast.LENGTH_SHORT);
                }
            }
        }
        return isStartSucc;
    }
    
    private boolean filterStartIntent(Intent intent) {
        return getIntentPackage(intent) != null;
    }

    
    public boolean stopApp(AppType type) {
        final AppType appPage = type != null ? mDefaultApps.get(type.value()) : null;
        if (appPage != null) {
            return forceStopPackage(getAppPagePackage(appPage));
        }
        return false;
    }
    
    private boolean forceStopPackage(String pkgname) {
        if (!TextUtils.isEmpty(pkgname)) {
            try {
                Method method = ActivityManager.class.getMethod("forceStopPackage", String.class);
                if (method != null) {
                    method.invoke(mActivityManager, pkgname);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    
    public boolean setAppEnabled(AppType type, boolean isEnabled) {
        final AppType appPage = type != null ? mDefaultApps.get(type.value()) : null;
        if (appPage != null) {
            return setPackageEnabled(getAppPagePackage(appPage), isEnabled);
        }
        return false;
    }
    
    private boolean setPackageEnabled(String pkgname, boolean isEnabled) {
        if (!TextUtils.isEmpty(pkgname)) {
            try {
                int state = isEnabled 
                             ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
                             : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                mPackageManager.setApplicationEnabledSetting(pkgname, state, 0);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    
    public boolean notifyStartApp(AppType type, String extra) {
        XLog.d("Snser", "notifyStartApp type=" + type.value() + " extra=" + extra);
        final AppType appPage = type != null ? mDefaultApps.get(type.value()) : null;
        final String pkgname = getAppPagePackage(appPage);
        XLog.d(Workspace.TAG, "notifyStartApp handleCtrlLauncher extra=" + extra);
        handleTrafficCtrl(pkgname);
        return handleCtrlLauncher(pkgname, extra);
    }
    
    
    public boolean handleCtrlLauncher(String pkgname) {
        return handleCtrlLauncher(pkgname, null);
    }
    
    public boolean handleCtrlLauncher(String pkgname, String extra) {
        return true;
    }
    
    public void handleTrafficCtrl(final String pkgname) {
        if (NetmonServiceHelper.getInstance().isAppNetworkDisabled(pkgname)) {
            App.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.showMessage(App.getAppContext(), "该应用已禁止联网");
                }
            });
        }
    }
    
    /**
     * 检测是否支持指定方向滑屏
     * @param direction 滑屏方向(1:右滑 -1:左滑 0:滑到主屏)
     * @return {@link #CHECK_SUPPORT_ERRCODE_OK}: 成功</br>
     *          {@link #CHECK_SUPPORT_ERRCODE_HOME}: 失败(当前不在桌面)</br>
     *          {@link #CHECK_SUPPORT_ERRCODE_DIRECTION}: 失败(不支持该方向滑屏)
     */
    public int checkSupportScrollPage(int direction) {
        final Launcher launcher = Launcher.getInstance();
        final Workspace workspace = launcher.getWorkspace();
        XLog.i("Snser", "scrollPage state=" + launcher.getLauncherState() + " locked=" + workspace.isLocked());
        XLog.i("Snser", "scrollPage direction=" + direction + " currentScreen=" + workspace.getCurrentScreen() + " screenCount=" + workspace.getScreenCount());
        if (launcher.getLauncherState() == LauncherState.ONRESUME
            && !workspace.isLocked()) {
            final int currentScreen = workspace.getCurrentScreen();
            final int screenCount = workspace.getScreenCount();
            if (direction == SCROLL_PAGE_DIRECTION_LEFT) {
                return currentScreen > 0 && currentScreen <= screenCount - 1 ? CHECK_SUPPORT_ERRCODE_OK : CHECK_SUPPORT_ERRCODE_DIRECTION;
            } else if (direction == SCROLL_PAGE_DIRECTION_RIGHT) {
                return currentScreen >= 0 && currentScreen < screenCount - 1 ? CHECK_SUPPORT_ERRCODE_OK : CHECK_SUPPORT_ERRCODE_DIRECTION;
            } else if (direction == SCROLL_PAGE_DIRECTION_HOME) {
                return CHECK_SUPPORT_ERRCODE_OK;
            }
        }
        return CHECK_SUPPORT_ERRCODE_HOME;
    }

    /**
     * 指定方向滑屏
     * @param direction 滑屏方向(1:右滑 -1:左滑 0:滑到主屏)
     * @return {@link #CHECK_SUPPORT_ERRCODE_OK}: 成功</br>
     *          {@link #CHECK_SUPPORT_ERRCODE_HOME}: 失败(当前不在桌面)</br>
     *          {@link #CHECK_SUPPORT_ERRCODE_DIRECTION}: 失败(不支持该方向滑屏)
     */
    public int scrollPage(final int direction) {
        final int checkSupport = checkSupportScrollPage(direction);
        if (checkSupport == CHECK_SUPPORT_ERRCODE_OK) {
            App.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Workspace workspace = Launcher.getInstance().getWorkspace();
                    if (direction == SCROLL_PAGE_DIRECTION_LEFT) {
                        workspace.scrollLeft();
                    } else if (direction == SCROLL_PAGE_DIRECTION_RIGHT) {
                        workspace.scrollRight();
                    } else if (direction == SCROLL_PAGE_DIRECTION_HOME) {
                        if (workspace.getCurrentScreen() != workspace.getDefaultScreen()) {
                            workspace.scrollToScreen(workspace.getDefaultScreen());
                        }
                    }
                }
            });
        }
        return checkSupport;
    }
    
}
