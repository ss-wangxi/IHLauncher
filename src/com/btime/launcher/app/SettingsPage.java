package com.btime.launcher.app;

public class SettingsPage extends AppType {
    public static final AppType PAGE_SETTINGS_CAROS_WLAN = new AppType(101001);
    public static final AppType PAGE_SETTINGS_CAROS_WLAN_ON = new AppType(101101);
    public static final AppType PAGE_SETTINGS_CAROS_WLAN_OFF = new AppType(101201);
    public static final AppType PAGE_SETTINGS_CAROS_BLUETOOTH = new AppType(101301);
    public static final AppType PAGE_SETTINGS_CAROS_BLUETOOTH_ON = new AppType(101401);
    public static final AppType PAGE_SETTINGS_CAROS_BLUETOOTH_OFF = new AppType(101501);
    public static final AppType PAGE_SETTINGS_CAROS_TRAFFIC = new AppType(101601);
    public static final AppType PAGE_SETTINGS_CAROS_TRAFFIC_ON = new AppType(101701);
    public static final AppType PAGE_SETTINGS_CAROS_TRAFFIC_OFF = new AppType(101801);
    public static final AppType PAGE_SETTINGS_CAROS_FM = new AppType(101901);
    public static final AppType PAGE_SETTINGS_CAROS_FM_ON = new AppType(102001);
    public static final AppType PAGE_SETTINGS_CAROS_FM_OFF = new AppType(102101);
    public static final AppType PAGE_SETTINGS_CAROS_AP = new AppType(102201);
    public static final AppType PAGE_SETTINGS_CAROS_AP_ON = new AppType(102301);
    public static final AppType PAGE_SETTINGS_CAROS_AP_OFF = new AppType(102401);
    public static final AppType PAGE_SETTINGS_CAROS_VOLUME_UP = new AppType(102501);
    public static final AppType PAGE_SETTINGS_CAROS_VOLUME_DOWN = new AppType(102601);
    public static final AppType PAGE_SETTINGS_CAROS_VOLUME_ON = new AppType(102701);
    public static final AppType PAGE_SETTINGS_CAROS_VOLUME_OFF = new AppType(102801);
    public static final AppType PAGE_SETTINGS_CAROS_VOLUME_MAX = new AppType(102901);
    public static final AppType PAGE_SETTINGS_CAROS_VOLUME_MIN = new AppType(103001);
    public static final AppType PAGE_SETTINGS_CAROS_BRIGHTNESS_UP = new AppType(103101);
    public static final AppType PAGE_SETTINGS_CAROS_BRIGHTNESS_DOWN = new AppType(103201);
    public static final AppType PAGE_SETTINGS_CAROS_BRIGHTNESS_MAX = new AppType(103301);
    public static final AppType PAGE_SETTINGS_CAROS_BRIGHTNESS_MIN = new AppType(103401);
    
    protected SettingsPage(int type) {
        super(type);
    }
}
