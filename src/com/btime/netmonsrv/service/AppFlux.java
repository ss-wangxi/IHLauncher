package com.btime.netmonsrv.service;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;

public class AppFlux implements Parcelable {
	public int _collapseKey; //uid
	public String _appName;
	public String _appFlux;
	public String _pkgname;
	public Bitmap _icon;
	public String _id;
	public long _flux;
	public long _tx;
	public long _rx;
    public SparseBooleanArray _uids = new SparseBooleanArray();
    public int _netState;
    
    
    public void addUid(int uid) {
        _uids.put(uid, true);
    }

	public AppFlux(int collapseKey) {
		_collapseKey = collapseKey;
	}

	private AppFlux(Parcel source) {
		_collapseKey = source.readInt();
		_appName = source.readString();
		_appFlux = source.readString();
		_pkgname = source.readString();
		_icon = source.readParcelable(this.getClass().getClassLoader());
		_id = source.readString();
		_flux = source.readLong();
		_tx = source.readLong();
		_rx = source.readLong();
        _uids = source.readSparseBooleanArray();
        _netState = source.readInt();
	}

	@Override
	public int describeContents() {
		return this.hashCode();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(_collapseKey);
		dest.writeString(_appName);
		dest.writeString(_appFlux);
		dest.writeString(_pkgname);
		dest.writeParcelable(_icon, flags);
		dest.writeString(_id);
		dest.writeLong(_flux);
		dest.writeLong(_tx);
		dest.writeLong(_rx);
        dest.writeSparseBooleanArray(_uids);
        dest.writeInt(_netState);
}

	public static final Parcelable.Creator<AppFlux> CREATOR = new Parcelable.Creator<AppFlux>() {

		@Override
		public AppFlux[] newArray(int size) {
			return new AppFlux[size];
		}

		@Override
		public AppFlux createFromParcel(Parcel source) {
			return new AppFlux(source);
		}
	};
}