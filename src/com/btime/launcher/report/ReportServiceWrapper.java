package com.btime.launcher.report;

import java.util.ArrayList;

import cc.snser.launcher.App;

import com.btime.launcher.report.ReportService.ReportServiceBinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

public class ReportServiceWrapper {
    
    private static final int SERVICE_STATE_CONNECTING = 0;
    private static final int SERVICE_STATE_CONNECTED = 1;
    private static final int SERVICE_STATE_DISCONNECTED = 2;
    
    private ReportServiceBinder mService;
    private int mServiceState = SERVICE_STATE_DISCONNECTED;
    
    private Handler mHandler = new Handler();
    private ArrayList<Runnable> mPendingTasks = new ArrayList<Runnable>();

    private ReportServiceWrapper() {
    }
    
    private static class SingletonHolder {
        public static ReportServiceWrapper sInstance = new ReportServiceWrapper();
    }
    
    public static ReportServiceWrapper getInstance() {
        return SingletonHolder.sInstance;
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof ReportServiceBinder) {
                mService = (ReportServiceBinder)service;
                mServiceState = SERVICE_STATE_CONNECTED;
                runPendingTasks();
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mServiceState = SERVICE_STATE_DISCONNECTED;
        }
    };
    
    private void bindService() {
        Context context = App.getAppContext();
        Intent intent = new Intent(context, ReportService.class);
        boolean ret = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mServiceState = ret ? SERVICE_STATE_CONNECTING : SERVICE_STATE_DISCONNECTED;
    }
    
    private ReportServiceBinder getServiceAutoBind() {
        if (mService != null) {
            return mService;
        } else {
            bindService();
            return null;
        }
    }
    
    private void runPendingTasks() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mPendingTasks) {
                    for (Runnable task : mPendingTasks) {
                        task.run();
                    }
                    mPendingTasks.clear();
                }
            }
        });
    }
    
    public boolean onReportEvent(final Context context, final Intent intent, final boolean bWait) {
        final ReportServiceBinder service = getServiceAutoBind();
        if (service != null) {
            try {
                service.onReportEvent(context, intent);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        onReportEvent(context, intent, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
}
