package com.btime.launcher.report.gps;

import com.btime.launcher.report.Constants;

import android.os.AsyncTask;

public class GpsAccStateOnTask extends AsyncTask<Object, Object, Object> {

    @Override
    protected Object doInBackground(Object... params) {
        final JsonGPSRecord record = ReportGPS.getInstance().getGPSRecord();
        record.clear().init().setType(Constants.RECORD_TYPE_START);
        record.addPositionAuto();
        record.toFile();
        record.clear().setType(Constants.RECORD_TYPE_MOVE);
        return null;
    }
    
    @Override
    protected void onPostExecute(Object result) {
        ReportGPS.getInstance().stopRecord();
        ReportGPS.getInstance().startRecord();
    }

}
