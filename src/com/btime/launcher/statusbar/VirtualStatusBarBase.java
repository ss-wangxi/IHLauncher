package com.btime.launcher.statusbar;

import java.lang.reflect.Method;
import java.util.Date;

import com.btime.launcher.util.XLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;

public class VirtualStatusBarBase {
    private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final String STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION";
    
    public static final String BLUETOOTH_BROADCAST_ACTION = "com.caros.bluetoothservice.ACTION_BROADCARST_MESSAGE";
    public static final int BLUETOOTH_MSG_HFP_STATUS_CHANGE = 500;
    public static final String BLUETOOTH_KEY_MESSAGE = "message";
    public static final String BLUETOOTH_KEY_HFP_STATUS = "hfp_status"; 
    
    public static final int SIM_NETWORK_TYPE_UNKNOWN = 0x00;
    public static final int SIM_NETWORK_TYPE_G = 0x01;
    public static final int SIM_NETWORK_TYPE_E = 0x02;
    public static final int SIM_NETWORK_TYPE_3G = 0x03;
    public static final int SIM_NETWORK_TYPE_H = 0x04;
    public static final int SIM_NETWORK_TYPE_4G = 0x05;
    
    private static VirtualStatusBarBase sInstance;
    private VirtualStatusBar mStatusBar;
    
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;
    private TelephonyManager mTelephonyManager;
    private StorageManager mStorageManager;
    private AudioManager mAudioManager;
    
    private boolean mIs24HourFormat = false;
    private boolean mIsTimeZoneChanged = false;
    private Date mDate = new Date();
    private int mSimSignalLevel = 0;
    
    private VirtualStatusBarBase() {
    }
    
    public static VirtualStatusBarBase getInstance() {
        if (sInstance == null) {
            sInstance = new VirtualStatusBarBase();
        }
        return sInstance;
    }
    
    public void init(Context context, VirtualStatusBar statusbar) {
        mStatusBar = statusbar;        
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(new SimStateListener(), 
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);      
        mIs24HourFormat = DateFormat.is24HourFormat(context);  
        initStatusBar(context);
        registerBroadcastReceiver(context);
    }
    
    private void registerBroadcastReceiver(Context context) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
        filter.addAction(BLUETOOTH_BROADCAST_ACTION);               
        filter.addAction(VOLUME_CHANGED_ACTION);
        filter.addAction(STREAM_MUTE_CHANGED_ACTION);       
        filter.addAction(BLUETOOTH_BROADCAST_ACTION);       
        //filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(new StatusBarBroadcastReceiver(), filter);
        
