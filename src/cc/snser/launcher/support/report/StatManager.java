package cc.snser.launcher.support.report;

import android.R.integer;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import cc.snser.launcher.App;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.util.PackageUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

public class StatManager {
    private static final String TAG = "Launcher.StatManager";
    private static StatManager mInstance = new StatManager();
    private static final int LOG_FILES_LIMITATION = 5;
    private static Map<String, Long> mLastStatSendTime = new HashMap<String, Long>();
    
    private static final int LOG_FILES_COUNT_LIMITATION = 5;
    
    private static final long LOG_FILES_SIZE_LIMITATION = 2000000;
    
    private static final String PACKAGE = App.getApp().getApplicationContext().getPackageName();
    
    private static File sFileDirectory;
    static { 
    	File sdCard = null;
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			sdCard = Environment.getExternalStorageDirectory(); 
		}
		else {
			sdCard = App.getApp().getCacheDir();
		}
		
		sFileDirectory = new File(sdCard.getAbsolutePath() + "/Android/data/" + PACKAGE + "/crash");
		if(!sFileDirectory.exists()){
			sFileDirectory.mkdirs();
		}
    }	
	public static String mUninstallPkgName = null;

    public static StatManager getInstance(){
    	return mInstance;
    }
    
    public static void handleException(Context context, Throwable throwable) {
    	if(throwable != null){
    		throwable.printStackTrace();
    	}
    	
        if (throwable != null)
        	saveLog2Storage(context, throwable);
    }
    
    private static void saveLog2Storage(Context context, Throwable throwable){
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    	Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		throwable.printStackTrace(printWriter);

		Throwable cause = throwable.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		
		printWriter.close();
		
		String strPackageVersion = String.valueOf(PackageUtils.getPackageVersionName(context, context.getPackageName()));
		String strSignTime = PackageUtils.getPackageSignTime(context); 
		if(strPackageVersion != null && !TextUtils.isEmpty(strSignTime)){
			strPackageVersion += "_";
			strPackageVersion += strSignTime;
		}
		

		XLog.e(TAG,"======================================================");
		XLog.e(TAG,writer.toString());
		XLog.e(TAG,"======================================================");

		try {
			String fileName = "crash-" + formatter.format(new Date()) + ".log";			
			File crashlog = new File(sFileDirectory, fileName);
			FileOutputStream os = FileUtils.openOutputStream(crashlog, false);
			if(os != null){
				os.write(writer.toString().getBytes());
				os.close();
			}

			if (LOGD_ENABLED) {
                XLog.d(TAG, fileName + " is successfully saved to " + crashlog.toString());
            }
			
		} catch (Exception e) {
			e.printStackTrace();
			
			if (LOGD_ENABLED) {
                XLog.w(TAG, "Exception happened while writing string to file.");
            }
		}
    }
	public static final String STAT_HAS_SEND_MOBILEINFO = "stat_has_send_mobileinfo";
	
}
