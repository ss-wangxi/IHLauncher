package com.btime.launcher.report.gps;

import java.util.Timer;

import cc.snser.launcher.style.SettingPreferences;

import com.btime.launcher.report.Constants;
import com.btime.launcher.util.XLog;

import android.content.Context;

/**
 * 上报GPS信息
 * @author snsermail@gmail.com
 *
 */

public class ReportGPS {
    
    private Timer mTimerGpsRecord;
    private Timer mTimerGpsReport;
    
    private JsonGPSRecord mGpsRecord = new JsonGPSRecord();
    
    private boolean mIsEnabled = getReportGPSEnabled();
    
    private ReportGPS() {
    }
    
    private static class SingletonHolder {
        private static ReportGPS sINSTANCE = new ReportGPS();
    }
    
    public static ReportGPS getInstance() {
        return SingletonHolder.sINSTANCE;
    }
    
    public void init(Context context) {
        XLog.d(Constants.TAG, "init enabled=" + mIsEnabled);
        if (mIsEnabled) {
            GPSListenser.getInstance().init(context);
            startReport(true);
        }
    }
    
    public void unInit() {
        XLog.d(Constants.TAG, "unInit enabled=" + mIsEnabled);
        GPSListenser.getInstance().unInit();
        stopReport();
        stopRecord();
    }
    
    public boolean getReportGPSEnabled() {
        return SettingPreferences.getCommonPreferences().getReportGPSEnabled();
    }
    
    public void setReportGPSEnabled(Context context, boolean isEnabled) {
        XLog.d(Constants.TAG, "setReportGPSEnabled mIsEnabled=" + mIsEnabled + " newEnabled=" + isEnabled);
        if (mIsEnabled != isEnabled) {
            mIsEnabled = isEnabled;
            SettingPreferences.getCommonPreferences().setReportGPSEnabled(isEnabled);
            if (mIsEnabled) {
                init(context);
            } else {
                unInit();
            }
        }
    }
    
    public void onAccStateOn() {
        XLog.d(Constants.TAG, "onAccStateOn enabled=" + mIsEnabled);
        if (mIsEnabled) {
            new GpsAccStateOnTask().execute();
        }
    }
    
    public void onAccStateOff() {
        XLog.d(Constants.TAG, "onAccStatusOff enabled=" + mIsEnabled);
        if (mIsEnabled) {
            stopRecord();
            new GpsAccStateOffTask().execute();
        }
    }
    
    public JsonGPSRecord getGPSRecord() {
        return mGpsRecord;
    }
    
    public void startRecord() {
        if (mTimerGpsRecord == null) {
            mTimerGpsRecord = new Timer(true);
            mTimerGpsRecord.schedule(new GpsRecordTask(), Constants.INTERVAL_RECORD_MILLS, Constants.INTERVAL_RECORD_MILLS);
        }
    }
    
    public void stopRecord() {
        if (mTimerGpsRecord != null) {
            mTimerGpsRecord.cancel();
            mTimerGpsRecord = null;
        }
    }
    
    public void startReport(boolean isDelay) {
        if (mTimerGpsReport == null) {
            final int delayMills = isDelay ? Constants.INTERVAL_REPORT_MILLS : 0;
            mTimerGpsReport = new Timer(true);
            mTimerGpsReport.schedule(new GpsReportTask(), delayMills, Constants.INTERVAL_REPORT_MILLS);
        }
    }
    
    public void stopReport() {
        if (mTimerGpsReport != null) {
            mTimerGpsReport.cancel();
            mTimerGpsReport = null;
        }
    }
    
}