        final IntentFilter filterSdcard = new IntentFilter();
        filterSdcard.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filterSdcard.addAction(Intent.ACTION_MEDIA_EJECT);
        filterSdcard.addDataScheme("file");
        context.registerReceiver(new SdcardBroadcastReceiver(), filterSdcard);
        
    }
    
    private void initStatusBar(Context context) {
        onTimeChanged();
        onWifiStateChanged();
        onApStateChanged();
        onBluetoothStateChanged(context);
        onSimStateChanged();
        onSdcardStateChanged();
        onMuteStateChanged();
    }
    
    private class StatusBarBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_TIMEZONE_CHANGED:
                    mIsTimeZoneChanged = true;
                case Intent.ACTION_TIME_CHANGED:
                    mIs24HourFormat = DateFormat.is24HourFormat(context);
                case Intent.ACTION_TIME_TICK:
                    onTimeChanged();
                    mIsTimeZoneChanged = false;
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                case WifiManager.RSSI_CHANGED_ACTION:
                    onWifiStateChanged();
                    break;
                case WIFI_AP_STATE_CHANGED_ACTION:
                    onApStateChanged();
                    break;
                case VOLUME_CHANGED_ACTION:
                case STREAM_MUTE_CHANGED_ACTION:
                    onMuteStateChanged();
                    break;
                case BLUETOOTH_BROADCAST_ACTION:             
                	int what = intent.getIntExtra(BLUETOOTH_KEY_MESSAGE,0);
                	if (what == BLUETOOTH_MSG_HFP_STATUS_CHANGE ) {
                		int status = intent.getIntExtra(BLUETOOTH_KEY_HFP_STATUS, 0);
                		onBluetoothNameChanged(status);
                	}
                    break;
                default:
                    break;
            }
        }
    }
    
    private class SdcardBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_MEDIA_MOUNTED:
                case Intent.ACTION_MEDIA_EJECT:
                    onSdcardStateChanged();
                    break;
                default:
                    break;
            }
        }
    }
    
    private class SimStateListener extends PhoneStateListener {
        @Override
        public void onDataConnectionStateChanged(int state) {
            onSimStateChanged();
        }
        
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            try {
                Method method = signalStrength.getClass().getMethod("getLevel"); 
                mSimSignalLevel = (int)method.invoke(signalStrength);
            } catch (Exception e) {
                e.printStackTrace();
            }
            onSimStateChanged();
        }
        
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            final int state = serviceState.getState();
            if (state == ServiceState.STATE_OUT_OF_SERVICE || state == ServiceState.STATE_POWER_OFF) {
                mSimSignalLevel = 0;
            }
            onSimStateChanged();
        }
    }
    
    private void onTimeChanged() {
        mDate.setTime(System.currentTimeMillis());
        mStatusBar.setTimeStatus(mDate, mIs24HourFormat, mIsTimeZoneChanged);
    }
    
    private void onWifiStateChanged() {
        boolean isConnected = false;
        int signalLevel = 0;
        if (mWifiManager.isWifiEnabled()) {
            final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            final NetworkInfo wifiConnectInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiInfo != null && wifiConnectInfo != null && wifiConnectInfo.isConnected()) {
                isConnected = true;
                signalLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), VirtualStatusBarIcons.Wifi.SIGNAL_LEVEL_COUNT);
            }
        }
        mStatusBar.setWifiStatus(isConnected, signalLevel);
        XLog.d("Snser", "onWifiStateChanged connect=" + isConnected + " level=" + signalLevel);
    }
    
    private void onApStateChanged() {
        boolean isApEnabled = false;
        try {
            Method method = mWifiManager.getClass().getMethod("isWifiApEnabled"); 
            isApEnabled = (boolean)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mStatusBar.setApStatus(isApEnabled);
        XLog.d("Snser", "onApStateChanged enable=" + isApEnabled);
    }
    
    private void onBluetoothStateChanged(Context context) {
    }
    
    private void onBluetoothNameChanged(int status) {
		if (status >= 3) {
			mStatusBar.setBluetoothStatus(true);
	    } else {
	    	mStatusBar.setBluetoothStatus(false);
	    }		
	}
    
    private void onSimStateChanged() {
        boolean isExist = mTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
        int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        switch (mTelephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                type = SIM_NETWORK_TYPE_G;
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                type = SIM_NETWORK_TYPE_E;
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                type = SIM_NETWORK_TYPE_3G;
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                type = SIM_NETWORK_TYPE_H;
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                type = SIM_NETWORK_TYPE_4G;
                break;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                type = SIM_NETWORK_TYPE_UNKNOWN;
                break;
            default:
                type = SIM_NETWORK_TYPE_UNKNOWN;
                break;
        }
        mStatusBar.setSimStatus(isExist, type, mSimSignalLevel);
        XLog.d("Snser", "onSimStateChanged isExist=" + isExist + " type=" + type + " level=" + mSimSignalLevel);
    }

    private void onSdcardStateChanged() {
        int mountedSdcard = 0;
        try {
            Method methodGetVolumePaths = mStorageManager.getClass().getMethod("getVolumePaths");
            Method methodGetVolumeState = mStorageManager.getClass().getMethod("getVolumeState", String.class);
            String[] paths = (String[])methodGetVolumePaths.invoke(mStorageManager);
            if (paths != null && paths.length > 0) {
                for (String path : paths) {
                    try {
                        String state = (String)methodGetVolumeState.invoke(mStorageManager, path);
                        mountedSdcard += (Environment.MEDIA_MOUNTED.equals(state) ? 1 : 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mStatusBar.setSdcardStatus(mountedSdcard > 1);
        XLog.d("Snser", "onSdcardStateChanged isExist=" + (mountedSdcard > 1));
    }
    
    private void onMuteStateChanged() {
        boolean isEnabled = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
        mStatusBar.setMuteStatus(isEnabled);
        XLog.d("Snser", "onMuteStateChanged isEnabled=" + isEnabled);
    }
    
}
