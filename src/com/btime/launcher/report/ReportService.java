package com.btime.launcher.report;

import com.btime.launcher.report.gps.ReportGPS;
import com.btime.launcher.util.XLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ReportService extends Service {
    
    private ReportServiceBinder mBinder = new ReportServiceBinder();
    
    private ReportEventReceiver mReportEventReceiver;
    
    public class ReportServiceBinder extends Binder {
        public void onReportEvent(Context context, Intent intent) {
            ReportService.this.onReportEvent(context, intent);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        XLog.d(Constants.TAG, "ReportService onCreate");
        init(this.getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        XLog.d(Constants.TAG, "ReportService onStartCommand");
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        XLog.d(Constants.TAG, "ReportService onDestroy");
        unInit(this.getApplicationContext());
    }
    
    private void init(Context context) {
        registerReceiver(context);
        ReportGPS.getInstance().init(context);
    }
    
    private void unInit(Context context) {
        unregisterReceiver(context);
        ReportGPS.getInstance().unInit();
    }
    
    private void registerReceiver(Context context) {
        context.registerReceiver(mReportEventReceiver = new ReportEventReceiver(), 
                                 ReportEventReceiver.INTENT_FILTER, 
                                 //注意：这里加上Permission之后，收Sticky广播可能会受影响，因为它是实际发送者的签名
                                 ReportEventReceiver.PERMISSION,
                                 null);
    }
    
    private void unregisterReceiver(Context context) {
        if (mReportEventReceiver != null) {
            context.unregisterReceiver(mReportEventReceiver);
            mReportEventReceiver = null;
        }
    }
    
    private void onAccStatusOn() {
        XLog.d("Snser", "onAccStatusOn");
        ReportGPS.getInstance().onAccStateOn();
        //ToastUtils.showMessage(this, "onAccStateOn");
    }
    
    private void onAccStatusOff() {
        XLog.d("Snser", "onAccStatusOff");
        ReportGPS.getInstance().onAccStateOff();
        //ToastUtils.showMessage(this, "onAccStateOff");
    }
    
    private void onReportEvent(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Constants.ACTION_REPORT_POLICY.equals(action)) {
            int policy = intent.getIntExtra(Constants.EXTRA_REPORT_POLICY_GPS, -1);
            if (policy == Constants.REPORT_POLICY_GPS_ON) {
                ReportGPS.getInstance().setReportGPSEnabled(this.getApplicationContext(), true);
            } else if (policy == Constants.REPORT_POLICY_GPS_OFF) {
                ReportGPS.getInstance().setReportGPSEnabled(this.getApplicationContext(), false);
            }
        }
    }
    
}
