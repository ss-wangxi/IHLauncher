package com.btime.launcher.report.gps;

import java.util.TimerTask;

import com.btime.launcher.report.Constants;

/**
 * GPS记录任务
 * @author snsermail@gmail.com
 *
 */
public class GpsRecordTask extends TimerTask {
    
    @Override
    public void run() {
        final JsonGPSRecord record = ReportGPS.getInstance().getGPSRecord();
        record.addPositionAuto();
        if (record.getCount() == Constants.RECORD_PER_REPORT_LOCAL) {
            record.toFile();
            record.clear().setType(Constants.RECORD_TYPE_MOVE);
        }
    }
    
}
