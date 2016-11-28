package com.btime.launcher.app;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaStatusInfo implements Parcelable {
    public static final int PLAYING = 1;
    public static final int PAUSE = 2;
    public static final int STOP = 3;
    
    private int _kw_status;
    private long _kw_lastactivetime;
    private int _xmly_status;
    private long _xmly_lastactivetime;
    private int _kl_status;
    private long _kl_lastactivetime;
    
    public MediaStatusInfo(int kwStatus, long kwLastActiveTime, int xmlyStatus, long xmlyLastActiveTime, int klStatus, long klLastActiveTime) {
        _kw_status = kwStatus;
        _kw_lastactivetime = kwLastActiveTime;
        _xmly_status = xmlyStatus;
        _xmly_lastactivetime = xmlyLastActiveTime;
        _kl_status = klStatus;
        _kl_lastactivetime = klLastActiveTime;
        
    }
    
    private MediaStatusInfo(Parcel source) {
        _kw_status = source.readInt();
        _kw_lastactivetime = source.readLong();
        _xmly_status = source.readInt();
        _xmly_lastactivetime = source.readLong();
        _kl_status = source.readInt();
        _kl_lastactivetime = source.readLong();
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_kw_status);
        dest.writeLong(_kw_lastactivetime);
        dest.writeInt(_xmly_status);
        dest.writeLong(_xmly_lastactivetime);
        dest.writeInt(_kl_status);
        dest.writeLong(_kl_lastactivetime);
    }
    
    public static final Parcelable.Creator<MediaStatusInfo> CREATOR = new Creator<MediaStatusInfo>() {
        @Override
        public MediaStatusInfo[] newArray(int size) {
            return new MediaStatusInfo[size];
        }
        @Override
        public MediaStatusInfo createFromParcel(Parcel source) {
            return new MediaStatusInfo(source);
        }
    };
    
    public int getKwStatus() {
        return _kw_status;
    }
    
    public long getKwLastActiveTime() {
        return _kw_lastactivetime;
    }
    
    public int getXmlyStatus() {
        return _xmly_status;
    }
    
    public long getXmlyLastActiveTime() {
        return _xmly_lastactivetime;
    }
    public int getKlStatus() {
        return _kl_status;
    }
    
    public long getKlLastActiveTime() {
        return _kl_lastactivetime;
    }
    
}
