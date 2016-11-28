package com.btime.launcher.report.gps;

import java.util.Locale;

import com.btime.launcher.report.Constants;
import com.btime.launcher.util.XLog;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * gps位置监听器
 * @author snsermail@gmail.com
 *
 */
public class GPSListenser {
    private LocationManager mLocationMgr = null;
    private InternalGPSListener mListenser = null;
    
    private double mLastLongitude = 0.0;
    private double mLastLatitude = 0.0;
    
    private GPSListenser() {
    }
    
    private static class SingletonHolder {
        private static GPSListenser sINSTANCE = new GPSListenser();
    }
    
    public static GPSListenser getInstance() {
        return SingletonHolder.sINSTANCE;
    }
    
    public void init(Context context) {
        mListenser = new InternalGPSListener();
        mLocationMgr = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        if (mListenser != null && mLocationMgr != null) {
            try {
                mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
                        Constants.MIN_DIFF_TIME_MILLS, 
                        Constants.MIN_DIFF_DISTANCE_METERS, 
                        mListenser);
                mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
                                        Constants.MIN_DIFF_TIME_MILLS, 
                                        Constants.MIN_DIFF_DISTANCE_METERS, 
                                        mListenser);
                XLog.d(Constants.TAG, "GPSListenser init succ");
            } catch (Exception e) {
                XLog.d(Constants.TAG, "GPSListenser init fail");
                e.printStackTrace();
            }
        }
    }
    
    public void unInit() {
        if (mListenser != null && mLocationMgr != null) {
            mLocationMgr.removeUpdates(mListenser);
            mListenser = null;
            mLocationMgr = null;
            mLastLongitude = 0.0;
            mLastLatitude = 0.0;
        }
    }
    
    private class InternalGPSListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            //ToastUtils.showMessage(App.getAppContext(), "onLocationChanged " + location.getProvider());
            XLog.d(Constants.TAG, "onLocationChanged lon=" + location.getLongitude() + " lat=" + location.getLatitude() + " src=" + location.getProvider());
            GPSTimeManager.handleAdjustSystemTime(location);
            mLastLongitude = location.getLongitude();
            mLastLatitude = location.getLatitude();
            //上述获取的是原始GPS坐标，转成百度地图坐标的接口如下(0:原始坐标 2:Google地图坐标 4:百度地图坐标)：
            //http://api.map.baidu.com/ag/coord/convert?from=0&to=4&x=longitude&y=latitude
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
    
    /**
     * 获取最新的经度(精确到0.000001)
     * @return 经度
     */
    public String getLastLongitude() {
        //mLastLongitude = 116.484083f + (new Random().nextFloat() - 0.5f) * 0.001f;
        return String.format(Locale.getDefault(), "%.6f", mLastLongitude);
    }
    
    /**
     * 获取最新的纬度(精确到0.000001)
     * @return 纬度
     */
    public String getLastLatitude() {
        //mLastLatitude = 39.981230f + (new Random().nextFloat() - 0.5f) * 0.001f;
        return String.format(Locale.getDefault(), "%.6f", mLastLatitude);
    }
    
}
