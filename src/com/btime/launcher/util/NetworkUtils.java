package com.btime.launcher.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtils {
    
    /**
     * 判断是否无网络连接(必须在initDataManager之后调用)
     * @return true:当前无网络连接 false:当前可能有网络连接
     */
    public static boolean isNetworkDisconnected(Context context){
        try {
            ConnectivityManager connectManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();
            return networkInfo == null || !networkInfo.isAvailable();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static String httpGetData(String url, int timeout, String cookie){
        String response = null;
        
        try {
            BasicHttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
            HttpConnectionParams.setSoTimeout(httpParams, timeout);
            HttpClient httpClient = new DefaultHttpClient(httpParams);
            HttpGet httpGet = new HttpGet(url);
            if (cookie != null){
                httpGet.setHeader("Cookie", cookie);
            }
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder responseData = new StringBuilder();
            String line = "";
            while ((line = bufReader.readLine()) != null){
                responseData.append(line);
            }
            response = responseData.toString();
        } catch (Exception e) {
            response = e.getMessage();
            e.printStackTrace();
            return null;
        }
        
        return response;
    }
    
    public static String httpPostData(String url, List<NameValuePair> params, int timeout, String cookie) {
        byte[] data = null;
        if (params != null && params.size() > 0){
            try {
                data = asByteArray(new UrlEncodedFormEntity(params, "UTF-8").getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return httpPostData(url, data, null, timeout, cookie);
    }
    
    public static String httpPostData(String url, String str, int timeout, String cookie) {
        byte[] data = null;
        if (str != null){
            try {
                data = asByteArray(new StringEntity(str, "UTF-8").getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return httpPostData(url, data, null, timeout, cookie);
    }
    
    public static String httpPostData(String url, byte[] data, String contentType, int timeout, String cookie) {
        String response = null;
        
        try {
            BasicHttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
            HttpConnectionParams.setSoTimeout(httpParams, timeout);
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            if (data != null) {
                ByteArrayEntity entity = new ByteArrayEntity(data);
                entity.setContentType(contentType != null ? contentType : "application/x-www-form-urlencoded");
                httpPost.setEntity(entity);
            }
            if (cookie != null){
                httpPost.setHeader("Cookie", cookie);
            }
            
            HttpResponse httpResponse = httpClient.execute(httpPost);
            InputStream inputStream = httpResponse.getEntity().getContent();
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder responseData = new StringBuilder();
            String line = "";
            while ((line = bufReader.readLine()) != null){
                responseData.append(line);
            }
            response = responseData.toString();
        } catch (Exception e) {
            response = e.getMessage();
            e.printStackTrace();
        }
        
        return response;
    }
    
    public static byte[] asByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = stream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        stream.close();
        return outStream.toByteArray();
    }
}
