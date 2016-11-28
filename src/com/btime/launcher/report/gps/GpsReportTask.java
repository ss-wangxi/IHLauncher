package com.btime.launcher.report.gps;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimerTask;

import cc.snser.launcher.App;

import com.btime.launcher.report.Constants;
import com.btime.launcher.util.NetworkUtils;
import com.btime.launcher.util.XLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * GPS上报任务
 * @author snsermail@gmail.com
 *
 */
public class GpsReportTask extends TimerTask {

    @Override
    public void run() {
        final File[] files = new File(Constants.RECORD_SAVE_PATH).listFiles();
        final boolean isNetworkDisconnected = NetworkUtils.isNetworkDisconnected(App.getAppContext());
        
        XLog.d(Constants.TAG, "GpsReportTask.run files.count=" + (files != null ? files.length : -1) + " isNetworkDisconnected=" + isNetworkDisconnected);
        
        if (files != null && files.length > 0 && !isNetworkDisconnected) {
            //按文件名排序，listFiles方法不保证结果按名称有序
            final List<File> records = Arrays.asList(files);
            Collections.sort(records, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    return lhs.getName().compareTo(rhs.getName());
                }
            });
            
            final JsonGPSRecord jsonMoveRecord = new JsonGPSRecord().setType(Constants.RECORD_TYPE_MOVE);
            for (File record : records) {
                JsonGPSRecord newJsonRecord = JsonGPSRecord.fromFile(record.getAbsolutePath());
                if (newJsonRecord != null) {
                    int type = newJsonRecord.type;
                    if (type == Constants.RECORD_TYPE_START) {
                        //此处是ACC_ON的日志，立刻上传
                        if (report(newJsonRecord)) {
                            cleanReportFiles(newJsonRecord);
                        }
                    } else if (type == Constants.RECORD_TYPE_STOP) {
                        //此处是ACC_OFF的日志，先上传遗留的MOVE日志
                        if (report(jsonMoveRecord)) {
                            cleanReportFiles(jsonMoveRecord);
                        }
                        jsonMoveRecord.unInit().setType(Constants.RECORD_TYPE_MOVE);
                        //此处是ACC_OFF的日志，立刻上传
                        if (report(newJsonRecord)) {
                            cleanReportFiles(newJsonRecord);
                        }
                    } else if (type == Constants.RECORD_TYPE_MOVE) {
                        //此处是MOVE的日志，如果和上一个MOVE不是一个轨迹，或者之前已经攒够服务端接口约定的数据量，则立刻上传之前攒下的MOVE日志
                        if (!jsonMoveRecord.isSameRoute(newJsonRecord) 
                            || jsonMoveRecord.getCount() + newJsonRecord.getCount() > Constants.RECORD_PER_REPORT_API) {
                            if (report(jsonMoveRecord)) {
                                cleanReportFiles(jsonMoveRecord);
                            }
                            jsonMoveRecord.unInit().setType(Constants.RECORD_TYPE_MOVE);
                        }
                        //此处是MOVE的日志，暂时不需要上传，将其merge到jsonMoveRecord中
                        jsonMoveRecord.merge(newJsonRecord);
                    }
                }
            }
            
            //如果MOVE的日志达到产品策略约定的触发值(且没到服务端接口约定的数据量)，就上传一次
            if (jsonMoveRecord.getCount() >= Constants.RECORD_PER_REPORT_POLICY) {
                if (report(jsonMoveRecord)) {
                    cleanReportFiles(jsonMoveRecord);
                }
                jsonMoveRecord.unInit().setType(Constants.RECORD_TYPE_MOVE);
            }
        }
    }
    
    private static class JsonResponce {
        public int code;
        //public String msg;
        
        public static boolean checkSucc(String responce) {
            boolean succ = false;
            Gson gson = new GsonBuilder().serializeNulls().create();
            try {
                succ = gson.fromJson(responce, JsonResponce.class).code == 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return succ;
        }
    }
    
    private boolean report(final JsonGPSRecord record) {
        //NetworkUtils.setHttpEncryptEnabled(false);
        String info = record.commit().toJsonString();
        
        boolean isSucc = false;
        XLog.d(Constants.TAG, "GpsReportTask.report n=" + record.n + " ctime=" + record.ctime + " type=" + record.type + " count=" + record.getCount() + " start");
        if (record.getCount() > 0) {
            String responce = NetworkUtils.httpPostData(Constants.URL_REPORT_GPS, info, Constants.REPORT_GPS_TIMEOUT_MILLS, null);
            isSucc = JsonResponce.checkSucc(responce);
        }
        XLog.d(Constants.TAG, "GpsReportTask.report n=" + record.n + " ctime=" + record.ctime + " type=" + record.type + " count=" + record.getCount() + " succ=" + isSucc);
        return isSucc;
    }
    
    private void cleanReportFiles(JsonGPSRecord record) {
        for (String filepath : record.filelist) {
            new File(filepath).delete();
        }
    }

}
