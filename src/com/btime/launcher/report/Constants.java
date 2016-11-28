package com.btime.launcher.report;

import java.io.File;

import cc.snser.launcher.App;

public class Constants {
    
    public static final String TAG = "Report";
    
    public static final int RECORD_TYPE_MOVE = 0;
    public static final int RECORD_TYPE_START = 1;
    public static final int RECORD_TYPE_STOP = 2;
    
    public static final int MIN_DIFF_TIME_MILLS = 2000;
    public static final float MIN_DIFF_DISTANCE_METERS = 0.0f;
    
    public static final int INTERVAL_RECORD_MILLS = 5 * 1000; //每隔INTERVAL_RECORD_MILLS记录一次gps
    public static final int RECORD_PER_REPORT_LOCAL = 10; //本地每个报文json文件包含的gps记录个数
    public static final int RECORD_PER_REPORT_POLICY = 60; //产品策略约定触发上传逻辑的gps记录个数
    public static final int RECORD_PER_REPORT_API = 120; //服务器接口约定每个报文最多支持的gps记录个数
    public static final int INTERVAL_REPORT_MILLS = INTERVAL_RECORD_MILLS * (RECORD_PER_REPORT_POLICY + 1); //每隔INTERVAL_REPORT_MILLS触发一次上报任务
    
    public static final int ACC_STATUS_ON = 1;
    public static final int ACC_STATUS_OFF = 0;
    
    public static final String RECORD_SAVE_PATH = App.getAppContext().getFilesDir() + File.separator + "record";
    
    public static final String URL_REPORT_GPS = "http://36.110.210.166/api/mobileos/put_track";
    public static final int REPORT_GPS_TIMEOUT_MILLS = 30 * 1000;
    
    public static final String ACTION_REPORT_POLICY = "com.caros.launcher.action.REPORT_POLICY";
    public static final String EXTRA_REPORT_POLICY_GPS = "com.caros.launcher.extra.REPORT_POLICY_GPS";
    public static final int REPORT_POLICY_GPS_ON = 0x101;
    public static final int REPORT_POLICY_GPS_OFF = 0x102;
    
}
