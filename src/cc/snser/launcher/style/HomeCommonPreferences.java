package cc.snser.launcher.style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.snser.launcher.Constant;
import cc.snser.launcher.support.settings.GestureSettings;
import cc.snser.launcher.support.settings.GestureType;
import cc.snser.launcher.ui.utils.SettingsConstants;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.DeviceUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class HomeCommonPreferences {
	private SharedPreferences mSharedPreferences;
	private Set<String> mWhiteListMap = new HashSet<String>();

	private boolean mWhiteListChanged = false;
	
	private static final String TAG = "HomeCommonPreferences";
	private static final String KEY_STYLE_MODE = "pref_k_style_mode";
	private static final String KEY_IMPORTED = "importimanager_dataimported"; 
	private static final String KEY_IS_DOUBLELAYER = "pref_k_launcher_layer";
	private static final String KEY_CLASSIFY = "pref_k_classify";
	private static final String KEY_REPORT_GPS_ENABLED = "pref_k_r_g_enabled";
	
	private GestureSettings mWorkspaceGestureUpAction = null;
    private GestureSettings mWorkspaceGestureDownAction = null;
    private GestureSettings mWorkspaceGestureDoubleClickAction = null;
	
	public HomeCommonPreferences(SharedPreferences preferences){
		mSharedPreferences = preferences;
	}
	
	public void setHomeStyleMode(int mode){
		mSharedPreferences.edit().putInt(KEY_STYLE_MODE, mode).commit();
	}
	
	public int getHomeStyleMode(){
		return mSharedPreferences.getInt(KEY_STYLE_MODE, 0);
	}
	
	
	public void setIntelligentClassification(boolean classify){
		mSharedPreferences.edit().putBoolean(KEY_CLASSIFY, classify).commit();
	}
	
	public boolean isIntelligentClassification(){
		return mSharedPreferences.getBoolean(KEY_CLASSIFY, false);
	}
	
	public void onWhiteListChanged(Context context,
            List<String> mappspacknames) {
        mWhiteListMap.clear();

        mWhiteListMap.add(Constant.PACKAGE_NAME);

        //系统级的进程
        mWhiteListMap.add("android"); //系统
        mWhiteListMap.add("com.android.phone"); //拨号器
        mWhiteListMap.add("com.android.mms"); //信息

        mWhiteListMap.add(Constant.ANDROID_SETTINGS_PACKAGENAME); //设置
        mWhiteListMap.add("com.android.systemui"); //状态栏

        mWhiteListMap.add("com.android.providers.settings"); //设置存储
        mWhiteListMap.add("com.android.providers.applications"); //搜索应用程序提供
        mWhiteListMap.add("com.android.providers.contacts"); //联系人存储
        mWhiteListMap.add("com.android.providers.userdictionary"); //用户字典
        mWhiteListMap.add("com.android.providers.telephony"); //拨号器存存储
        mWhiteListMap.add("com.android.providers.drm"); //DRM 保护的内容的存储
        mWhiteListMap.add("com.android.providers.downloads"); //下载管理器
        mWhiteListMap.add("com.android.providers.media"); //媒体存储

        List<String> adaptiveWhiteList = getAdaptiveWhiteList(context);
        if (adaptiveWhiteList != null) {
            mWhiteListMap.addAll(adaptiveWhiteList);
        }

        if (mappspacknames != null) {
            mWhiteListMap.addAll(mappspacknames);
            mWhiteListChanged = true;
            return;
        }

        String mAppPackagename = new String();

        mAppPackagename = mSharedPreferences.getString(
                SettingsConstants.KEY_TASK_MANAGER_WHITE_LIST, null);

        if (mAppPackagename != null) {
            String[] mAppList = mAppPackagename.split(",");
            mWhiteListMap.addAll(Arrays.asList(mAppList));
        }

        mWhiteListChanged = true;
    }

    private List<String> getAdaptiveWhiteList(Context context) {
        String model = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;

        if (Constant.LOGD_ENABLED) {
            XLog.d(TAG, "model " + model + " manufacturer " + manufacturer);
        }

        List<String> adaptiveList = new ArrayList<String>();

        //根据厂商的适配
        if (manufacturer.equalsIgnoreCase("HTC")) {
            adaptiveList.add("com.android.htccontacts"); //联系人
            adaptiveList.add("com.android.htcdialer"); //拨号器
            adaptiveList.add("com.htc.messagecs"); //MessageCS
            adaptiveList.add("com.htc.idlescreen.shortcut"); //壁纸
            adaptiveList.add("com.android.providers.htcCheckin"); //HTC Checkin Service
        } else if (manufacturer.equalsIgnoreCase("ZTE")) {
            adaptiveList.add("zte.com.cn.alarmclock"); //闹钟时钟
            adaptiveList.add("com.android.utk"); //无线升级
        } else if (manufacturer.equalsIgnoreCase("huawei")) {
            adaptiveList.add("com.huawei.widget.localcityweatherclock"); //时钟
        } else if (manufacturer.equalsIgnoreCase("Sony Ericsson")) {
            adaptiveList.add("com.sonyericsson.provider.useragent"); //UserAgentProvider
            adaptiveList.add("com.sonyericsson.provider.customization"); //CustomizationProvider
            adaptiveList.add("com.sonyericsson.secureclockservice"); //时钟
            adaptiveList.add("com.sonyericsson.widget.digitalclock"); //时钟
            adaptiveList.add("com.sonyericsson.digitalclockwidget"); //时钟
        } else if (manufacturer.equalsIgnoreCase("samsung")) {
            adaptiveList.add("com.samsung.inputmethod"); //中文键盘
            adaptiveList.add("com.sec.android.app.controlpanel"); //任务管理器
            adaptiveList.add("com.sonyericsson.provider.customization"); //Wi-Fi sharing manager
        } else if (manufacturer.equalsIgnoreCase("motorola")) {
            adaptiveList.add("com.motorola.numberlocation"); // 号码归属地查询
            adaptiveList.add("com.motorola.android.fota"); // FOTA
            adaptiveList.add("com.motorola.atcmd"); // AtCommandService
            adaptiveList.add("com.motorola.locationsensor"); // LocationSensor
            adaptiveList.add("com.motorola.blur.conversations"); // 信息
            adaptiveList.add("com.motorola.blur.alarmclock"); // 闹钟
            adaptiveList.add("com.motorola.blur.providers.contacts"); // 存储联系人信息
        } else if (manufacturer.equalsIgnoreCase("LGE")) {
            adaptiveList.add("com.lge.clock"); // clock
        } else if (manufacturer.equalsIgnoreCase("magnum2x")) { // 2天语 大黄蜂2
            adaptiveList.add("ty.com.android.TYProfileSetting"); // clock
        }

        // 根据机型的适配
        if (model.equalsIgnoreCase("HTC Sensation Z710e")
                || model.equalsIgnoreCase("HTC Sensation G14")
                || model.equalsIgnoreCase("HTC Z710e")) {
            adaptiveList.add("android.process.acore");
        } else if (model.equalsIgnoreCase("LT18i")) {
            adaptiveList.add("com.sonyericsson.provider.customization");
            adaptiveList.add("com.sonyericsson.provider.useragent");
        } else if (model.equalsIgnoreCase("U8500") || model.equalsIgnoreCase("U8500 HiQQ")) {
            adaptiveList.add("android.process.launcherdb");
            adaptiveList.add("com.motorola.process.system");
            adaptiveList.add("com.nd.assistance.ServerService");
        } else if (model.equalsIgnoreCase("MT15I")) {
            adaptiveList.add("com.sonyericsson.eventstream.calllogplugin"); //通话记录扩展
        } else if (model.equalsIgnoreCase("GT-I9100") || model.equalsIgnoreCase("GT-I9100G")) {
            adaptiveList.add("com.samsung.inputmethod"); //通话记录扩展
            adaptiveList.add("com.sec.android.app.controlpanel"); //任务管理器
            adaptiveList.add("com.sec.android.app.FileTransferManager"); // Wi-Fi sharing manager
            adaptiveList.add("com.sec.android.providers.downloads"); // our screen lock call taskmanager error
            adaptiveList.add("com.android.providers.downloads.ui"); // our screen lock call taskmanager error
        } else if (model.equalsIgnoreCase("DROIDX")) { //moto_droid3
            adaptiveList.add("com.motorola.blur.contacts.data"); //联系人
            adaptiveList.add("com.motorola.blur.contacts"); //联系人
        } else if (model.equalsIgnoreCase("DROID2") || model.equalsIgnoreCase("DROID2 GLOBA")) { //DROID2
            adaptiveList.add("com.motorola.blur.contacts"); //联系人
        } else if (DeviceUtils.isU8800()) {
            adaptiveList.add("com.huawei.android.gpms");
            adaptiveList.add("com.android.hwdrm");
            adaptiveList.add("com.huawei.omadownload");
        } else if (model.equalsIgnoreCase("LG-P503")) {
            adaptiveList.add("com.lge.simcontacts"); //联系人
        } else if (model.equalsIgnoreCase("XT702")) {
            adaptiveList.add("com.motorola.usb"); // 杀后无法连接电脑
            adaptiveList.add("com.android.alarmclock"); // 闹钟
        } else if (model.equalsIgnoreCase("e15i")) {// e15i live wallpaper
            adaptiveList.add("com.sec.ccl.csp.app.secretwallpaper.themetwo"); // 闹钟
        } else if (model.equalsIgnoreCase("zte-c n600")) {
            mWhiteListMap.add("com.android.wallpaper"); // galaxy, grass, nexus, polar clock, water
            mWhiteListMap.add("com.android.musicvis"); // many, vu Meter , waveform, spectrun
            mWhiteListMap.add("com.android.magicsmoke"); // smoke
        } else if (DeviceUtils.isGtS5830() || DeviceUtils.isGtS5830i() || model.startsWith("HTC Velocity 4G")) {
            mWhiteListMap.add("com.android.providers.downloads.ui"); // 锁屏解锁清内存时，我们桌面如果开着音乐播放器，桌面会重启
        }

        if (adaptiveList.size() != 0) {
            return adaptiveList;
        } else {
            return null;
        }
    }

    public Set<String> getWhiteList(Context context) {
        if (!mWhiteListChanged) {
            onWhiteListChanged(context, null);
        }
        return mWhiteListMap;
    }
    
    public GestureSettings getWorkspaceGestureSettings(Context context, GestureType gestureType) {
        GestureSettings gestureSettings = gestureType.restoreFromOld(context);
        if (gestureSettings != null) {
            
            mSharedPreferences.edit().putString(gestureType.getPrefKey(context), gestureSettings.toString()).commit();
            return gestureSettings;
        }

        return GestureSettings.from(context, gestureType, mSharedPreferences.getString(gestureType.getPrefKey(context), ""));
    }

    public void setWorkspaceGestureSettings(Context context, GestureSettings gestureSettings) {
        
        mSharedPreferences.edit().putString(gestureSettings.gestureType.getPrefKey(context), gestureSettings.toString()).commit();

        if (gestureSettings.gestureType == GestureType.UP) {
        	mWorkspaceGestureUpAction = gestureSettings;
        } else if (gestureSettings.gestureType == GestureType.DOWN) {
            mWorkspaceGestureDownAction = gestureSettings;
        } else if (gestureSettings.gestureType == GestureType.DOUBLE_CLICK) {
            mWorkspaceGestureDoubleClickAction = gestureSettings;
        }
    }

    public GestureSettings getWorkspaceGestureUpAction(Context context) {
        if (mWorkspaceGestureUpAction == null) {
            mWorkspaceGestureUpAction = getWorkspaceGestureSettings(context, GestureType.UP);
        }
        return mWorkspaceGestureUpAction;
    }

    public GestureSettings getWorkspaceGestureDownAction(Context context) {
        if (mWorkspaceGestureDownAction == null) {
            mWorkspaceGestureDownAction = getWorkspaceGestureSettings(context, GestureType.DOWN);
        }
        return mWorkspaceGestureDownAction;
    }

    public GestureSettings getWorkspaceGestureDoubleClickAction(Context context) {
        if (mWorkspaceGestureDoubleClickAction == null) {
            mWorkspaceGestureDoubleClickAction = getWorkspaceGestureSettings(context, GestureType.DOUBLE_CLICK);
        }
        return mWorkspaceGestureDoubleClickAction;
    }
    
    public int isAdaptDataImported(){
    	return mSharedPreferences.getInt(KEY_IMPORTED, 0);
    }
    
    public void setAdaptDataImported(int imported){
    	mSharedPreferences.edit().putInt(KEY_IMPORTED, imported).commit();
    }
    
    public boolean hasSetLauncherLayer(){
    	int val = mSharedPreferences.getInt(KEY_IS_DOUBLELAYER, 0);
    	if(val == 1 || val == 2){
    		return true;
    	}
    	return false;
    }
    
    public void setLauncherLayer(boolean doubleLay){
    	mSharedPreferences.edit().putInt(KEY_IS_DOUBLELAYER, doubleLay ? 2 : 1).commit();
    }
    
    public boolean getReportGPSEnabled() {
        return mSharedPreferences.getBoolean(KEY_REPORT_GPS_ENABLED, false);
    }
    
    public void setReportGPSEnabled(boolean isEnabled) {
        mSharedPreferences.edit().putBoolean(KEY_REPORT_GPS_ENABLED, isEnabled).commit();
    }
}
