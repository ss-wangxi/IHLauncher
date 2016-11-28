package com.btime.launcher.report.gps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.text.TextUtils;
import cc.snser.launcher.App;
import cc.snser.launcher.util.PackageUtils;

import com.btime.launcher.report.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shouxinzm.launcher.util.DeviceInfoUtils;

public class JsonGPSRecord {
    
    private static final String sDeviceId =  DeviceInfoUtils.getVerifyId(App.getAppContext());
    private static final String sVersionName = PackageUtils.getSelfVersionName(App.getAppContext());
    
    static {
        new File(Constants.RECORD_SAVE_PATH).mkdirs();
    }
    
    public String deviceid; //设备唯一号
    public String ver; //客户端版本号
    public int type; //类型 0:实时位置 1:启动位置  2:熄火位置
    public String position;  //GPS位置  [[23.123456,113.654321],[25.894894,118.750595]] 
    public long ctime; //position最后一个GPS点生成时刻的本地时间戳
    public long stime; //日志上报时刻的本地时间戳
    public int n; //随机数 每次启动到熄火用一个随机数
    public int span; //type=0时，相邻两点之间的时间间隔
    
    public transient List<List<String>> posInternal = new ArrayList<List<String>>(); //position对应的实际List
    public transient List<String> filelist = new ArrayList<String>(); //数据所对应的缓存json文件列表
    
    public JsonGPSRecord() {
        this.deviceid = sDeviceId;
        this.ver = sVersionName;
    }
    
    public JsonGPSRecord init() {
        synchronized (this) {
            this.n = (int)(System.currentTimeMillis() & Integer.MAX_VALUE);
            this.span = Constants.INTERVAL_RECORD_MILLS;
        }
        return this;
    }
    
    public JsonGPSRecord unInit() {
        setN(0);
        setType(0);
        setSpan(0);
        clear();
        return this;
    }
    
    public JsonGPSRecord setType(int type) {
        synchronized (this) {
            this.type = type;
        }
        return this;
    }
    
    public JsonGPSRecord setN(int n) {
        synchronized (this) {
            this.n = n;
        }
        return this;
    }
    
    public JsonGPSRecord setSpan(int spanMills) {
        synchronized (this) {
            this.span = spanMills;
        }
        return this;
    }
    
    public JsonGPSRecord clear() {
        synchronized (this) {
            this.position = null;
            this.posInternal.clear();
            this.ctime = 0;
            this.stime = 0;
            this.filelist.clear();
        }
        return this;
    }
    
    public void addPositionAuto() {
        String latitude = GPSListenser.getInstance().getLastLatitude();
        String longitude = GPSListenser.getInstance().getLastLongitude();
        if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {
            addPosition(longitude, latitude);
        }
    }
    
    public void addPosition(String longitude, String latitude) {
        synchronized (this) {
            this.posInternal.add(Arrays.asList(longitude, latitude));
            this.ctime = System.currentTimeMillis();
        }
    }
    
    public int getCount() {
        return this.posInternal.size();
    }
    
    public void toFile() {
        commit();
        String filepath = String.format(Locale.getDefault(), "%s%s%015d_%015d_%d.json", Constants.RECORD_SAVE_PATH, File.separator, this.n, this.ctime, this.type);
        try {
            File file = new File(filepath);
            file.createNewFile();
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
            Gson gson = new GsonBuilder().serializeNulls().create();
            gson.toJson(this, JsonGPSRecord.class, osw);
            osw.flush();
            osw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static JsonGPSRecord fromFile(String filepath) {
        JsonGPSRecord record = null;
        File file = new File(filepath);
        Gson gson = new GsonBuilder().serializeNulls().create();
        if (file.exists() && file.canRead()) {
            try {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
                record = gson.fromJson(isr, JsonGPSRecord.class);
                if (record != null) {
                    record.posInternal = gson.fromJson(record.position, List.class);
                    record.filelist.add(filepath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return record;
    }
    
    public boolean isSameRoute(JsonGPSRecord record) {
        return record != null && (this.n == 0 || record.n == 0 || this.n == record.n);
    }
    
    public JsonGPSRecord merge(JsonGPSRecord record) {
        if (isSameRoute(record) && this.type == record.type) {
            synchronized (this) {
                this.posInternal.addAll(record.posInternal);
                this.filelist.addAll(record.filelist);
                this.ctime = record.ctime;
                this.n = this.n != 0 ? this.n : record.n;
            }
        }
        return this;
    }
    
    public JsonGPSRecord commit() {
        synchronized (this) {
            this.position = this.posInternal.toString();
            this.stime = System.currentTimeMillis();
        }
        return this;
    }
    
    public String toJsonString() {
        String jsonString = null;
        try {
            Gson gson = new GsonBuilder().serializeNulls().create();
            jsonString = gson.toJson(this, JsonGPSRecord.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonString;
    }
    
}
