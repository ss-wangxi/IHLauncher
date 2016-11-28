package cc.snser.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.ui.utils.IconMetrics;
import cc.snser.launcher.ui.utils.PrefConstants;
import cc.snser.launcher.ui.utils.PrefUtils;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.FileUtils;
import com.shouxinzm.launcher.util.IOUtils;
import com.shouxinzm.launcher.util.PathUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;

/**
 * icon fast cache 将图标缓存在SD卡上，快速读取（比从PackManager中快）
 * @author huangninghai
 *
 */
public class IconFsCache {

    public static final String TAG = "Launcher.Model.IconFsCache";

    private static final String CACHE_DATA_FILE_NAME = ".data";

    private static IconFsCache mInstance = null;
    private static IconFsCache mInstanceSecondLayer = null;

    private boolean mCacheExpired = true;

    private final Context mContext;

    private static Object mSync = new Object();

    public static IconFsCache getInstance(Context context) {
        synchronized (mSync) {
            if (mInstance == null) {
                mInstance = new IconFsCache(context.getApplicationContext());
            }
        }
        return mInstance;
    }
    
    public static IconFsCache getInstanceSecondLayer(Context context) {
        synchronized (mSync) {
            if (mInstanceSecondLayer == null) {
            	mInstanceSecondLayer = new IconFsCache(context.getApplicationContext());
            }
        }
        return mInstanceSecondLayer;
    }

    private IconFsCache(Context context) {
        this.mContext = context;
        mCacheExpired = PrefUtils.getBooleanPref(context, PrefConstants.KEY_FS_CACHE_EXPIRED, true);
    }

