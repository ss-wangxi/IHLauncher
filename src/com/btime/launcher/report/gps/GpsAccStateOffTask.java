package com.btime.launcher.report.gps;

import com.btime.launcher.report.Constants;

import android.os.AsyncTask;

public class GpsAccStateOffTask extends AsyncTask<Object, Object, Object> {

    @Override
    protected Object doInBackground(Object... params) {
        final JsonGPSRecord record = ReportGPS.getInstance().getGPSRecord();
        if (record.getCount() != 0) {
            record.toFile();
        }
        record.clear().setType(Constants.RECORD_TYPE_STOP);
        record.addPositionAuto();
        record.toFile();
        return null;
    }
    
    @Override
    protected void onPostExecute(Object result) {
        ReportGPS.getInstance().stopReport();
        ReportGPS.getInstance().startReport(false);
    }

}
