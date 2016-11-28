package com.btime.launcher.report;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 接收打点事件的广播
 * @author snsermail@gmail.com
 *
 */
public class ReportEventReceiver extends BroadcastReceiver {
    
    public static final IntentFilter INTENT_FILTER = new IntentFilter();
    public static final String PERMISSION = "com.caros.launcher.permission.BROADCAST_REPORT_EVENT";
    
    static {
        INTENT_FILTER.addAction(Constants.ACTION_REPORT_POLICY);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ReportServiceWrapper.getInstance().onReportEvent(context, intent, true);
    }

}