    /**
     * 禁用此cache，禁止尝试从文件系统中读取到icon的缓存
     */
    public void expireCache() {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "expireCache begins");
        }
        mCacheExpired = true;
        PrefUtils.setBooleanPref(mContext, PrefConstants.KEY_FS_CACHE_EXPIRED, mCacheExpired);
        deleteFolderBg();
    }

    /**
     *启用此cache，可以尝试从文件系统中读取到icon的缓存
     */
    public void enableCache() {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "enableCache begins");
        }
        mCacheExpired = false;
        PrefUtils.setBooleanPref(mContext, PrefConstants.KEY_FS_CACHE_EXPIRED, mCacheExpired);
    }

    public Bitmap getIconFromCacheFile(Context context, ComponentName cn) {
        if (!Utils.isExternalStorageReadable() || !Utils.isExternalStorageWritable()) {
            return null;
        }

        if (mCacheExpired) {
            return null;
        }

        File file = new File(getIconCacheFilePath(cn));
        if (!file.exists()) {
            return null;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (WorkspaceIconUtils.isInvalidIconSize(options.outWidth, options.outHeight)) {
                if (LOGE_ENABLED) {
                    XLog.e(TAG, "decode icon cache " + cn + " file dimension error:" + options.outWidth + "," + options.outHeight);
                }
                FileUtils.deleteQuietly(file);
                return null;
            } else {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                int iconSize = IconMetrics.getIconWidth(mContext);
                if (iconSize != bitmap.getWidth()) {
                    if (LOGE_ENABLED) {
                        XLog.e(TAG, "icon size changed. from " + bitmap.getWidth() + " to " + iconSize);
                    }
                    FileUtils.deleteQuietly(file);
                    return null;
                } else {
                    return bitmap;
                }
            }
        } catch (Throwable e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "getIconFromCacheFile failed", e);
            }
        }

        return null;
    }
    
    public boolean saveIconToCacheFile(ComponentName componentName,Bitmap bitmap){
    	if (!Utils.isExternalStorageReadable() || !Utils.isExternalStorageWritable()) {
            return false;
        }
    	FileOutputStream fos = null;
    	try {
    		String filePath = getIconCacheFilePath(componentName);
    		fos = new FileOutputStream(filePath,true);
    		bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
    		fos.flush();
    		return true;
		} catch (Exception e) {
		}finally{
			 IOUtils.closeQuietly(fos);
		}
    	
    	return false;
    }
    
    public Bitmap getIconFromCacheNoExpire(ComponentName componentName){
    	  if (!Utils.isExternalStorageReadable() || !Utils.isExternalStorageWritable()) {
              return null;
          }

          File file = new File(getIconCacheFilePath(componentName));
          if (!file.exists()) {
              return null;
          }

          try {
              BitmapFactory.Options options = new BitmapFactory.Options();
              options.inJustDecodeBounds = true;
              BitmapFactory.decodeFile(file.getAbsolutePath(), options);
              Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
              return bitmap;
          } catch (Throwable e) {
              if (LOGE_ENABLED) {
                  XLog.e(TAG, "getIconFromCacheFile failed", e);
              }
          }

          return null;
    }
    
    public boolean isIconExist(ComponentName componentName){
    	  if (!Utils.isExternalStorageReadable() || !Utils.isExternalStorageWritable()) {
    		  return false;
    	  }
    	  
    	  File file = new File(getIconCacheFilePath(componentName));
    	  return file.exists();
    }
    
    public boolean saveIconsToCacheFile(List<? extends AppInfo> infos) {
        long t = 0;
        if (LOGD_ENABLED) {
            XLog.d(TAG, "saveIconsToCacheFile begin");
            t = System.currentTimeMillis();
        }
        if (!Utils.isExternalStorageReadable() || !Utils.isExternalStorageWritable()) {
            return false;
        }

        Map<String, long[]> cacheFileDataMap = readCacheData();

        try {
            File parentFile = new File(getIconCacheFolderPath());
            int saveNewFileNum = 0;
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }

            long[] times = null;
            for (AppInfo info : infos) {
                String fileName = getCacheFileName(info.getIntent().getComponent());
                times = cacheFileDataMap.get(fileName);
                if (times == null) {
                    times = new long[] {-1L, -1L};
                    cacheFileDataMap.put(fileName, times);
                }
                String filePath = getIconCacheFilePath(info.getIntent().getComponent());
                File cacheFile = new File(filePath);
                if (info.getStorage() == Constant.APPLICATION_EXTERNAL || mCacheExpired || info.getLastUpdateTime() != times[0] || cacheFile.lastModified() != times[1]) {
                    times[0] = -1;
                    times[1] = -1;

                    if (info.getStorage() == Constant.APPLICATION_EXTERNAL) {
                        FileUtils.deleteQuietly(cacheFile);

                        if (LOGE_ENABLED) {
                            XLog.e(TAG, "default icon " + info.getIntent().getComponent() + " is external and ignored");
                        }
                    } else {
                        FileOutputStream fos = null;
                        try {
                            Bitmap icon = info.getIcon().getBitmap();

                            if (IconCache.getInstance(mContext).isDefaultIcon(icon)) {
                                FileUtils.deleteQuietly(cacheFile);

                                if (LOGE_ENABLED) {
                                    XLog.e(TAG, "default icon " + info.getIntent().getComponent() + " is is default icon and ignored");
                                }
                            } else {
                                fos = new FileOutputStream(filePath);
                                icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                fos.flush();

                                times[0] = info.getLastUpdateTime();
                                times[1] = cacheFile.lastModified();
                            }
                        } catch (Throwable e) {
                            FileUtils.deleteQuietly(cacheFile);

                            if (LOGE_ENABLED) {
                                XLog.e(TAG, "save icon cache " + info.getIntent().getComponent() + " file failed", e);
                            }
                        } finally {
                            IOUtils.closeQuietly(fos);
                        }
                    }

                    saveNewFileNum++;
                }
            }
            if (LOGD_ENABLED) {
                XLog.d(TAG, "save file num:" + saveNewFileNum);
            }
            if (saveNewFileNum > 0) {
                writeDataFile(cacheFileDataMap);
            }
            return true;
        } catch (Throwable e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "saveBitmapToIconCacheFile failed", e);
            }

            return false;
        } finally {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "saveIconsToCacheFile ends and took " + (System.currentTimeMillis() - t) + " ms");
            }
        }
    }

    public void deleteCacheFiles(List<? extends AppInfo> infos) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "deleteCacheFiles begin");
        }
        for (AppInfo info : infos) {
            deleteCacheFile(info);
        }
    }

    public void deleteCacheFile(AppInfo info) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "deleteCacheFile begin");
        }
        String filePath = getIconCacheFilePath(info.getIntent().getComponent());
        FileUtils.deleteQuietly(new File(filePath));
    }
    
    public void delteCacheFile(ComponentName componentName){
    	if(componentName == null) return;
    	
    	String filePath = getIconCacheFilePath(componentName);
        FileUtils.deleteQuietly(new File(filePath));
    }

    private Map<String, long[]> readCacheData() {
        BufferedReader reader = null;
        Map<String, long[]> cacheFileData = new HashMap<String, long[]>();
        try {
            File cacheDataFile = new File(getIconCacheFolderPath() + CACHE_DATA_FILE_NAME);
            if (!cacheDataFile.getParentFile().exists()) {
                cacheDataFile.getParentFile().mkdirs();
            }
            if (!cacheDataFile.exists()) {
                cacheDataFile.createNewFile();
                return cacheFileData;
            }
            reader = new BufferedReader(new FileReader(getIconCacheFolderPath() + CACHE_DATA_FILE_NAME));
            String line = null;
            String[] splits = null;
            long[] times = null;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    splits = line.split(":");
                    if (splits.length != 2) {
                        continue;
                    }
                    index = splits[1].indexOf(';');
                    if (index <= 0) {
                        continue;
                    }
                    try {
                        times = new long[2];
                        times[0] = Long.parseLong(splits[1].substring(0, index));
                        times[1] = Long.parseLong(splits[1].substring(index + 1));

                        cacheFileData.put(splits[0], times);
                    } catch (NumberFormatException e) {
                        if (LOGE_ENABLED) {
                            XLog.e(TAG, "read time error", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "read cache data failed", e);
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return cacheFileData;
    }

    private void writeDataFile(Map<String, long[]> cacheDataMap) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(getIconCacheFolderPath() + CACHE_DATA_FILE_NAME));
            for (String filename : cacheDataMap.keySet()) {
                long[] times = cacheDataMap.get(filename);
                writer.write(filename);
                writer.write(':');
                writer.write("" + times[0]);
                writer.write(';');
                writer.write("" + times[1]);
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "write cache data failed", e);
            }
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private String getIconCacheFilePath(ComponentName component) {
        return getIconCacheFolderPath() + getCacheFileName(component);
    }

    private String getCacheFileName(ComponentName component) {
        return component.getPackageName() + "-" + component.getShortClassName();
    }

    private String getIconCacheFolderPath() {
        return PathUtils.getLauncherExternalStoreBase(".cache/icon/");
    }
    
    private void deleteFolderBg(){
    	ComponentName componentName = new ComponentName("miui.content.res","IconCustomizer");
        delteCacheFile(componentName);    	
    }
}
