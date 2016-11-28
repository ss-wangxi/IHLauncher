package com.btime.launcher.app;

import android.os.Parcel;
import android.os.Parcelable;

public class AppType implements Parcelable {
    public static final AppType TYPE_TRAFFIC = new AppType(1005);
    public static final AppType TYPE_SETTINGS = new AppType(1009);
    public static final AppType TYPE_SETTINGS_WLAN = new AppType(1010);
    public static final AppType TYPE_SETTINGS_WLAN_ON = new AppType(1011);
    public static final AppType TYPE_SETTINGS_WLAN_OFF = new AppType(1012);
    public static final AppType TYPE_SETTINGS_BLUETOOTH = new AppType(1013);
    public static final AppType TYPE_SETTINGS_BLUETOOTH_ON = new AppType(1014);
    public static final AppType TYPE_SETTINGS_BLUETOOTH_OFF = new AppType(1015);
    public static final AppType TYPE_SETTINGS_TRAFFIC = new AppType(1016);
    public static final AppType TYPE_SETTINGS_TRAFFIC_ON = new AppType(1017);
    public static final AppType TYPE_SETTINGS_TRAFFIC_OFF = new AppType(1018);
    public static final AppType TYPE_SETTINGS_FM = new AppType(1019);
    public static final AppType TYPE_SETTINGS_FM_ON = new AppType(1020);
    public static final AppType TYPE_SETTINGS_FM_OFF = new AppType(1021);
    public static final AppType TYPE_SETTINGS_AP = new AppType(1022);
    public static final AppType TYPE_SETTINGS_AP_ON = new AppType(1023);
    public static final AppType TYPE_SETTINGS_AP_OFF = new AppType(1024);
    public static final AppType TYPE_SETTINGS_VOLUME_UP = new AppType(1025);
    public static final AppType TYPE_SETTINGS_VOLUME_DOWN = new AppType(1026);
    public static final AppType TYPE_SETTINGS_VOLUME_ON = new AppType(1027);
    public static final AppType TYPE_SETTINGS_VOLUME_OFF = new AppType(1028);
    public static final AppType TYPE_SETTINGS_VOLUME_MAX = new AppType(1029);
    public static final AppType TYPE_SETTINGS_VOLUME_MIN = new AppType(1030);
    public static final AppType TYPE_SETTINGS_BRIGHTNESS_UP = new AppType(1031);
    public static final AppType TYPE_SETTINGS_BRIGHTNESS_DOWN = new AppType(1032);
    public static final AppType TYPE_SETTINGS_BRIGHTNESS_MAX = new AppType(1033);
    public static final AppType TYPE_SETTINGS_BRIGHTNESS_MIN = new AppType(1034);
    
    private int _type;
    
    protected AppType(int type) {
        _type = type;
    }
    
    private AppType(Parcel source) {
        _type = source.readInt();
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_type);
    }
    
    public static final Parcelable.Creator<AppType> CREATOR = new Creator<AppType>() {
        @Override
        public AppType[] newArray(int size) {
            return new AppType[size];
        }
        @Override
        public AppType createFromParcel(Parcel source) {
            return new AppType(source);
        }
    };
    
    public int value() {
        return _type;
    }

}
