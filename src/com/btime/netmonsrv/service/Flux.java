package com.btime.netmonsrv.service;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Flux implements Parcelable {

	public String _todayFlux; //今日流量
	public String _totalFlux;  //本套餐已用
	public long _daysLeft;     //剩余天数
	public String _comboUnused; //剩余流量
	public String _combo;      //套餐流量
	public int   _comboPercent; //套餐使用百分比
	public long _todayFlux_n;
	public long _totalFlux_n;
	public long _comboUnused_n;
	public long _combo_n;
	public List<AppFlux> _flux = new ArrayList<AppFlux>();

	public Flux(){

	}

	public Flux(Parcel in){
		_totalFlux = in.readString();
		_todayFlux = in.readString();
		_daysLeft   = in.readLong();
		_comboUnused = in.readString();
		_combo       = in.readString();
		_comboPercent = in.readInt();
		_todayFlux_n = in.readLong();
		_totalFlux_n = in.readLong();
		_combo_n = in.readLong();
		_comboUnused_n = in.readLong();
		in.readTypedList(_flux, AppFlux.CREATOR);
	}

	@Override
	public int describeContents() {
		return this.hashCode();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(_totalFlux);
		dest.writeString(_todayFlux);
		dest.writeLong(_daysLeft);
		dest.writeString(_comboUnused);
		dest.writeString(_combo);
		dest.writeInt(_comboPercent);
		dest.writeLong(_todayFlux_n);
		dest.writeLong(_totalFlux_n);
		dest.writeLong(_combo_n);
		dest.writeLong(_comboUnused_n);
		dest.writeTypedList(_flux);
	}

	public static final Parcelable.Creator<Flux> CREATOR = new Parcelable.Creator<Flux>() {

		@Override
		public Flux createFromParcel(Parcel source) {
			return new Flux(source);
		}

		@Override
		public Flux[] newArray(int size) {
			return new Flux[size];
		}

	};

}
